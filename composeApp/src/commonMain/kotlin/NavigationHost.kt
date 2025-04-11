
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import augmy.interactive.shared.ui.base.LocalNavController
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
import ui.conversation.settings.ConversationSettingsScreen
import ui.home.HomeScreen
import ui.login.LoginScreen
import ui.network.NetworkManagementScreen
import ui.network.received.networkManagementModule
import ui.search.user.SearchUserScreen
import kotlin.jvm.JvmSuppressWildcards

/** Host of the main navigation tree */
@Composable
fun NavigationHost(
    modifier: Modifier = Modifier,
    navController: NavHostController,
    startDestination: NavigationNode = NavigationNode.Home,
    enterTransition:
    (@JvmSuppressWildcards
    AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition) =
        {
            fadeIn(animationSpec = tween(700))
        },
    exitTransition:
    (@JvmSuppressWildcards
    AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition) =
        {
            fadeOut(animationSpec = tween(700))
        },
    additionalContent: (@Composable () -> Unit)? = null
) {
    CompositionLocalProvider(LocalNavController provides navController) {
        NavHost(
            modifier = modifier,
            navController = navController,
            enterTransition = enterTransition,
            exitTransition = exitTransition,
            startDestination = startDestination
        ) {
            composable<NavigationNode.Login> { args ->
                loadKoinModules(loginModule())
                LoginScreen(
                    nonce = args.arguments?.getString("nonce"),
                    loginToken = args.arguments?.getString("loginToken")
                )
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
            composable<NavigationNode.ConversationSettings> {
                ConversationSettingsScreen(conversationId = it.arguments?.getString("conversationId"))
            }
            composable<NavigationNode.SearchUser> {
                SearchUserScreen(
                    awaitingResult = it.arguments?.getBoolean("awaitingResult"),
                    excludeUsers = it.arguments?.getString("excludeUsers")?.split(",")
                )
            }
            composable<NavigationNode.MessageDetail> {
                loadKoinModules(messageDetailModule)
                MessageDetailScreen(
                    messageId = it.arguments?.getString("messageId"),
                    conversationId = it.arguments?.getString("conversationId"),
                    title = it.arguments?.getString("title")
                )
            }
        }
    }
}

@Composable
private fun CombinedComposable(
    contentOne: (@Composable () -> Unit)? = null,
    contentTwo: @Composable () -> Unit
) {
    if(contentOne != null) {
        Column {
            contentOne()
            contentTwo()
        }
    }else contentTwo()
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