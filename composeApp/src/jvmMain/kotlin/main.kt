
import android.app.Application
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPlacement
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import chat.enrichment.eu.SharedBuildConfig
import chat.enrichment.shared.ui.base.LocalScreenSize
import chatenrichment.composeapp.generated.resources.Res
import chatenrichment.composeapp.generated.resources.app_name
import com.google.firebase.Firebase
import com.google.firebase.FirebaseOptions
import com.google.firebase.FirebasePlatform
import com.google.firebase.initialize
import com.russhwolf.settings.Settings
import com.russhwolf.settings.set
import koin.commonModule
import org.jetbrains.compose.resources.stringResource
import org.koin.core.context.startKoin
import org.koin.mp.KoinPlatform
import java.awt.Button
import java.awt.Dialog
import java.awt.FlowLayout
import java.awt.Frame
import java.awt.GraphicsEnvironment
import java.awt.Label
import java.awt.Toolkit


// paranoid check
private var isAppInitialized = false

/** Initialization of the Jvm application. */
@OptIn(ExperimentalComposeUiApi::class)
fun main() = application {
    if(isAppInitialized.not()) {
        startKoin {
            modules(commonModule)
        }
        initializeFirebase()
        isAppInitialized = true
    }

    val density = LocalDensity.current
    val toolkit = Toolkit.getDefaultToolkit()

    Thread.setDefaultUncaughtExceptionHandler { _, e ->
        Dialog(Frame(), e.message ?: "Error").apply {
            layout = FlowLayout()
            add(Label("We apologize, an unexpected error occurred: ${e.message}"))
            val button = Button("Okay, FINE").apply {
                addActionListener { dispose() }
            }
            add(button)
            setSize(300, 300)
            isVisible = true
        }
    }

    Window(
        onCloseRequest = {
            exitApplication()
        },
        state = rememberWindowState(
            placement = WindowPlacement.Floating,
            width = with(density) {
                toolkit?.screenSize?.width?.div(2)?.toDp() ?: 600.dp
            },
            height = with(density) {
                (toolkit?.screenSize?.height?.minus(
                    toolkit.getScreenInsets(
                        GraphicsEnvironment.getLocalGraphicsEnvironment()
                            .defaultScreenDevice
                            .defaultConfiguration
                    )?.bottom ?: 0
                ))?.toDp() ?: 600.dp
            },
            position = WindowPosition.Aligned(Alignment.TopEnd)
        ),
        title = stringResource(Res.string.app_name),
        visible = true,
        undecorated = false,
        resizable = true,
        alwaysOnTop = false
    ) {
        val containerSize = LocalWindowInfo.current.containerSize

        CompositionLocalProvider(
            LocalScreenSize provides IntSize(
                height = with(density) { containerSize.height.toDp() }.value.toInt(),
                width = with(density) { containerSize.width.toDp() }.value.toInt()
            )
        ) {
            App()
        }
    }
}



/**
 * initializes Firebase, which is specific to JVM,
 * see https://github.com/GitLiveApp/firebase-java-sdk?tab=readme-ov-file#initializing-the-sdk
 */
private fun initializeFirebase(setting: Settings = KoinPlatform.getKoin().get<Settings>()) {
    FirebasePlatform.initializeFirebasePlatform(
        object : FirebasePlatform() {
            override fun store(key: String, value: String) {
                setting[key] = value
            }
            override fun retrieve(key: String) = setting.getString(key, "").ifEmpty { null }
            override fun clear(key: String) {
                setting.remove(key)
            }
            override fun log(msg: String) = println(msg)
        }
    )

    val options: FirebaseOptions = FirebaseOptions.Builder()
        .setProjectId(SharedBuildConfig.FirebaseProjectId)
        .setApplicationId(SharedBuildConfig.AndroidAppId)
        .setApiKey(SharedBuildConfig.CloudWebApiKey)
        .build()

    Firebase.initialize(Application(), options)
}