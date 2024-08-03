package base

import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController

@Composable
fun BaseScreen(
    modifier: Modifier = Modifier
) {

}

/** Current device frame */
val LocalDeviceType = staticCompositionLocalOf { WindowWidthSizeClass.Medium }

/** Default page size based on current device tye */
val LocalNavController = staticCompositionLocalOf<NavHostController?> { null }