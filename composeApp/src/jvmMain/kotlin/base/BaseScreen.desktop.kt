package base

import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.IntSize

actual val LocalScreenSize: ProvidableCompositionLocal<IntSize> = staticCompositionLocalOf {
    IntSize(0, 0)
}