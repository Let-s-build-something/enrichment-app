package base.utils

import data.io.social.network.conversation.message.MediaIO
import utils.SharedLogger
import java.awt.Desktop
import java.io.File
import java.io.IOException
import java.net.URI
import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.StandardOpenOption

actual fun shareLink(title: String, link: String): Boolean {
    return false
}

actual fun shareMessage(media: List<MediaIO>, messageContent: String): Boolean {
    return false
}

actual fun openLink(link: String): Boolean {
    return if (Desktop.isDesktopSupported()) {
        try {
            Desktop.getDesktop().browse(URI(link.replace(" ", "")))
            true
        }catch (e: Exception) { // Jvm is very sensitive to incorrect links
            SharedLogger.logger.error { "Error opening URL: ${e.message}" }
            false
        }
    }else {
        SharedLogger.logger.error { "Desktop is not supported. Cannot open URL." }
        false
    }
}

actual fun openFile(path: String?) {
    Desktop.getDesktop().open(
        File(path ?: Paths.get(System.getProperty("user.home"), "Downloads").toString())
    )
}

actual fun downloadFiles(data: Map<MediaIO, ByteArray>): Boolean {
    var result = true

    data.forEach { (media, data) ->
        // Determine file path based on mimeType
        val fileName = "${media.url?.toSha256()}.${getExtensionFromMimeType(media.mimetype)}"
        val filePath = Paths.get(System.getProperty("user.home"), "Downloads", fileName)

        try {
            Files.createDirectories(filePath.parent)
            Files.write(filePath, data, StandardOpenOption.CREATE, StandardOpenOption.WRITE)
        } catch (e: IOException) {
            SharedLogger.logger.error { "Error saving file: ${e.message}" }
            result = false
        }
    }

    return result
}

actual fun openEmail(address: String?): Boolean {
    return openLink("ms-outlook://")
            || openLink("googlegmail://")
            || openLink("thunderbird://")
            || (address != null && openLink(
                when(val domain = address.substringAfterLast("@")) {
                    "gmail.com" -> "https://mail.google.com"
                    "outlook.com", "hotmail.com" -> "https://outlook.live.com"
                    "yahoo.com" -> "https://mail.yahoo.com"
                    "icloud.com" -> "https://www.icloud.com/mail"
                    "protonmail.com" -> "https://mail.proton.me"
                    else -> "https://$domain"
                }
            ))
}

actual val deeplinkHost: String = "augmy://"
