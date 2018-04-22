package weft.nasa.messages

import io.vertx.core.buffer.Buffer
import io.vertx.core.eventbus.EventBus
import io.vertx.core.eventbus.MessageCodec
import io.vertx.core.json.Json
import weft.nasa.decode
import weft.nasa.encode


data class FetchFileMessage(val host: String, val uri: String, val outputFilename: String, val cacheFilename: String) {
	companion object {
		const val address = "fetch.file"
	}

	class FetchFileMesssageCodec : MessageCodec<FetchFileMessage, FetchFileMessage> {
		override fun encodeToWire(buffer: Buffer, msg: FetchFileMessage) {
			buffer.encode(msg, { um -> Json.encode(um) })
		}

		override fun decodeFromWire(position: Int, buffer: Buffer): FetchFileMessage {
			return buffer.decode(position, { json -> Json.decodeValue(json, FetchFileMessage::class.java) })
		}

		override fun transform(msg: FetchFileMessage): FetchFileMessage {
			return msg
		}

		override fun name(): String {
			return this.javaClass.simpleName
		}

		override fun systemCodecID(): Byte {
			return -1 // Always -1
		}

		companion object {
			fun register(eventBus: EventBus) {
				eventBus.registerDefaultCodec(FetchFileMessage::class.java, FetchFileMesssageCodec())
			}
		}
	}
}
