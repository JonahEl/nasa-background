package weft.nasa

import kotlinx.coroutines.experimental.async
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption

class AppMain {

	companion object {
		@JvmStatic
		fun main(args: Array<String>) {
			val cxt = Context(args[0])
			val imageDirectory = args[1]
			val tmpImageDirectory = "${args[1]}tmp/"
			val outputFile = "latest-nasa.png"

			val co = async {
				println("fetching list")
				var metadata = cxt.fetch("api.nasa.gov", "/EPIC/api/natural", true, Array<ImageMetaData>::class.java)
				println("fetch done. got ${metadata.size} items")

				//copy the metadata since Gson screws with the constructor behaviour resulting in url have a null in it
				//need to find a better solution
				metadata = metadata.map { it -> it.copy() }.toTypedArray()

				//move the existing file to a tmp dir
				File(tmpImageDirectory).mkdirs()
				File(imageDirectory).list()
					.filter { it.endsWith(".png") }
					.forEach {
						Files.move(File("$imageDirectory$it").toPath(),
							File("$tmpImageDirectory$it").toPath(),
							StandardCopyOption.REPLACE_EXISTING)
					}

				//download each image if we don't have it already
				//otherwise move it back to the top level
				metadata.forEach {
					val tmp = File("$tmpImageDirectory${it.filename}")
					if (tmp.exists())
						Files.move(tmp.toPath(),
								File("$imageDirectory${it.filename}").toPath(),
								StandardCopyOption.REPLACE_EXISTING)
					else
						cxt.download("epic.gsfc.nasa.gov", it.url, true, "$imageDirectory${it.filename}")
				}

				//clean the tmp dir to get rid of any old files
				File(tmpImageDirectory).list().forEach {File("$tmpImageDirectory$it").delete() }
				File(tmpImageDirectory).delete()

				//copy the newest file to the output file
				val first = File(imageDirectory).list()
						.filter { it.endsWith(".png") }
						.sortedDescending()
						.first()

				File("$imageDirectory$first").copyTo(File("$imageDirectory$outputFile"), true)


				println("async done")
			}

			while (!co.isCompleted) {
				Thread.sleep(100)
			}

			if (co.isCompletedExceptionally)
				println(co.getCompletionExceptionOrNull())

			println("closing")
			cxt.close()
		}
	}
}
