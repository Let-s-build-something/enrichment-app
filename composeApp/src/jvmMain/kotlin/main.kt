import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPlacement
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import chat.enrichment.shared.ui.base.LocalScreenSize
import koin.commonModule
import org.koin.core.context.startKoin
import java.awt.GraphicsEnvironment
import java.awt.Toolkit


private var isKoinInitialized = false

@OptIn(ExperimentalComposeUiApi::class)
fun main() = application {
    if(isKoinInitialized.not()) {
        startKoin {
            modules(commonModule)
        }
        isKoinInitialized = true
    }

    val density = LocalDensity.current
    val toolkit = Toolkit.getDefaultToolkit()

    Window(
        onCloseRequest = {
            exitApplication()
        },
        state = rememberWindowState(
            placement = WindowPlacement.Floating,
            width = with(density) {
                toolkit.screenSize.width.div(2).toDp()
            },
            height = with(density) {
                (toolkit.screenSize.height - toolkit.getScreenInsets(
                    GraphicsEnvironment.getLocalGraphicsEnvironment()
                        .defaultScreenDevice
                        .defaultConfiguration
                ).bottom).toDp()
            },
            position = WindowPosition.Aligned(Alignment.TopEnd)
        ),
        title = "Chatrich",
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