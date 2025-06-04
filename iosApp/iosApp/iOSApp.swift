import SwiftUI
import ComposeApp
import FirebaseMessaging
import CryptoKit

class AppDelegate: NSObject,
                   UNUserNotificationCenterDelegate,
                   UIApplicationDelegate {

    func application(
        _ application: UIApplication,
        didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey : Any]? = nil
    ) -> Bool {
        // while Swift is great, Kotlin is simply better, we call initialization of everything in Helper.kt - in Kotlin
        return HelperKt.onIOSApplication(self, application: application, launchOptions: launchOptions)
    }

    func application(_ application: UIApplication,
                     didRegisterForRemoteNotificationsWithDeviceToken deviceToken: Data) {
        print("Registered for Apple Remote Notifications")
        Messaging.messaging().setAPNSToken(deviceToken, type: .unknown)
        HelperKt.onAPNSTokenSet()
    }
    
    func application(
        _ application: UIApplication,
        didFailToRegisterForRemoteNotificationsWithError error: Error
    ) {
        print("Failed to register for remote notifications: \(error.localizedDescription)")
    }

    func userNotificationCenter(_ center: UNUserNotificationCenter, willPresent notification: UNNotification) async-> UNNotificationPresentationOptions {
        try! await HelperKt.onNewNotificationRequest(self, center: center, notification: notification)
        return [.list, .banner, .sound, .badge]
    }

    func applicationWillTerminate(_ application: UIApplication) {
        HelperKt.onAppTerminate()
    }
}


@main
struct iOSApp: App {
    @UIApplicationDelegateAdaptor(AppDelegate.self) var delegate

    init() {
        HelperKt.onAppInitialized()
    }

    var body: some Scene {
        WindowGroup {
            ContentView()
                .onOpenURL(perform: { url in
                    HelperKt.onNewUrl(path: url.path)
                })
        }
    }
}
