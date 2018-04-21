package weft.nasa

import kotlinx.coroutines.experimental.async
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.time.Instant
import java.time.temporal.ChronoUnit

class NasaImage(apiKey: String, outputPath: String, private val outputFile: String = "nasa.png", private val refetchMetadata: Boolean = false) {
	private val fetcher = Fetcher("api_key=$apiKey")
	private val outputDirectory = File(outputPath)
	private val tmpDirectory = outputDirectory.toSubFile("tmp/")
	private val cacheFile = outputDirectory.toSubFile("metadata.json")
	private val imageExt = "png"
	private val imagePrefix = "epic_"
	private val apiHost = "api.nasa.gov"
	private val epicMetadataApiUri = "/EPIC/api/natural"
	private val epicApiHost = "epic.gsfc.nasa.gov"

	init {
		require(arrayOf("png", "jpg").contains(imageExt)) { "The image extension must be png or jpg" }
		require(outputFile.endsWith(".$imageExt")) { "The output file must be a .$imageExt" }
	}

	private fun epicImageUriBuilder(md: ImageMetaData) = "/archive/natural/%04d/%02d/%02d/$imageExt/${md.image}.$imageExt".format(md.date.year, md.date.monthValue, md.date.dayOfMonth)

	private suspend fun fetchMetadata(): Array<ImageMetaData> {
		val metadata: Array<ImageMetaData>
		if (refetchMetadata || cacheFile.isOlderThan(Instant.now().minus(1, ChronoUnit.DAYS))) {
			metadata = fetcher.fetch(apiHost, epicMetadataApiUri, Array<ImageMetaData>::class.java)
			cacheFile.writeJson(metadata)
		} else
			metadata = cacheFile.readJson(Array<ImageMetaData>::class.java)
		return metadata
	}

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

	private suspend fun getImagesIfMissing(metadata: Array<ImageMetaData>) {
		metadata.forEach {
			val tmp = tmpDirectory.toSubFile("${it.image}.$imageExt")
			if (tmp.exists())
				Files.move(tmp.toPath(),
						outputDirectory.toSubPath("${it.image}.$imageExt"),
						StandardCopyOption.REPLACE_EXISTING)
			else
				fetcher.download(epicApiHost,
						epicImageUriBuilder(it),
						outputDirectory.toSubPath("${it.image}.$imageExt").toString())
		}
	}

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

	fun execute() {
		outputDirectory.mkdirs()
		tmpDirectory.mkdirs()

		val co = async {

			//get the image metadata
			val metadata = fetchMetadata()

			//move the existing file to a tmp dir
			moveImagesToTemp()

			//download each image if we don't have it already
			//otherwise move it back to the top level
			getImagesIfMissing(metadata)

			//clean the tmp dir to get rid of any old files
			cleanTempDirectory()

			//copy the newest file to the output file
			updateOutputImage()

			println("async done")
		}

		while (!co.isCompleted) {
			Thread.sleep(100)
		}

		if (co.isCompletedExceptionally)
			co.getCompletionExceptionOrNull()?.printStackTrace()

		println("closing")
		fetcher.close()
	}
}