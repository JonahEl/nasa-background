package weft.nasa

import io.vertx.core.AbstractVerticle
import io.vertx.core.DeploymentOptions
import io.vertx.core.Vertx
import io.vertx.core.buffer.Buffer
import io.vertx.core.http.HttpClient
import io.vertx.core.http.HttpClientOptions
import io.vertx.core.http.RequestOptions
import weft.nasa.messages.FetchFileMessage
import weft.nasa.messages.FetchImageMetaDataMessage
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.time.Instant

class ProcessingVerticle(private val queryStringAppend: String = "", private val useSSL: Boolean = true) : AbstractVerticle() {
	private var httpClient: HttpClient? = null

	override fun start() {
		println("[Worker - Processing] Starting in " + Thread.currentThread().name)

		try {
			httpClient = vertx.createHttpClient(HttpClientOptions()
					.setTrustAll(true)
					.setTryUseCompression(true))

			vertx.eventBus().consumer<FetchFileMessage>(FetchFileMessage.address) { message ->
				println("received message from ${FetchFileMessage.address}")
				downloadIfMissing(message.body()) {
					message.reply(true)
				}
			}

			vertx.eventBus().consumer<FetchImageMetaDataMessage>(FetchImageMetaDataMessage.address) { msg ->
				val b = msg.body()
				fetchJson(b.host, b.uri, Array<ImageMetaData>::class.java, b.cacheFilename, b.cacheUntil, b.refetch) {
					msg.reply(it.toList())
				}
			}
		} catch (ex: Exception) {
			ex.printStackTrace()
		}
		println("[Worker - Processing] Started in " + Thread.currentThread().name)
	}

	private fun buildRequestOptions(host: String, uri: String) = RequestOptions().apply {
		this.host = host
		this.uri = uri + (if (uri.contains("?")) "&" else "?") + queryStringAppend
		this.isSsl = useSSL
		this.port = if (useSSL) 443 else 80
	}

	private fun fetch(host: String, uri: String, handler: (Buffer) -> Unit) {
		val req = httpClient?.get(buildRequestOptions(host, uri))
				?: error("httpClient is null. Maybe the verticle wasn't deployed?")
		req.setTimeout(60000)
		println("getting ${req.absoluteURI()}")
		req.exceptionHandler { it.cause?.printStackTrace() }
		req.handler {
			if (it.statusCode() >= 400)
				error("Error getting $uri: ${it.statusCode()} ${it.statusMessage()}")
			else
				it.bodyHandler { b -> handler(b) }
		}
		req.end()
	}

	private fun <T> fetch(host: String, uri: String, classOfT: Class<T>, handler: (T) -> Unit) {
		fetch(host, uri) {
			handler(it.toString().fromJson(classOfT))
		}
	}

	private fun download(host: String, uri: String, filename: String, handler: () -> Unit = {}) {
		fetch(host, uri) {
			println("writing file $filename")
			vertx.fileSystem().writeFile(filename, it) {
				if (it.failed()) error(it.cause())
				else handler()
			}
		}
	}

	private fun downloadIfMissing(msg: FetchFileMessage, handler: () -> Unit = {}) {
		val tmp = File(msg.cacheFilename)
		if (tmp.exists()) {
			println("${msg.cacheFilename} exists. Skipping.")
			Files.move(tmp.toPath(),
					File(msg.outputFilename).toPath(),
					StandardCopyOption.REPLACE_EXISTING)
			handler()
		} else
			println("${msg.cacheFilename} doesn't exist. Downloading.")
		download(msg.host,
				msg.uri,
				msg.outputFilename) {
			handler()
		}
	}

	private fun <T> fetchJson(host: String, uri: String, classOfT: Class<T>, cacheFilename: String, cacheUntil: Instant?, refetch: Boolean, handler: (T) -> Unit = {}) {
		val cacheFile = File(cacheFilename)
		if (refetch || cacheFile.isOlderThan(cacheUntil)) {
			fetch(host, uri, classOfT) {
				cacheFile.writeJson(it)
				handler(it)
			}
		} else
			handler(cacheFile.readJson(classOfT))
	}

	companion object {
		fun deploy(vertx: Vertx) {
			var deployComplete = false
			vertx.deployVerticle(ProcessingVerticle::class.java.canonicalName, DeploymentOptions().setWorker(true)) {
				deployComplete = true
			}
			while (!deployComplete)
				Thread.sleep(100)
		}
	}
}