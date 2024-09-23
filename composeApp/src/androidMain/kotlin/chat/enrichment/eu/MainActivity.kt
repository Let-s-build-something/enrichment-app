package chat.enrichment.eu

import App
import android.Manifest.permission.POST_NOTIFICATIONS
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.IntSize
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import chat.enrichment.shared.ui.base.LocalScreenSize
import data.shared.SharedViewModel
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.annotation.KoinExperimentalAPI

class MainActivity: ComponentActivity() {
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { isGranted: Boolean ->
        if (isGranted) {
            // FCM SDK (and your app) can post notifications.
        } else {
            // TODO: Inform user that that your app will not show notifications.
        }
    }

    private fun askForPermissions() {
        // This is only necessary for API level >= 33 (TIRAMISU)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                requestPermissionLauncher.launch(POST_NOTIFICATIONS)
            }
        }
    }

    @OptIn(KoinExperimentalAPI::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        askForPermissions()

        setContent {
            val viewModel: SharedViewModel = koinViewModel()
            val settings = viewModel.localSettings.collectAsState()
            val configuration = LocalConfiguration.current
            val isSystemInDarkTheme = isSystemInDarkTheme()

            LaunchedEffect(Unit) {
                viewModel.initApp(isDeviceDarkTheme = isSystemInDarkTheme)
            }

            installSplashScreen().setKeepOnScreenCondition {
                settings.value != null
            }

            CompositionLocalProvider(
                LocalScreenSize provides IntSize(
                    height = configuration.screenHeightDp,
                    width = configuration.screenWidthDp
                )
            ) {
                if(settings.value != null) {
                    App(viewModel)
                }
            }
        }
    }
}