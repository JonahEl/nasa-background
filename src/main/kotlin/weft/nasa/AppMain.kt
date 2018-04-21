package weft.nasa

import kotlinx.coroutines.experimental.async
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.time.Instant
import java.time.temporal.ChronoUnit

class AppMain {

	companion object {
		@JvmStatic
		fun main(args: Array<String>) {
			val cxt = Fetcher("api_key=${args[0]}")
			val imageDirectory = File(args[1])
			val tmpImageDirectory = File("${args[1]}tmp/")
			val outputFile = "nasa.png"
			val cacheFile = imageDirectory.toSubFile("metadata.json")
			val refetchMetadata = false

			imageDirectory.mkdirs()
			tmpImageDirectory.mkdirs()

			val co = async {

				//get the image metadata
				val metadata: Array<ImageMetaData>
				if (refetchMetadata || cacheFile.isOlderThan(Instant.now().minus(1, ChronoUnit.DAYS))) {
					metadata = cxt.fetch("api.nasa.gov", "/EPIC/api/natural", Array<ImageMetaData>::class.java)
					cacheFile.writeJson(metadata)
				}
				else
					metadata = cacheFile.readJson(Array<ImageMetaData>::class.java)

				//move the existing file to a tmp dir
				imageDirectory.list()
						.filter { it.endsWith(".png") }
						.forEach {
							imageDirectory.path
							Files.move(imageDirectory.toSubPath(it),
									tmpImageDirectory.toSubPath(it),
									StandardCopyOption.REPLACE_EXISTING)
						}

				//download each image if we don't have it already
				//otherwise move it back to the top level
				metadata.forEach {
					val tmp = tmpImageDirectory.toSubFile("${it.image}.png")
					if (tmp.exists())
						Files.move(tmp.toPath(),
								imageDirectory.toSubPath("${it.image}.png"),
								StandardCopyOption.REPLACE_EXISTING)
					else
						cxt.download("epic.gsfc.nasa.gov",
								"/archive/natural/%04d/%02d/%02d/png/${it.image}.png".format(it.date.year, it.date.monthValue, it.date.dayOfMonth),
								imageDirectory.toSubPath("${it.image}.png").toString())
				}

				//clean the tmp dir to get rid of any old files
				tmpImageDirectory.list().forEach { tmpImageDirectory.toSubFile(it).delete() }
				tmpImageDirectory.delete()

				//copy the newest file to the output file
				val first = imageDirectory.list()
						.filter { it.endsWith(".png") }
						.sortedDescending()
						.first()

				imageDirectory.toSubFile(first)
						.copyTo(imageDirectory.toSubFile(outputFile), true)

				println("async done")
			}

			while (!co.isCompleted) {
				Thread.sleep(100)
			}

			if (co.isCompletedExceptionally)
				co.getCompletionExceptionOrNull()?.printStackTrace()

			println("closing")
			cxt.close()
		}
	}
}
