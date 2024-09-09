import SwiftUI
import ComposeApp
import FirebaseCore
import FirebaseAuth
import GoogleSignIn

private func configureFirebase() -> Bool {
    FirebaseApp.configure()
    guard let clientID = FirebaseApp.app()?.options.clientID else { return false }
              
    // Create Google Sign In configuration object.
    let config = GIDConfiguration(clientID: clientID)
              
    GIDSignIn.sharedInstance.configuration = config
    return true
}


class AppDelegate: NSObject, UIApplicationDelegate {

    func application(
        _ application: UIApplication,
        didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey : Any]? = nil
    ) -> Bool {
      return configureFirebase()
    }
    
    func application(_ app: UIApplication,
                     open url: URL,
                     options: [UIApplication.OpenURLOptionsKey: Any] = [:]) -> Bool {
      return GIDSignIn.sharedInstance.handle(url)
    }
}


@main
struct iOSApp: App {
    @UIApplicationDelegateAdaptor(AppDelegate.self) var delegate
    
    init() {
        HelperKt.doInitKoin()
    }

    var body: some Scene {
        WindowGroup {
            ContentView()
        }
    }
}

