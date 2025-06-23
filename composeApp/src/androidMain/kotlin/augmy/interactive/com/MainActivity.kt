package augmy.interactive.com

import App
import android.Manifest.permission.POST_NOTIFICATIONS
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.window.BackEvent
import android.window.OnBackAnimationCallback
import android.window.OnBackInvokedDispatcher
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.IntSize
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.WindowCompat
import augmy.interactive.shared.ui.base.BackPressDispatcher
import augmy.interactive.shared.ui.base.DeviceOrientation
import augmy.interactive.shared.ui.base.LocalBackPressDispatcher
import augmy.interactive.shared.ui.base.LocalOrientation
import augmy.interactive.shared.ui.base.LocalScreenSize
import com.android.installreferrer.api.InstallReferrerClient
import com.android.installreferrer.api.InstallReferrerStateListener
import data.io.app.SettingsKeys.KEY_REFEREE_USER_ID
import data.io.app.SettingsKeys.KEY_REFERRER_FINISHED
import data.shared.AppServiceModel
import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.dialogs.init
import koin.commonModule
import koin.settings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.koin.android.ext.android.getKoin
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.compose.KoinContext
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.core.context.unloadKoinModules
import kotlin.system.exitProcess

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
            private lateinit var progressListener: OnBackAnimationCallback

            init {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                    progressListener = object : OnBackAnimationCallback {
                        override fun onBackProgressed(backEvent: BackEvent) {
                            progress.floatValue = backEvent.progress
                            super.onBackProgressed(backEvent)
                        }
                        override fun onBackInvoked() {
                            executeBackPress()
                        }
                    }.also {
                        onBackInvokedDispatcher.registerOnBackInvokedCallback(
                            OnBackInvokedDispatcher.PRIORITY_DEFAULT, it
                        )
                    }
                }
            }

            val listeners = mutableListOf<() -> Unit>()
            override val progress = mutableFloatStateOf(0f)

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
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                    onBackInvokedDispatcher.unregisterOnBackInvokedCallback(progressListener)
                }
                finishAndRemoveTask()
                exitProcess(0)
            }
        }

        setContent {
            val configuration = LocalConfiguration.current
            val containerSize = LocalWindowInfo.current.containerSize
            val density = LocalDensity.current
            val scope = rememberCoroutineScope()

            LaunchedEffect(scope) {
                installReferrer(scope)
            }

            CompositionLocalProvider(
                LocalBackPressDispatcher provides backPressDispatcher,
                LocalScreenSize provides IntSize(
                    height = with(density) { containerSize.height.toDp().value }.toInt(),
                    width = with(density) { containerSize.width.toDp().value }.toInt()
                ),
                LocalOrientation provides if(configuration.orientation == Configuration.ORIENTATION_PORTRAIT) {
                    DeviceOrientation.Vertical
                }else DeviceOrientation.Horizontal
            ) {
                KoinContext { App() }
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
            viewModel.emitDeepLink(intent.data?.toString())
        }
    }

    private fun installReferrer(scope: CoroutineScope) {
        scope.launch {
            if (settings.getBooleanOrNull(KEY_REFERRER_FINISHED) != true
                && settings.getString(KEY_REFEREE_USER_ID, "").isBlank()
            ) {
                val referrerClient = InstallReferrerClient.newBuilder(this@MainActivity).build()
                referrerClient.startConnection(object: InstallReferrerStateListener {
                    override fun onInstallReferrerSetupFinished(responseCode: Int) {
                        if (responseCode == InstallReferrerClient.InstallReferrerResponse.OK) {
                            val response = referrerClient.installReferrer
                            val referrerUrl = response.installReferrer
                            val referralUserId = "https://dummy?$referrerUrl".toUri()
                                .getQueryParameter("ref")

                            if (!referralUserId.isNullOrEmpty()) {
                                scope.launch {
                                    settings.putBoolean(KEY_REFERRER_FINISHED, true)
                                    settings.putString(KEY_REFEREE_USER_ID, referralUserId)
                                }
                            }
                        }
                        referrerClient.endConnection()
                    }

                    override fun onInstallReferrerServiceDisconnected() {}
                })
            }
        }
    }
}