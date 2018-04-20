package weft.nasa

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import io.vertx.core.Vertx
import io.vertx.core.buffer.Buffer
import io.vertx.core.http.HttpClient
import io.vertx.core.http.HttpClientOptions
import io.vertx.core.http.RequestOptions
import kotlinx.coroutines.experimental.CompletableDeferred
import java.time.LocalDateTime

class Context(private val apiKey: String, private val imageDirectory: String) {

	private val vertx = Vertx.vertx()!!
	private val httpClient: HttpClient
	private val gson: Gson

	init {
		val options = HttpClientOptions()
				.setSsl(true)
				.setDefaultPort(443)
				.setTrustAll(true)
				.setTryUseCompression(true)
		httpClient = vertx.createHttpClient(options)

		val gsonBuilder = GsonBuilder()
		gsonBuilder.registerTypeAdapter(LocalDateTime::class.java, LocalDateTimeDeserializer())
		gsonBuilder.create()

		gson = gsonBuilder.create()
	}

	suspend fun fetch(host: String, uri: String, isSSL: Boolean): Buffer {
		val opts = RequestOptions()
		opts.host = host
		opts.uri = "$uri?api_key=$apiKey"
		opts.isSsl = isSSL
		opts.port = if (isSSL) 443 else 80

		val future = CompletableDeferred<Buffer>()
		val req = httpClient.get(opts)
		req.setTimeout(60000)
		println("getting ${req.absoluteURI()}")
		req.exceptionHandler { future.completeExceptionally(it.cause ?: NullPointerException()) }
		req.handler {
			if (it.statusCode() >= 400)
				future.completeExceptionally(Exception("Error getting $uri: ${it.statusCode()} ${it.statusMessage()}"))
			else
				it.bodyHandler { b -> future.complete(b) }
		}
		req.end()
		println("awaiting ${req.absoluteURI()}")
		return future.await()
	}

	suspend fun <T> fetch(host: String, uri: String, isSSL: Boolean, classOfT: Class<T>): T {
		val opts = RequestOptions()
		opts.host = host
		opts.uri = "$uri?api_key=$apiKey"
		opts.isSsl = isSSL
		opts.port = if (isSSL) 443 else 80

		val future = CompletableDeferred<T>()
		val req = httpClient.get(opts)
		req.setTimeout(60000)
		println("getting ${req.absoluteURI()}")
		req.exceptionHandler { future.completeExceptionally(it.cause ?: NullPointerException()) }
		req.handler {
			if (it.statusCode() >= 400)
				future.completeExceptionally(Exception("Error getting $uri: ${it.statusCode()} $it.statusMessage()}"))
			else {
				it.bodyHandler { b ->
					val img = gson.fromJson(b.toString(), classOfT)
					if (img != null)
						future.complete(img)
					else
						future.completeExceptionally(Exception("Unable to find image for $uri"))
				}
			}

		}
		req.end()
		println("awaiting ${req.absoluteURI()}")
		return future.await()
	}

	suspend fun download(host: String, uri: String, isSSL: Boolean, filename: String): String {
		val img = fetch(host, uri, isSSL)

		println("writing file $imageDirectory$filename")
		val future = CompletableDeferred<String>()
		vertx.fileSystem().writeFile("$imageDirectory$filename", img) {
			if (it.failed()) future.completeExceptionally(it.cause())
			else future.complete("$imageDirectory$filename")
		}
		future.complete("$imageDirectory$filename")
		println("awaiting $imageDirectory$filename")
		return future.await()
	}

	fun close() {
		vertx.close()
	}
}
