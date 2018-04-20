package weft.nasa

import kotlinx.coroutines.experimental.async

class AppMain {

	companion object {
		@JvmStatic
		fun main(args: Array<String>) {
			val cxt = Context(args[0], args[1])

			val co = async {
				println("fetching list")
				val metadata = cxt.fetch("api.nasa.gov", "/EPIC/api/natural", true, Array<ImageMetaData>::class.java)
				println("fetch done. got ${metadata.size} items")

				val md = metadata.maxBy { it.date } ?: error("Unable to find any images")
				val imgPath = cxt.download("epic.gsfc.nasa.gov", md.url, true, "latest-nasa.png")
				println("downloaded img to $imgPath")
				println("async done")
			}

			while (!co.isCompleted) {
				Thread.sleep(100)
			}

			if(co.isCompletedExceptionally)
				println(co.getCompletionExceptionOrNull())

			println("closing")
			cxt.close()
		}
	}
}
