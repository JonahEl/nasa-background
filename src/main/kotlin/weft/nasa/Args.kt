package weft.nasa

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required

class Args : CliktCommand() {
	//val count: Int by option(help="Number of greetings").int().default(1)
	private val apikey: String by option(help = "The Nasa Api Key").required()
	private val destination: String by option("--dest", help = "The output directory").required()
	private val outputFilename: String by option("--file", help = "The filename for the latest image").default("nasa.png")
	private val refetch: Boolean by option(help = "Refetch the image metadata").flag(default = false)

	override fun run() {
		NasaImage(apikey, destination, outputFilename, refetch).execute()
	}
}