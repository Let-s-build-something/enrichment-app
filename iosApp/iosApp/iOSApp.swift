import SwiftUI
import koin.appModule
import org.koin.core.context.startKoin

@main
struct iOSApp: App {
    init() {
        startKoin {
            modules(appModule)
        }
    }

    var body: some Scene {
        WindowGroup {
            ContentView()
        }
    }
}