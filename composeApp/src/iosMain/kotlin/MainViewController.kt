import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.window.ComposeUIViewController
import augmy.interactive.shared.ui.base.LocalScreenSize

@OptIn(ExperimentalComposeUiApi::class)
fun MainViewController() = ComposeUIViewController {
    val containerSize = LocalWindowInfo.current.containerSize
    val density = LocalDensity.current

    CompositionLocalProvider(
        LocalScreenSize provides IntSize(
            height = with(density) { containerSize.height.toDp() }.value.toInt(),
            width = with(density) { containerSize.width.toDp() }.value.toInt()
        )
    ) {
        App()
    }
}