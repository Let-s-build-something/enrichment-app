package base

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FabPosition
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import augmy.composeapp.generated.resources.Res
import augmy.composeapp.generated.resources.hint_unauthorized_matrix_action
import augmy.composeapp.generated.resources.hint_unauthorized_matrix_text
import augmy.composeapp.generated.resources.hint_unauthorized_matrix_title
import augmy.interactive.shared.ext.scalingClickable
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
import base.theme.Colors
import components.navigation.VerticalAppBar
import components.notification.InfoHintBox
import data.shared.SharedViewModel
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import ui.account.profile.DisplayNameChangeLauncher

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
    val sharedViewModel: SharedViewModel = koinViewModel()
    val navController = LocalNavController.current
    val dispatcher = LocalBackPressDispatcher.current

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

    BackHandlerOverride {
        dispatcher?.executeBackPress()
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

    Column {
        // information about being missing display name
        AnimatedVisibility(
            currentUser.value != null
                    && currentUser.value?.displayName == null
                    && !sharedViewModel.awaitingAutologin
        ) {
            InformationLine()
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
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun InformationLine() {
    val density = LocalDensity.current
    val showNameChangeLauncher = remember { mutableStateOf(false) }

    if(showNameChangeLauncher.value) {
        DisplayNameChangeLauncher {
            showNameChangeLauncher.value = false
        }
    }

    InfoHintBox(
        modifier = Modifier
            .scalingClickable(scaleInto = .95f) {
                showNameChangeLauncher.value = true
            }
            .fillMaxWidth(),
        icon = {
            Text(
                text = stringResource(Res.string.hint_unauthorized_matrix_action),
                style = LocalTheme.current.styles.title.copy(
                    color = Colors.ProximityContacts,
                    fontSize = with(density) { 36.dp.toSp() }
                )
            )
        },
        title = stringResource(Res.string.hint_unauthorized_matrix_title),
        text = stringResource(Res.string.hint_unauthorized_matrix_text)
    )
}
