
import android.app.Application
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowDecoration
import androidx.compose.ui.window.WindowPlacement
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import augmy.composeapp.generated.resources.Res
import augmy.composeapp.generated.resources.app_name
import augmy.interactive.com.BuildKonfig
import augmy.interactive.shared.ui.base.BackPressDispatcher
import augmy.interactive.shared.ui.base.LocalBackPressDispatcher
import augmy.interactive.shared.ui.base.LocalScreenSize
import com.google.firebase.Firebase
import com.google.firebase.FirebaseOptions
import com.google.firebase.FirebasePlatform
import com.google.firebase.initialize
import com.russhwolf.settings.ExperimentalSettingsApi
import com.russhwolf.settings.coroutines.FlowSettings
import com.russhwolf.settings.coroutines.SuspendSettings
import com.russhwolf.settings.coroutines.toBlockingSettings
import data.shared.AppServiceViewModel
import io.kamel.core.config.KamelConfig
import io.kamel.core.config.takeFrom
import io.kamel.image.config.Default
import io.kamel.image.config.LocalKamelConfig
import koin.commonModule
import koin.settingsModule
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.core.context.unloadKoinModules
import org.koin.mp.KoinPlatform
import java.awt.Button
import java.awt.Dialog
import java.awt.Dimension
import java.awt.FlowLayout
import java.awt.Frame
import java.awt.GraphicsEnvironment
import java.awt.Toolkit
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import javax.swing.JScrollPane
import javax.swing.JTextArea

// paranoid check
private var isAppInitialized = false

/** Initialization of the Jvm application. */
@OptIn(ExperimentalComposeUiApi::class, ExperimentalSettingsApi::class)
fun main(args: Array<String>) = application {
    var arguments: Array<String>? = args
    val coroutineScope = rememberCoroutineScope()

    if(isAppInitialized.not()) {
        startKoin {
            modules(settingsModule)
            initializeFirebase(
                settings = KoinPlatform.getKoin().get<FlowSettings>(),
                scope = coroutineScope
            )
            modules(commonModule)
        }
        isAppInitialized = true
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
                Button("I'll report this to info@augmy.org").apply {
                    addActionListener { dispose() }
                }
            )
            setSize(1000, 500)
            isAutoRequestFocus = true
            isResizable = false
            isVisible = true
        }
        e.printStackTrace()
    }

    val backPressDispatcher = remember {
        object: BackPressDispatcher {
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
                unloadKoinModules(commonModule)
                stopKoin()
                exitApplication()
            }
        }
    }

    val kamelConfig = remember {
        KamelConfig {
            takeFrom(KamelConfig.Default)
        }
    }

    LaunchedEffect(Unit) {
        associateWithDomain()
    }

    Window(
        onCloseRequest = {
            backPressDispatcher.executeBackPress()
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
            position = WindowPosition.Aligned(Alignment.TopStart)
        ),
        title = stringResource(Res.string.app_name),
        visible = true,
        decoration = WindowDecoration.SystemDefault,
        resizable = true,
        alwaysOnTop = false
    ) {
        val containerSize = LocalWindowInfo.current.containerSize

        CompositionLocalProvider(
            LocalKamelConfig provides kamelConfig,
            LocalBackPressDispatcher provides backPressDispatcher,
            LocalScreenSize provides IntSize(
                height = with(density) { containerSize.height.toDp() }.value.toInt(),
                width = with(density) { containerSize.width.toDp() }.value.toInt()
            )
        ) {
            val viewModel: AppServiceViewModel = koinViewModel()

            App(viewModel)

            LaunchedEffect(arguments) {
                delay(500)
                arguments?.firstOrNull()?.let { arg ->
                    viewModel.emitDeepLink(arg)
                    arguments = null
                }
            }
        }
    }
}


private fun associateWithDomain() {
    val os = System.getProperty("os.name").lowercase()

    when {
        os.contains("windows") -> {
            initWindowsRegistry()
        }
        os.contains("linux") -> {
            registerUriSchemeLinux()
        }
    }
}

private fun initWindowsRegistry() {
    try {
        val exePath = System.getProperty("user.dir") + File.separator + "Augmy.exe"

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

private fun registerUriSchemeLinux() {
    val appPath = System.getProperty("user.dir") + File.separator + "Augmy"
    val desktopFilePath = System.getProperty("user.home") + "/.local/share/applications/augmy.desktop"
    val desktopFileContent = """
        [Desktop Entry]
        Name=Augmy
        Exec=$appPath %u
        Type=Application
        MimeType=x-scheme-handler/augmy
        Terminal=false
    """.trimIndent()

    try {
        // Create and write to the .desktop file
        val desktopFile = File(desktopFilePath)
        desktopFile.parentFile.mkdirs() // Ensure directory exists
        desktopFile.writeText(desktopFileContent)

        // Register with xdg-mime and xdg-settings
        val commands = listOf(
            "xdg-mime default augmy.desktop x-scheme-handler/augmy",
            "xdg-settings set default-url-scheme-handler augmy augmy.desktop"
        )

        for (cmd in commands) {
            val process = Runtime.getRuntime().exec(arrayOf("sh", "-c", cmd))
            process.waitFor()
            if (process.exitValue() == 0) {
                println("Command succeeded: $cmd")
            } else {
                println("Command failed: $cmd")
                process.errorStream.bufferedReader().use { it.lines().forEach { line -> println(line) } }
            }
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

/**
 * initializes Firebase, which is specific to JVM,
 * see https://github.com/GitLiveApp/firebase-java-sdk?tab=readme-ov-file#initializing-the-sdk
 */
@OptIn(ExperimentalSettingsApi::class)
private fun initializeFirebase(
    settings: SuspendSettings,
    scope: CoroutineScope
) {
    FirebasePlatform.initializeFirebasePlatform(
        object : FirebasePlatform() {
            override fun store(key: String, value: String) {
                scope.launch {
                    settings.putString(key, value)
                }
            }
            override fun retrieve(key: String) = settings.toBlockingSettings().getString(key, "").ifEmpty { null }
            override fun clear(key: String) {
                scope.launch {
                    settings.remove(key)
                }
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
