package base

import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.material3.FabPosition
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import augmy.interactive.shared.ui.base.BaseScreen
import augmy.interactive.shared.ui.base.LocalNavController
import augmy.interactive.shared.ui.base.PlatformType
import augmy.interactive.shared.ui.base.currentPlatform
import augmy.interactive.shared.ui.theme.LocalTheme
import base.navigation.DefaultAppBarActions
import base.navigation.NavIconType
import base.navigation.NavigationNode
import components.navigation.VerticalAppBar
import data.shared.SharedViewModel
import org.koin.compose.viewmodel.koinViewModel

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
    showDefaultActions: Boolean = actionIcons == null,
    onNavigationIconClick: (() -> Unit)? = null,
    contentModifier: Modifier = Modifier,
    appBarVisible: Boolean = true,
    containerColor: Color? = LocalTheme.current.colors.backgroundLight,
    contentColor: Color = Color.Transparent,
    floatingActionButtonPosition: FabPosition = FabPosition.End,
    floatingActionButton: @Composable () -> Unit = {},
    content: @Composable BoxScope.() -> Unit
) {
    val sharedViewModel: SharedViewModel = koinViewModel()
    val navController = LocalNavController.current

    val firebaseUser = sharedViewModel.firebaseUser.collectAsState(null)
    val currentUser = sharedViewModel.currentUser.collectAsState(null)

    val isPreviousHome = navController?.previousBackStackEntry?.destination?.route == NavigationNode.Home.route
            || (navIconType == NavIconType.HAMBURGER && currentPlatform == PlatformType.Jvm)

    val navIconClick: (() -> Unit)? = when {
        navIconType == NavIconType.HOME || isPreviousHome -> {
            {
                navController?.popBackStack(NavigationNode.Home, inclusive = false)
            }
        }
        onNavigationIconClick != null -> onNavigationIconClick
        else -> null
    }

    val actions: @Composable (expanded: Boolean) -> Unit = { expanded ->
        actionIcons?.invoke(expanded)

        if(showDefaultActions) {
            DefaultAppBarActions(
                isUserSignedIn = firebaseUser.value != null,
                userPhotoUrl = try { firebaseUser.value?.photoURL } catch (e: NotImplementedError) { null },
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
        appBarVisible = appBarVisible,
        containerColor = containerColor,
        contentColor = contentColor,
        onNavigationIconClick = {
            navIconClick?.invoke() ?: if(onBackPressed()) {
                navController?.popBackStack()
            } else { }
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
                actions = actions
            )
        }
    )
}