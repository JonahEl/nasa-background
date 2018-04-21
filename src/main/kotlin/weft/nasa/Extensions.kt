package weft.nasa

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import java.io.File
import java.time.Instant
import java.time.LocalDateTime

fun File.toSubPath(subPath: String) = File(absolutePath).toPath().resolve(subPath)!!
fun File.toSubFile(subPath: String) = File(absolutePath).toPath().resolve(subPath).toFile()!!

fun <T> String.fromJson(classOfT: Class<T>) = GsonCache.gson.fromJson(this, classOfT)!!

fun <T> File.readJson(classOfT: Class<T>) = this.readText().fromJson(classOfT)
fun File.writeJson(obj: Any) = this.writeText(GsonCache.gson.toJson(obj))
fun File.isOlderThan(dt: Instant) = if (exists()) lastModified() < dt.toEpochMilli() else true

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

