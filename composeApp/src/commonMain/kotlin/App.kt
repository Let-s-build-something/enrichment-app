import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import chat.enrichment.shared.ui.base.BaseSnackbarHost
import base.ChatEnrichmentTheme
import chat.enrichment.shared.ui.base.LocalDeviceType
import chat.enrichment.shared.ui.base.LocalNavController
import chat.enrichment.shared.ui.base.LocalSnackbarHost
import base.navigation.NavigationNode
import chat.enrichment.shared.ui.theme.LocalTheme
import koin.loginModule
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.koin.core.context.loadKoinModules
import ui.account.AccountDashboardScreen
import ui.account.accountDashboardModule
import ui.home.HomeScreen
import ui.home.homeModule
import ui.login.LoginScreen

@OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
@Composable
@Preview
fun App() {
    val windowSizeClass = calculateWindowSizeClass()
    val snackbarHostState = remember { SnackbarHostState() }

    //TODO isDarkTheme
    ChatEnrichmentTheme(isDarkTheme = false) {
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
                NavHost(
                    navController = navController,
                    startDestination = NavigationNode.Home
                ) {
                    composable<NavigationNode.Login> {
                        loadKoinModules(loginModule())
                        LoginScreen()
                    }
                    composable<NavigationNode.Home> {
                        loadKoinModules(homeModule)
                        HomeScreen()
                    }
                    composable<NavigationNode.Water> {
                    }
                    composable<NavigationNode.AccountDashboard> {
                        loadKoinModules(accountDashboardModule)
                        AccountDashboardScreen()
                    }
                }
            }
        }
    }
}