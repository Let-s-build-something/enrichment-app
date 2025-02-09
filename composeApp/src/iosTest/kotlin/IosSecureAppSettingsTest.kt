import koin.SecureAppSettings
import koin.SecureAppSettingsTest
import koin.secureSettings

class IosSecureAppSettingsTest : SecureAppSettingsTest() {
    override val settings: SecureAppSettings = secureSettings
}