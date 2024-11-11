package augmy.interactive.com

import App
import android.Manifest.permission.POST_NOTIFICATIONS
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.IntSize
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import augmy.interactive.shared.ui.base.LocalScreenSize
import io.github.vinceglb.filekit.core.FileKit
import koin.commonModule
import org.koin.android.ext.android.getKoin
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin
import ui.home.HomeViewModel

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

    override fun onCreate(savedInstanceState: Bundle?) {
        startKoin {
            androidContext(this@MainActivity)
            androidLogger()
            modules(commonModule)
        }
        installSplashScreen()
        handleDeepLink(intent)

        super.onCreate(savedInstanceState)
        FileKit.init(this)
        askForPermissions()

        setContent {
            val configuration = LocalConfiguration.current

            CompositionLocalProvider(
                LocalScreenSize provides IntSize(
                    height = configuration.screenHeightDp,
                    width = configuration.screenWidthDp
                )
            ) {
                App()
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleDeepLink(intent)
    }

    private fun handleDeepLink(intent: Intent) {
        if(intent.data != null) {
            val viewModel: HomeViewModel = getKoin().get()
            viewModel.emitDeepLink(intent.data?.path)
        }
    }
}