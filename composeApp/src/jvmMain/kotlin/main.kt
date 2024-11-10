
import android.app.Application
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.Text
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowDecoration
import androidx.compose.ui.window.WindowPlacement
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import augmy.composeapp.generated.resources.Res
import augmy.composeapp.generated.resources.app_name
import augmy.interactive.com.BuildKonfig
import augmy.interactive.shared.ui.base.LocalScreenSize
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
import java.awt.Dimension
import java.awt.FlowLayout
import java.awt.Frame
import java.awt.GraphicsEnvironment
import java.awt.Toolkit
import java.io.PrintWriter
import java.io.StringWriter
import javax.swing.JScrollPane
import javax.swing.JTextArea


// paranoid check
private var isAppInitialized = false

/** Initialization of the Jvm application. */
@OptIn(ExperimentalComposeUiApi::class)

fun main(args: Array<String>) = application {
    if(isAppInitialized.not()) {
        startKoin {
            modules(commonModule)
        }
        initializeFirebase()
        isAppInitialized = true
    }

    /*Dialog(Frame(), "arguments").apply {
        layout = FlowLayout()
        add(Label(args.joinToString(separator = ", ") + " ${System.getProperty("user.dir")}"))
        add(
            Button("Okay, FINE").apply {
                addActionListener { dispose() }
            }
        )
        setSize(1200, 300)
        isAutoRequestFocus = true
        isResizable = true
        isVisible = true
    }*/
    initWindowsRegistry()

    val crashException = remember {
        mutableStateOf<Throwable?>(null)
    }
    val density = LocalDensity.current
    val toolkit = Toolkit.getDefaultToolkit()

    Thread.setDefaultUncaughtExceptionHandler { _, e ->
        Dialog(Frame(), e.message ?: "Error").apply {
            layout = FlowLayout()

            val stringWriter = StringWriter()
            e.printStackTrace(PrintWriter(stringWriter))
            val stackTraceText = "We apologize, an unexpected error occurred:\n\n$stringWriter"

            val errorMessageTextArea = JTextArea(stackTraceText, 10, 80).apply {
                lineWrap = true
                wrapStyleWord = true
                isEditable = false
            }

            val scrollPane = JScrollPane(errorMessageTextArea).apply {
                preferredSize = Dimension(1100, 200)
                verticalScrollBarPolicy = JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED
                horizontalScrollBarPolicy = JScrollPane.HORIZONTAL_SCROLLBAR_NEVER
            }

            add(scrollPane)
            add(
                Button("Okay, FINE").apply {
                    addActionListener { dispose() }
                }
            )
            setSize(1200, 300)
            isAutoRequestFocus = true
            isResizable = true
            isVisible = true
        }
        e.printStackTrace()
        crashException.value = e
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
        decoration = WindowDecoration.SystemDefault,
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
            Crossfade(targetState = crashException.value == null) {
                if(it) {
                    App()
                }else {
                    if(crashException.value != null) {
                        Text(
                            modifier = Modifier.fillMaxWidth(),
                            text = "Error: ${crashException.value?.message}",
                            fontSize = 30.sp
                        )
                    }
                }
            }
        }
    }
}


private fun initWindowsRegistry() {
    if(System.getProperty("os.name").contains("Windows", ignoreCase = true)) {
        try {
            val exePath = System.getProperty("user.dir") + "\\Augmy.exe"

            val commands = listOf(
                """reg add "HKCU\Software\Classes\augmy" /ve /d "Description here" /f""",
                """reg add "HKCU\Software\Classes\augmy" /v "URL Protocol" /f""",
                """reg add "HKCU\Software\Classes\augmy\shell" /f""",
                """reg add "HKCU\Software\Classes\augmy\shell\open" /f""",
                """reg add "HKCU\Software\Classes\augmy\shell\open\command" /ve /d "\"$exePath\" \"%1\"" /f"""
            )

            for (cmd in commands) {
                val process = Runtime.getRuntime().exec(cmd)
                process.waitFor()
                if (process.exitValue() != 0) {
                    println("Command failed: $cmd")
                    process.errorStream.bufferedReader()
                        .use { it.lines().forEach { line -> println(line) } }
                } else {
                    println("Command succeeded: $cmd")
                }
            }
        }catch (e: Exception) {
            e.printStackTrace()
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
        .setProjectId(BuildKonfig.FirebaseProjectId)
        .setApplicationId(BuildKonfig.AndroidAppId)
        .setApiKey(BuildKonfig.CloudWebApiKey)
        .setStorageBucket(BuildKonfig.StorageBucketName)
        .build()

    Firebase.initialize(Application(), options)
}