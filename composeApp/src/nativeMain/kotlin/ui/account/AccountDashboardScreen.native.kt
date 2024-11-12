package ui.account

import platform.UIKit.UIActivityViewController
import platform.UIKit.UIApplication
import platform.UIKit.UIViewController

actual fun shareLink(title: String, imageUrl: String?, link: String): Boolean {
    val itemsToShare = mutableListOf(title, link).apply {
        if(imageUrl != null) add(imageUrl)
    }

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