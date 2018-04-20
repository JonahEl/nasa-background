package weft.nasa

import java.time.LocalDateTime

data class ImageMetaData(val identifier: String, val caption: String, val image: String, val date: LocalDateTime) {
	val filename: String
		get() {
			return "$image.png"
		}

	val url: String
		get() {
			return "/archive/natural/%04d/%02d/%02d/png/$filename".format(date.year, date.monthValue, date.dayOfMonth)
		}
}