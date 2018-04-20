package weft.nasa

import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.JsonParseException
import java.lang.reflect.Type
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class LocalDateTimeDeserializer : JsonDeserializer<LocalDateTime> {
	@Throws(JsonParseException::class)
	override fun deserialize(element: JsonElement, arg1: Type, arg2: JsonDeserializationContext): LocalDateTime? {
		return LocalDateTime.parse(element.asString, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
	}
}