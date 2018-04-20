package weft.nasa

import com.google.gson.*
import java.lang.reflect.Type
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class LocalDateTimeSerializer : JsonSerializer<LocalDateTime>, JsonDeserializer<LocalDateTime> {
	override fun serialize(src: LocalDateTime?, typeOfSrc: Type?, context: JsonSerializationContext?): JsonElement {
		return JsonPrimitive(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").format(src))
	}

	@Throws(JsonParseException::class)
	override fun deserialize(element: JsonElement, arg1: Type, arg2: JsonDeserializationContext): LocalDateTime? {
		return LocalDateTime.parse(element.asString, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
	}
}