package base.navigation

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.PersonAddAlt
import androidx.compose.material.icons.outlined.PersonOutline
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.currentBackStackEntryAsState
import augmy.composeapp.generated.resources.Res
import augmy.composeapp.generated.resources.screen_account_title
import augmy.composeapp.generated.resources.screen_login
import augmy.interactive.shared.ui.base.LocalDeviceType
import augmy.interactive.shared.ui.base.LocalNavController
import augmy.interactive.shared.ui.components.DEFAULT_ANIMATION_LENGTH_SHORT
import augmy.interactive.shared.ui.components.navigation.ActionBarIcon
import augmy.interactive.shared.ui.theme.LocalTheme
import org.jetbrains.compose.resources.stringResource

/**
 * The default layout of action in top action bar
 * @param isUserSignedIn whether user is currently signed in or not
 * @param userPhotoUrl url of user photo which should be displayed instead of the vector image
 * @param expanded whether the menu is expanded or not
 */
@Composable
fun DefaultAppBarActions(
    isUserSignedIn: Boolean = false,
    userPhotoUrl: String? = null,
    expanded: Boolean = true
) {
    val navController = LocalNavController.current
    val isPhone = LocalDeviceType.current == WindowWidthSizeClass.Compact

    val currentEntry = navController?.currentBackStackEntryAsState()

    // first action
    Crossfade(
        targetState = currentEntry?.value?.destination?.route,
        animationSpec = tween(
            durationMillis = DEFAULT_ANIMATION_LENGTH_SHORT,
            delayMillis = DEFAULT_ANIMATION_LENGTH_SHORT
        )
    ) { route ->
        when(route) {
            // lobby destinations
            NavigationNode.Home.route -> {
                Crossfade(targetState = isUserSignedIn) { showAccount ->
                    if(showAccount) {
                        ActionBarIcon(
                            text = if(expanded && (userPhotoUrl == null || !isPhone)) {
                                stringResource(Res.string.screen_account_title)
                            } else null,
                            imageUrl = userPhotoUrl,
                            imageVector = Icons.Outlined.PersonOutline,
                            onClick = {
                                navController?.navigate(NavigationNode.AccountDashboard)
                            }
                        )
                    }else {
                        ActionBarIcon(
                            text = if(expanded) stringResource(Res.string.screen_login) else null,
                            imageVector = Icons.Outlined.PersonAddAlt,
                            onClick = {
                                navController?.navigate(NavigationNode.Login)
                            }
                        )
                    }
                }
            }
            else -> {
                //enforce the same height of the appbar
                if(isPhone) {
                    ActionBarIcon(
                        modifier = Modifier,
                        enabled = false,
                        imageVector = Icons.Outlined.PersonAddAlt,
                        tint = LocalTheme.current.colors.brandMainDark,
                        text = ""
                    )
                }
            }
        }
    }
}
