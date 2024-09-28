import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import base.navigation.NavigationNode
import koin.loginModule
import org.koin.core.context.loadKoinModules
import ui.account.AccountDashboardScreen
import ui.account.accountDashboardModule
import ui.home.HomeScreen
import ui.home.homeModule
import ui.login.LoginScreen

/** Host of the main navigation tree */
@Composable
fun NavigationHost(
    modifier: Modifier = Modifier,
    navController: NavHostController
) {
    NavHost(
        modifier = modifier,
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
            AccountDashboardScreen()
        }
        composable<NavigationNode.AccountDashboard> {
            loadKoinModules(accountDashboardModule)
            AccountDashboardScreen()
        }
    }
}