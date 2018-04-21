package weft.nasa

import io.vertx.core.Vertx
import io.vertx.core.buffer.Buffer
import io.vertx.core.http.HttpClient
import io.vertx.core.http.HttpClientOptions
import io.vertx.core.http.RequestOptions
import kotlinx.coroutines.experimental.CompletableDeferred

class Fetcher(private val queryStringAppend: String = "", private val useSSL: Boolean = true) {

	private val vertx = Vertx.vertx()!!
	private val httpClient: HttpClient

	init {
		val options = HttpClientOptions()
				.setSsl(useSSL)
				.setDefaultPort(if (useSSL) 443 else 80)
				.setTrustAll(true)
				.setTryUseCompression(true)
		httpClient = vertx.createHttpClient(options)
	}

	private fun buildRequestOptions(host: String, uri: String) = RequestOptions().apply {
		this.host = host
		this.uri = uri + (if (uri.contains("?")) "&" else "?") + queryStringAppend
		this.isSsl = useSSL
		this.port = if (useSSL) 443 else 80
	}

	suspend fun fetch(host: String, uri: String): Buffer {
		val future = CompletableDeferred<Buffer>()
		val req = httpClient.get(buildRequestOptions(host, uri))
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

	suspend fun <T> fetch(host: String, uri: String, classOfT: Class<T>): T {
		return fetch(host, uri).toString().fromJson(classOfT)
	}

	suspend fun download(host: String, uri: String, filename: String): String {
		val img = fetch(host, uri)

		println("writing file $filename")
		val future = CompletableDeferred<String>()
		vertx.fileSystem().writeFile(filename, img) {
			if (it.failed()) future.completeExceptionally(it.cause())
			else future.complete(filename)
		}
		future.complete(filename)
		println("awaiting $filename")
		return future.await()
	}

	fun close() {
		vertx.close()
	}
}
