package weft.nasa.messages

import io.vertx.core.buffer.Buffer
import io.vertx.core.eventbus.EventBus
import io.vertx.core.eventbus.MessageCodec
import io.vertx.core.json.Json
import weft.nasa.ImageMetaData
import weft.nasa.decode
import weft.nasa.encode
import java.time.Instant

data class FetchImageMetaDataMessage(val host: String, val uri: String, val cacheFilename: String, val cacheUntil: Instant? = null, val refetch: Boolean = false) {
	companion object {
		const val address = "fetch.imagemetadata"
	}

	class FetchImageMetaDataMessageCodec() : MessageCodec<FetchImageMetaDataMessage, FetchImageMetaDataMessage> {

		override fun encodeToWire(buffer: Buffer, msg: FetchImageMetaDataMessage) {
			buffer.encode(msg, { um -> Json.encode(um) })
		}

		override fun decodeFromWire(position: Int, buffer: Buffer): FetchImageMetaDataMessage {
			return buffer.decode(position, { json -> Json.decodeValue(json, FetchImageMetaDataMessage::class.java) })
		}

		override fun transform(msg: FetchImageMetaDataMessage): FetchImageMetaDataMessage {
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
				eventBus.registerDefaultCodec(FetchImageMetaDataMessage::class.java, FetchImageMetaDataMessageCodec())
				eventBus.registerDefaultCodec(ImageMetaData::class.java, ImageMetaDataCodec())
				eventBus.registerDefaultCodec(ArrayList<ImageMetaData>().javaClass, ArrayListCodec())
			}
		}
	}

	class ImageMetaDataCodec() : MessageCodec<ImageMetaData, ImageMetaData> {

		override fun encodeToWire(buffer: Buffer, msg: ImageMetaData) {
			buffer.encode(msg, { um -> Json.encode(um) })
		}

		override fun decodeFromWire(position: Int, buffer: Buffer): ImageMetaData {
			return buffer.decode(position, { json -> Json.decodeValue(json, ImageMetaData::class.java) })
		}

		override fun transform(msg: ImageMetaData): ImageMetaData {
			return msg
		}

		override fun name(): String {
			return this.javaClass.simpleName
		}

		override fun systemCodecID(): Byte {
			return -1 // Always -1
		}
	}

	class ArrayListCodec<T> : MessageCodec<ArrayList<T>, ArrayList<T>> {
		override fun encodeToWire(buffer: Buffer, msg: ArrayList<T>) {
			buffer.encode(msg, { um -> Json.encode(um) })
		}

		override fun decodeFromWire(position: Int, buffer: Buffer): ArrayList<T> {
			return buffer.decode(position, { json -> Json.decodeValue(json, ArrayList<T>().javaClass) })
		}

		override fun transform(msg: ArrayList<T>): ArrayList<T> {
			return msg
		}

		override fun name(): String {
			return this.javaClass.simpleName
		}

		override fun systemCodecID(): Byte {
			return -1 // Always -1
		}
	}

}

