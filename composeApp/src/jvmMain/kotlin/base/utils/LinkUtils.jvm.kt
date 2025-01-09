package base.utils

import java.awt.Desktop
import java.net.URI

actual fun shareLink(title: String, link: String): Boolean {
    return false
}

actual fun openLink(link: String): Boolean {
    return if (Desktop.isDesktopSupported()) {
        Desktop.getDesktop().browse(URI(link))
        true
    }else {
        println("Desktop is not supported. Cannot open URL.")
        false
    }
}