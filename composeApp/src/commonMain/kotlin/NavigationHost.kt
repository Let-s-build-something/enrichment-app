
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import base.navigation.NavigationNode
import data.io.social.network.conversation.message.MediaIO
import koin.loginModule
import kotlinx.coroutines.flow.collectLatest
import org.koin.core.context.loadKoinModules
import ui.account.AccountDashboardScreen
import ui.account.WaterPleaseScreen
import ui.account.accountDashboardModule
import ui.conversation.ConversationScreen
import ui.conversation.media.MediaDetailScreen
import ui.conversation.message.MessageDetailScreen
import ui.conversation.message.messageDetailModule
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
        composable<NavigationNode.MediaDetail> { args ->
            MediaDetailScreen(
                media = args.arguments?.getStringArray("encodedMedia").orEmpty().mapNotNull { media ->
                    media?.split("|||").let {
                        MediaIO(
                            name = it?.get(0),
                            mimetype = it?.get(1),
                            url = it?.get(2),
                            path = it?.get(3)
                        )
                    }
                }.toTypedArray(),
                selectedIndex = args.arguments?.getInt("selectedIndex") ?: 0,
                title = args.arguments?.getString("title") ?: "",
                subtitle = args.arguments?.getString("subtitle") ?: ""
            )
        }
        composable<NavigationNode.Conversation> {
            ConversationScreen(
                conversationId = it.arguments?.getString("conversationId"),
                name = it.arguments?.getString("name")
            )
        }
        composable<NavigationNode.MessageDetail> {
            loadKoinModules(messageDetailModule)
            MessageDetailScreen(
                messageId = it.arguments?.getString("messageId"),
                title = it.arguments?.getString("title")
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