package base

import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.IntSize
import dev.gitlive.firebase.Firebase
import io.ktor.util.Platform

actual val LocalScreenSize: ProvidableCompositionLocal<IntSize> = staticCompositionLocalOf {
    IntSize(0, 0)
}

/** Platform using this application */
actual val currentPlatform: Platform = Platform.Native

/** Platform specific Firebase instance */
actual val PlatformFirebase: Firebase? = Firebase