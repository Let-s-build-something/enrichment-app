import SwiftUI

@main
struct iOSApp: App {
    init() {
        startKoin {
            modules(koin.CommonModuleKt.initKoin())
        }
    }

    var body: some Scene {
        WindowGroup {
            ContentView()
        }
    }
}
