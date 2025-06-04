package base.utils

import data.io.social.network.conversation.message.MediaIO

object LinkUtils {
    /** Email address pattern, same as android.util.Patterns.EMAIL_ADDRESS */
    val emailRegex = """[a-zA-Z0-9+._%-+]{1,256}@[a-zA-Z0-9][a-zA-Z0-9-]{0,64}(\.[a-zA-Z0-9][a-zA-Z0-9-]{0,25})+""".toRegex()

    /** URL pattern, no HTTP or HTTPS needed */
    val urlRegex = """(https?:\/\/(?:www\.|(?!www))[a-zA-Z0-9][a-zA-Z0-9-]+[a-zA-Z0-9]\.[^\s]{2,}|www\.[a-zA-Z0-9][a-zA-Z0-9-]+[a-zA-Z0-9]\.[^\s]{2,}|https?:\/\/(?:www\.|(?!www))[a-zA-Z0-9]+\.[^\s]{2,}|www\.[a-zA-Z0-9]+\.[^\s]{2,})""".toRegex()

    /** International phone number regex */
    val phoneNumberRegex = """\+?\d{1,4}?[\s-]?\(?(\d{1,4})\)?[\s-]?\d{1,4}[\s-]?\d{1,4}[\s-]?\d{1,9}""".toRegex()
}

expect fun shareLink(title: String, link: String): Boolean

expect fun openEmail(address: String? = null): Boolean

expect fun shareMessage(media: List<MediaIO>, messageContent: String = ""): Boolean

expect fun openLink(link: String): Boolean

expect fun openFile(path: String?)

expect fun downloadFiles(data: Map<MediaIO, ByteArray>): Boolean

expect val deeplinkHost: String

/** Returns an extension for a file by mime type */
fun getExtensionFromMimeType(mimeType: String?): String? {
    return if(mimeType == null) null else when(mimeType) {
        "application/octet-stream" -> "bin"
        "application/json" -> "json"
        "image/png" -> "png"
        "image/jpeg" -> "jpg"
        "image/gif" -> "gif"
        "text/html" -> "html"
        "text/plain" -> "txt"
        "text/css" -> "css"
        "application/javascript" -> "js"
        "audio/mpeg" -> "mp3"
        "audio/ogg" -> "ogg"
        "audio/wav" -> "wav"
        "audio/x-aac" -> "aac"
        "audio/x-flac" -> "flac"
        "video/mp4" -> "mp4"
        "video/x-msvideo" -> "avi"
        "video/x-matroska" -> "mkv"
        "video/webm" -> "webm"
        "application/pdf" -> "pdf"
        "application/vnd.ms-powerpoint" -> "ppt"
        "application/vnd.openxmlformats-officedocument.presentationml.presentation" -> "pptx"
        "application/vnd.openxmlformats-officedocument.presentationml.slideshow" -> "ppsx"
        "application/zip" -> "zip"
        "application/x-rar-compressed" -> "rar"
        "application/x-7z-compressed" -> "7z"
        "application/gzip" -> "gz"
        "image/svg+xml" -> "svg"
        "image/webp" -> "webp"
        else -> null
    }
}
