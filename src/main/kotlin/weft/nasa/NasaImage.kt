package weft.nasa

import io.vertx.core.Vertx
import weft.nasa.messages.FetchFileMessage
import weft.nasa.messages.FetchImageMetaDataMessage
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.concurrent.ConcurrentHashMap


class NasaImage(val apiKey: String, outputPath: String, private val outputFile: String = "nasa.png", private val refetchMetadata: Boolean = false) {
	private val outputDirectory = File(outputPath)
	private val tmpDirectory = outputDirectory.toSubFile("tmp/")
	private val cacheFile = outputDirectory.toSubFile("metadata.json")
	private val imageExt = "png"
	private val imagePrefix = "epic_"
	private val apiHost = "api.nasa.gov"
	private val epicMetadataApiUri = "/EPIC/api/natural?api_key=$apiKey"
	private val epicApiHost = "epic.gsfc.nasa.gov"

	init {
		require(arrayOf("png", "jpg").contains(imageExt)) { "The image extension must be png or jpg" }
		require(outputFile.endsWith(".$imageExt")) { "The output file must be a .$imageExt" }
	}

	private fun epicImageUriBuilder(md: ImageMetaData) = "/archive/natural/%04d/%02d/%02d/$imageExt/${md.image}.$imageExt?api_key=$apiKey".format(md.date.year, md.date.monthValue, md.date.dayOfMonth)

//	private suspend fun fetchMetadata(): Array<ImageMetaData> {
//		val metadata: Array<ImageMetaData>
//		if (refetchMetadata || cacheFile.isOlderThan(Instant.now().minus(1, ChronoUnit.DAYS))) {
//			metadata = fetcher.fetch(apiHost, epicMetadataApiUri, Array<ImageMetaData>::class.java)
//			cacheFile.writeJson(metadata)
//		} else
//			metadata = cacheFile.readJson(Array<ImageMetaData>::class.java)
//		return metadata
//	}

	private fun listImages(): List<String> {
		return outputDirectory.list()
				.filter { it.startsWith(imagePrefix) && it.endsWith(".$imageExt") }
	}

	private fun moveImagesToTemp() {
		listImages()
				.forEach {
					outputDirectory.path
					Files.move(outputDirectory.toSubPath(it),
							tmpDirectory.toSubPath(it),
							StandardCopyOption.REPLACE_EXISTING)
				}
	}

//	private suspend fun getImagesIfMissing(metadata: Array<ImageMetaData>) {
//		metadata.forEach {
	//			val tmp = tmpDirectory.toSubFile("${it.image}.$imageExt")
//			if (tmp.exists())
//				Files.move(tmp.toPath(),
//						outputDirectory.toSubPath("${it.image}.$imageExt"),
//						StandardCopyOption.REPLACE_EXISTING)
//			else
//				fetcher.download(epicApiHost,
//						epicImageUriBuilder(it),
//						outputDirectory.toSubPath("${it.image}.$imageExt").toString())
//		}
//	}

	private fun cleanTempDirectory() {
		tmpDirectory.list().forEach { tmpDirectory.toSubFile(it).delete() }
		tmpDirectory.delete()
	}

	private fun updateOutputImage() {
		val first = listImages()
				.sortedDescending()
				.first()

		outputDirectory.toSubFile(first)
				.copyTo(outputDirectory.toSubFile(outputFile), true)
	}

	private fun fetchMetaData(vertx: Vertx, handler: (Map<String, ImageMetaData>) -> Unit) {
		val mdCfg = FetchImageMetaDataMessage(apiHost,
				epicMetadataApiUri,
				cacheFile.absolutePath,
				Instant.now().minus(1, ChronoUnit.DAYS),
				refetchMetadata)

		vertx.eventBus().send<List<ImageMetaData>>(FetchImageMetaDataMessage.address, mdCfg) {
			if (it.failed()) {
				it.cause().printStackTrace()
				return@send
			}
			handler(it.result().body().map { md -> md.identifier to md }.toMap())
		}
	}

	private fun fetchFile(vertx: Vertx, md: ImageMetaData, hanlder: () -> Unit) {
		val cfg = FetchFileMessage(epicApiHost,
				epicImageUriBuilder(md),
				outputDirectory.toSubPath("${md.image}.$imageExt").toString(),
				tmpDirectory.toSubPath("${md.image}.$imageExt").toString())

		vertx.eventBus().send<Any>(FetchFileMessage.address, cfg) {
			hanlder()
		}
	}

	fun execute() {
		val vertx = Vertx.vertx()

		ProcessingVerticle.deploy(vertx)
		FetchFileMessage.FetchFileMesssageCodec.register(vertx.eventBus())
		FetchImageMetaDataMessage.FetchImageMetaDataMessageCodec.register(vertx.eventBus())

		outputDirectory.mkdirs()
		tmpDirectory.mkdirs()

		//move the existing file to a tmp dir
		moveImagesToTemp()

		//get the image metadata
		val fetchState = ConcurrentHashMap<String, Boolean>()
		fetchMetaData(vertx) { metadata ->
			metadata.forEach { t, _ -> fetchState[t] = false }

			//download each image if we don't have it already, otherwise move it back to the top level
			metadata.forEach {
				fetchFile(vertx, it.value) {
					println("completed ${it.key}")
					fetchState[it.key] = true
				}
			}
		}

		//wait for completion
		println("waiting for completion")
		while (fetchState.isEmpty() || fetchState.any() { !it.value }) {
			Thread.sleep(100)
		}
		Thread.sleep(1000)

		println("cleaning")
		//clean the tmp dir to get rid of any old files
		cleanTempDirectory()

		//copy the newest file to the output file
		updateOutputImage()

		println("closing")
		vertx.close()
		println("closed")
	}
}