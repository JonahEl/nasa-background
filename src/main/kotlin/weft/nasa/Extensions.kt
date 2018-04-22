package weft.nasa

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import io.vertx.core.buffer.Buffer
import java.io.File
import java.time.Instant
import java.time.LocalDateTime

fun File.toSubPath(subPath: String) = File(absolutePath).toPath().resolve(subPath)!!
fun File.toSubFile(subPath: String) = File(absolutePath).toPath().resolve(subPath).toFile()!!

fun <T> String.fromJson(classOfT: Class<T>) = GsonCache.gson.fromJson(this, classOfT)!!

fun <T> File.readJson(classOfT: Class<T>) = this.readText().fromJson(classOfT)
fun File.writeJson(obj: Any?) = this.writeText(GsonCache.gson.toJson(obj))
fun File.isOlderThan(dt: Instant?) = if (dt == null) false else if (exists()) lastModified() < dt.toEpochMilli() else true

class GsonCache {
	companion object {
		var gson: Gson

		init {
			val gsonBuilder = GsonBuilder()
			gsonBuilder.registerTypeAdapter(LocalDateTime::class.java, LocalDateTimeSerializer())
			gson = gsonBuilder.create()
		}
	}
}

fun <T : Any> Buffer.encode(msg: T, encodeHandler: (msg: T) -> String) {
	val str = encodeHandler(msg)

	// Length of JSON: is NOT characters count
	val length = str.toByteArray().size

	// Write data into given buffer
	appendInt(length)
	appendString(str)
}

fun <T : Any> Buffer.decode(position: Int, decodeHandler: (json: String) -> T): T {
	// Length of JSON
	val length = getInt(position)

	// Get JSON string by it`s length
	// Jump 4 because getInt() == 4 bytes
	return decodeHandler(getString(position + 4, position + 4 + length))
}

