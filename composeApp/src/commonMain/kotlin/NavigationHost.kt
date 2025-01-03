
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import base.navigation.NavigationNode
import koin.loginModule
import kotlinx.coroutines.flow.collectLatest
import org.koin.core.context.loadKoinModules
import ui.account.AccountDashboardScreen
import ui.account.WaterPleaseScreen
import ui.account.accountDashboardModule
import ui.conversation.ConversationScreen
import ui.conversation.media.MediaDetailScreen
import ui.home.HomeScreen
import ui.login.LoginScreen
import ui.network.NetworkManagementScreen
import ui.network.received.networkManagementModule

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
            HomeScreen()
        }
        composable<NavigationNode.Water> {
            WaterPleaseScreen()
        }
        composable<NavigationNode.SearchNetwork> {
            WaterPleaseScreen()
        }
        composable<NavigationNode.SearchAccount> {
            WaterPleaseScreen()
        }
        composable<NavigationNode.Water> {
            WaterPleaseScreen()
        }
        composable<NavigationNode.AccountDashboard> {
            loadKoinModules(accountDashboardModule)
            AccountDashboardScreen()
        }
        composable<NavigationNode.NetworkManagement> {
            loadKoinModules(networkManagementModule)
            NetworkManagementScreen()
        }
        composable<NavigationNode.MediaDetail> {
            MediaDetailScreen(
                urls = it.arguments?.getStringArray("urls").orEmpty(),
                selectedIndex = it.arguments?.getInt("selectedIndex") ?: 0,
                title = it.arguments?.getString("title"),
                subtitle = it.arguments?.getString("subtitle")
            )
        }
        composable<NavigationNode.Conversation> {
            ConversationScreen(
                conversationId = it.arguments?.getString("conversationId"),
                name = it.arguments?.getString("name")
            )
        }
    }
}

@Composable
fun <T> NavController?.collectResult(
    key: String,
    defaultValue: T,
    listener: (T) -> Unit
) {
    LaunchedEffect(Unit) {
        this@collectResult?.currentBackStackEntry
            ?.savedStateHandle
            ?.run {
                getStateFlow(key, defaultValue).collectLatest {
                    if (it != null) listener(it)
                }
            }
    }
}