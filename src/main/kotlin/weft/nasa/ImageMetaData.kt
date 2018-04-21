package weft.nasa

import java.time.LocalDateTime

data class ImageMetaData(val identifier: String, val caption: String, val image: String, val date: LocalDateTime) {
}