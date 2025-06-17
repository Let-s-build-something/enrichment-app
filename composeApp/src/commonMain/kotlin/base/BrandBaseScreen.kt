package base

import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.material3.FabPosition
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import augmy.interactive.shared.ui.base.BackHandlerOverride
import augmy.interactive.shared.ui.base.BaseScreen
import augmy.interactive.shared.ui.base.LocalBackPressDispatcher
import augmy.interactive.shared.ui.base.LocalNavController
import augmy.interactive.shared.ui.base.PlatformType
import augmy.interactive.shared.ui.base.currentPlatform
import augmy.interactive.shared.ui.theme.LocalTheme
import base.navigation.DefaultAppBarActions
import base.navigation.NavIconType
import base.navigation.NavigationNode
import components.navigation.VerticalAppBar
import data.shared.SharedModel
import org.koin.compose.viewmodel.koinViewModel
import utils.SharedLogger

/**
 * Screen with a brand-specific layout and behavior.
 * This includes navigation specific to the brand, actions specific to the brand,
 * such as profile picture of a user in the top bar.
 *
 * @param navIconType type of navigation icon to be shown
 * @param title title of the screen
 * @param subtitle subtitle of the screen
 * @param onBackPressed event to be triggered when back button is pressed
 * @param actionIcons actions to be shown in the top bar
 * @param onNavigationIconClick event to be triggered when navigation icon is clicked
 * @param contentModifier modifier for the content
 * @param appBarVisible whether the app bar should be visible
 * @param clearFocus whether click within the content should clear current focus
 * @param containerColor color of the container
 * @param contentColor color of the content
 * @param floatingActionButtonPosition position of the floating action button
 * @param floatingActionButton floating action button
 * @param content content of the screen under app bar and any other framing elements
 */
@Composable
fun BrandBaseScreen(
    modifier: Modifier = Modifier,
    navIconType: NavIconType? = null,
    title: String? = null,
    subtitle: String? = null,
    onBackPressed: () -> Boolean = { true },
    actionIcons: (@Composable (expanded: Boolean) -> Unit)? = null,
    headerPrefix: @Composable RowScope.() -> Unit = {},
    showDefaultActions: Boolean = actionIcons == null,
    onNavigationIconClick: (() -> Unit)? = null,
    contentModifier: Modifier = Modifier,
    appBarVisible: Boolean = true,
    clearFocus: Boolean = true,
    containerColor: Color? = LocalTheme.current.colors.backgroundLight,
    contentColor: Color = Color.Transparent,
    floatingActionButtonPosition: FabPosition = FabPosition.End,
    floatingActionButton: @Composable () -> Unit = {},
    content: @Composable BoxScope.() -> Unit
) {
    val sharedModel: SharedModel = koinViewModel()
    val navController = LocalNavController.current
    val dispatcher = LocalBackPressDispatcher.current

    val currentUser = sharedModel.currentUser.collectAsState()

    val isPreviousHome = navController?.previousBackStackEntry?.destination?.route == NavigationNode.Home.route
            || (navIconType == NavIconType.HAMBURGER && currentPlatform == PlatformType.Jvm)

    val navIconClick: () -> Unit = when {
        onNavigationIconClick != null -> onNavigationIconClick
        navIconType == NavIconType.HOME || isPreviousHome -> {
            {
                navController?.popBackStack(NavigationNode.Home, inclusive = false)
            }
        }
        navController?.currentDestination?.route != NavigationNode.Home.route
                && navController?.previousBackStackEntry == null -> {
            {
                if(onBackPressed()) {
                    navController?.navigate(NavigationNode.Home) {
                        popUpTo(NavigationNode.Home) {
                            inclusive = false
                        }
                        launchSingleTop = true
                    }
                }
            }
        }
        else -> {
            {
                if (navController?.previousBackStackEntry != null) navController.popBackStack()
            }
        }
    }

    BackHandlerOverride {
        dispatcher?.executeBackPress()
    }

    SideEffect {
        SharedLogger.logger.debug { "BrandBaseScreen, currentUser outside of actions: ${currentUser.value}" }
    }
    val actions: @Composable (expanded: Boolean) -> Unit = { expanded ->
        actionIcons?.invoke(expanded)

        SideEffect {
            SharedLogger.logger.debug { "BrandBaseScreen, currentUser inside of actions: ${currentUser.value}" }
        }
        if(showDefaultActions) {
            DefaultAppBarActions(
                isUserSignedIn = currentUser.value != null,
                avatarUrl = currentUser.value?.avatarUrl,
                expanded = expanded,
                userTag = currentUser.value?.tag
            )
        }
    }

    BaseScreen(
        modifier = modifier,
        title = title,
        subtitle = subtitle,
        contentModifier = contentModifier,
        actionIcons = actions,
        headerPrefix = headerPrefix,
        appBarVisible = appBarVisible,
        containerColor = containerColor,
        contentColor = contentColor,
        clearFocus = clearFocus,
        onNavigationIconClick = {
            navIconClick.invoke()
        },
        floatingActionButtonPosition = floatingActionButtonPosition,
        floatingActionButton = floatingActionButton,
        navigationIcon = navIconType?.imageVector ?: if(isPreviousHome) {
            NavIconType.HOME.imageVector
        }else NavIconType.BACK.imageVector,
        content = content,
        verticalAppBar = {
            VerticalAppBar(
                modifier = Modifier
                    .fillMaxHeight()
                    .wrapContentWidth(),
                actions = actions,
                model = sharedModel
            )
        }
    )
}
