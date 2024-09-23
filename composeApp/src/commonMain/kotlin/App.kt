
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.navigation.compose.rememberNavController
import base.ChatEnrichmentTheme
import chat.enrichment.shared.ui.base.BaseSnackbarHost
import chat.enrichment.shared.ui.base.LocalDeviceType
import chat.enrichment.shared.ui.base.LocalNavController
import chat.enrichment.shared.ui.base.LocalSnackbarHost
import chat.enrichment.shared.ui.theme.LocalTheme
import data.io.app.ThemeChoice
import data.shared.SharedViewModel
import org.jetbrains.compose.ui.tooling.preview.Preview

@OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
@Composable
@Preview
fun App(viewModel: SharedViewModel) {
    val localSettings = viewModel.localSettings.collectAsState()
    val windowSizeClass = calculateWindowSizeClass()
    val snackbarHostState = remember { SnackbarHostState() }

    ChatEnrichmentTheme(
        isDarkTheme = when(localSettings.value?.theme) {
            ThemeChoice.DARK -> true
            ThemeChoice.LIGHT -> false
            else -> isSystemInDarkTheme()
        }
    ) {
        Scaffold(
            snackbarHost = {
                BaseSnackbarHost(hostState = snackbarHostState)
            },
            containerColor = LocalTheme.current.colors.brandMainDark,
            contentColor = LocalTheme.current.colors.brandMainDark
        ) { _ ->
            val navController = rememberNavController()

            CompositionLocalProvider(
                LocalNavController provides navController,
                LocalSnackbarHost provides snackbarHostState,
                LocalDeviceType provides windowSizeClass.widthSizeClass
            ) {
                NavigationHost(navController = navController)
            }
        }
    }
}