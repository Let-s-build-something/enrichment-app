package base.utils

import data.io.social.network.conversation.message.MediaIO
import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import platform.Foundation.NSData
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSURL
import platform.Foundation.NSUserDomainMask
import platform.Foundation.create
import platform.Foundation.writeToFile
import platform.UIKit.UIActivityViewController
import platform.UIKit.UIApplication
import platform.UIKit.UIViewController

actual fun shareLink(title: String, link: String): Boolean {
    val itemsToShare = listOf(link)

    val activityViewController = UIActivityViewController(
        activityItems = itemsToShare,
        applicationActivities = null
    )

    val rootViewController = UIApplication.sharedApplication.keyWindow?.rootViewController
    val topViewController = rootViewController?.getTopViewController()

    topViewController?.presentViewController(activityViewController, animated = true, completion = null)

    return topViewController != null
}

// Extension function to find the top view controller
fun UIViewController.getTopViewController(): UIViewController? {
    var currentController: UIViewController? = this
    while (currentController?.presentedViewController != null) {
        currentController = currentController.presentedViewController
    }
    return currentController
}

actual fun shareMessage(media: List<MediaIO>, messageContent: String): Boolean {
    return false
}

actual fun openLink(link: String): Boolean {
    val nsUrl = NSURL.URLWithString(link)

    return if (nsUrl != null && UIApplication.sharedApplication.canOpenURL(nsUrl)) {
        UIApplication.sharedApplication.openURL(nsUrl)
        true
    }else false
}

@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
actual fun downloadFiles(data: Map<MediaIO, ByteArray>): Boolean {
    var result = true

    data.forEach { (media, data) ->
        // Prepare file path
        val fileManager = NSFileManager.defaultManager()
        val documentsDirectoryURL: NSURL? = getDocumentsDirectory()
        val documentsDirectory = fileManager.URLForDirectory(
            NSDocumentDirectory,
            NSUserDomainMask,
            documentsDirectoryURL,
            false,
            null
        )
        val fileName = "${sha256(media.url)}.${getExtensionFromMimeType(media.mimetype)}"
        val fileURL = documentsDirectory?.URLByAppendingPathComponent(fileName)

        if (fileURL != null) {
            try {
                data.usePinned { pinnedData ->
                    val fileData = NSData.create(pinnedData.addressOf(0), data.size.toULong())
                    fileData.writeToFile(documentsDirectory.path ?: "", atomically = true)
                }
            } catch (e: Exception) {
                result = false
            }
        }
    }

    return result
}

@OptIn(ExperimentalForeignApi::class)
private fun getDocumentsDirectory(): NSURL? {
    val fileManager = NSFileManager.defaultManager()
    val url = fileManager.URLForDirectory(
        NSDocumentDirectory,
        NSUserDomainMask,
        null,
        true,
        null
    )
    return url
}

actual fun openFile(path: String?) {
}

actual fun openEmail(address: String?): Boolean {
    val url = NSURL(string = "message://")

    return if (UIApplication.sharedApplication.canOpenURL(url)) {
        UIApplication.sharedApplication.openURL(url)
        true
    }else false
}

actual val deeplinkHost: String = "https://augmy.org/"