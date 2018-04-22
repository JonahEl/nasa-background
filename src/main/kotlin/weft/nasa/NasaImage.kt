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
import javax.imageio.ImageIO


class NasaImage(private val apiKey: String, outputPath: String, private val outputFile: String = "nasa.png", private val outputLatest: Boolean = true, private val refetchMetadata: Boolean = false, private val setWallpaper: Boolean = true) {
	private val outputDirectory = File(outputPath)
	private val tmpDirectory = outputDirectory.toSubFile("tmp/")
	private val cacheFile = outputDirectory.toSubFile("metadata.json")
	private val imageExt = "png"
	private val imagePrefix = "epic_"
	private val apiHost = "api.nasa.gov"
	private val epicMetadataApiUri = "/EPIC/api/natural?api_key=$apiKey"
	private val epicApiHost = "epic.gsfc.nasa.gov"

	init {
		require(apiKey.isNotBlank()) { "apiKey can not be empty" }
		require(outputPath.isNotBlank()) { "outputPath can not be empty" }
		require(arrayOf("png", "jpg").contains(imageExt)) { "The image extension must be png or jpg" }
		require(outputFile.endsWith(".$imageExt")) { "The output file must be a .$imageExt" }
	}

	private fun epicImageUriBuilder(md: ImageMetaData) = "/archive/natural/%04d/%02d/%02d/$imageExt/${md.image}.$imageExt?api_key=$apiKey".format(md.date.year, md.date.monthValue, md.date.dayOfMonth)

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

	private fun cleanTempDirectory() {
		tmpDirectory.list().forEach { tmpDirectory.toSubFile(it).delete() }
		tmpDirectory.delete()

		assert(!tmpDirectory.exists()) { "${tmpDirectory.absolutePath} should have been deleted" }
	}

	private fun updateOutputImage() {
		val img = if (outputLatest) listImages().sortedDescending().first()
		else listImages().random()

		val file = outputDirectory.toSubFile(outputFile)
		outputDirectory.toSubFile(img)
				.copyTo(file, true)

		if (setWallpaper)
			setWallpaper(outputAsBitmap(file))
	}

	private fun outputAsBitmap(input: File): File {
		require(input.extension != ".$imageExt") { "Only $imageExt are supported" }
		require(input.exists()) { "Input file ${input.absolutePath} does not exist" }

		val output = File(input.absolutePath.removeSuffix(".$imageExt") + ".bmp")
		ImageIO.write(ImageIO.read(input), "bmp", output)
		return output
	}

	private fun setWallpaper(input: File) {
		require(input.exists()) { "Input file ${input.absolutePath} does not exist" }
		val exeFile = outputDirectory.toSubFile("set_background.exe")
		require(exeFile.exists()) { "exe to set background [${exeFile.absolutePath}] does not exist" }

		val cmd = arrayOf(exeFile.absolutePath, input.absolutePath)
		val p = Runtime.getRuntime().exec(cmd)
		p.waitFor()
	}

	private fun fetchMetaData(vertx: Vertx): Map<String, ImageMetaData> {
		val mdCfg = FetchImageMetaDataMessage(apiHost,
				epicMetadataApiUri,
				cacheFile.absolutePath,
				Instant.now().minus(1, ChronoUnit.DAYS),
				refetchMetadata)

		val data = vertx.eventBus().sendAndWait<List<ImageMetaData>>(FetchImageMetaDataMessage.address, mdCfg)
		return data?.map { md -> md.identifier to md }?.toMap() ?: HashMap<String, ImageMetaData>()
	}

	private fun fetchFile(vertx: Vertx, md: ImageMetaData, completed: () -> Unit) {
		val cfg = FetchFileMessage(epicApiHost,
				epicImageUriBuilder(md),
				outputDirectory.toSubPath("${md.image}.$imageExt").toString(),
				tmpDirectory.toSubPath("${md.image}.$imageExt").toString())

		vertx.eventBus().send<Any>(FetchFileMessage.address, cfg) {
			completed()
		}
	}

	fun execute() {
		//setup vertx
		val vertx = Vertx.vertx()
		ProcessingVerticle.deploy(vertx, 4) //should be switch to be verticle factory
		FetchFileMessage.FetchFileMesssageCodec.register(vertx.eventBus())
		FetchImageMetaDataMessage.FetchImageMetaDataMessageCodec.register(vertx.eventBus())

		//make sure the out dirs exist
		outputDirectory.mkdirs()
		tmpDirectory.mkdirs()

		//move the existing file to a tmp dir
		moveImagesToTemp()

		//get the image metadata
		val metadata = fetchMetaData(vertx)

		val fetchState = ConcurrentHashMap<String, Boolean>()
		metadata.forEach { t, _ -> fetchState[t] = false }

		//download each image if we don't have it already, otherwise move it back to the top level
		metadata.forEach {
			fetchFile(vertx, it.value) {
				println("completed ${it.key}")
				fetchState[it.key] = true
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