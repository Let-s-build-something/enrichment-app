
import cocoapods.FirebaseCore.FIRApp
import cocoapods.GoogleSignIn.GIDConfiguration
import cocoapods.GoogleSignIn.GIDSignIn
import koin.commonModule
import kotlinx.cinterop.ExperimentalForeignApi
import org.koin.core.context.startKoin
import platform.UIKit.UIApplication
import platform.UIKit.UIApplicationDelegateProtocol
import platform.UIKit.registerForRemoteNotifications
import platform.UserNotifications.UNAuthorizationOptionAlert
import platform.UserNotifications.UNAuthorizationOptionBadge
import platform.UserNotifications.UNAuthorizationOptionSound
import platform.UserNotifications.UNAuthorizationOptions
import platform.UserNotifications.UNUserNotificationCenter
import platform.UserNotifications.UNUserNotificationCenterDelegateProtocol
import platform.darwin.NSObject

/**
 * initializes iOS app this includes:
 * koin
 */
fun doAppInit() {
    startKoin {
        modules(commonModule)
    }
}

/** Callback when iOS application is */
fun <T> T.onIOSApplication(
    application: UIApplication,
    launchOptions: Map<Any?, *>?
): Boolean where T: UNUserNotificationCenterDelegateProtocol,
                 T: UIApplicationDelegateProtocol,
                 T: NSObject {
    val authOptions: UNAuthorizationOptions = UNAuthorizationOptionAlert or UNAuthorizationOptionBadge or UNAuthorizationOptionSound
    UNUserNotificationCenter.currentNotificationCenter().run {
        delegate = this@onIOSApplication
        requestAuthorizationWithOptions(
            options = authOptions,
            completionHandler = { _, _ -> }
        )
    }
    application.registerForRemoteNotifications()

    return configureFirebase()
}

@OptIn(ExperimentalForeignApi::class)
private fun configureFirebase(): Boolean {
    FIRApp.configure()

    GIDSignIn.sharedInstance.configuration = GIDConfiguration(
        clientID = FIRApp.defaultApp()?.options?.clientID ?: return false
    )
    return true
}