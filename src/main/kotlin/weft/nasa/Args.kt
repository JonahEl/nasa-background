package weft.nasa

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required

class Args : CliktCommand() {
	private val apikey: String by option(help = "The Nasa Api Key").required()
	private val destination: String by option("--dest", help = "The output directory").required()
	private val outputFilename: String by option("--file", help = "The filename for the output image").default("nasa.png")
	private val refetch: Boolean by option(help = "Refetch the image metadata").flag(default = false)
	private val outputLatest: Boolean by option("--latest", help = "Use the latest image as output").flag("--random", default = false)
	private val wallpaper: Boolean by option(help = "Sets the wallpaper. Requires set_background.exe to be in the output directory. See set_background.cs").flag(default = false)

	override fun run() {
		NasaImage(apikey, destination, outputFilename, outputLatest, refetch, wallpaper).execute()
	}
}