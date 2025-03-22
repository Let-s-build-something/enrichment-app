package augmy.interactive.com

import App
import android.Manifest.permission.POST_NOTIFICATIONS
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
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
import androidx.core.view.WindowCompat
import augmy.interactive.shared.ui.base.BackPressDispatcher
import augmy.interactive.shared.ui.base.DeviceOrientation
import augmy.interactive.shared.ui.base.LocalBackPressDispatcher
import augmy.interactive.shared.ui.base.LocalOrientation
import augmy.interactive.shared.ui.base.LocalScreenSize
import data.shared.AppServiceModel
import io.github.vinceglb.filekit.core.FileKit
import koin.commonModule
import org.koin.android.ext.android.getKoin
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.core.context.unloadKoinModules

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
        WindowCompat.setDecorFitsSystemWindows(window, false)
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

        val backPressDispatcher = object: BackPressDispatcher {
            val listeners = mutableListOf<() -> Unit>()

            override fun addOnBackPressedListener(listener: () -> Unit) {
                this.listeners.add(0, listener)
            }
            override fun removeOnBackPressedListener(listener: () -> Unit) {
                this.listeners.remove(listener)
            }
            override fun executeBackPress() {
                listeners.firstOrNull()?.invoke()
            }
            override fun executeSystemBackPress() {
                this@MainActivity.finish()
            }
        }

        setContent {
            val configuration = LocalConfiguration.current

            CompositionLocalProvider(
                LocalBackPressDispatcher provides backPressDispatcher,
                LocalScreenSize provides IntSize(
                    height = configuration.screenHeightDp,
                    width = configuration.screenWidthDp
                ),
                LocalOrientation provides if(LocalConfiguration.current.orientation == Configuration.ORIENTATION_PORTRAIT) {
                    DeviceOrientation.Vertical
                }else DeviceOrientation.Horizontal
            ) {
                App()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        unloadKoinModules(commonModule)
        stopKoin()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleDeepLink(intent)
    }

    private fun handleDeepLink(intent: Intent) {
        if(intent.data != null) {
            val viewModel: AppServiceModel = getKoin().get()
            viewModel.emitDeepLink(intent.data?.path)
        }
    }
}