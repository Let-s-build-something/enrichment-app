package base.utils

import platform.Foundation.NSURL
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

actual fun openLink(link: String): Boolean {
    val nsUrl = NSURL.URLWithString(link)

    return if (nsUrl != null && UIApplication.sharedApplication.canOpenURL(nsUrl)) {
        UIApplication.sharedApplication.openURL(nsUrl)
        true
    }else false
}