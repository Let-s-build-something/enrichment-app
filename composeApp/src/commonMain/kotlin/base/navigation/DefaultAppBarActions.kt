package base.navigation

import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.PersonAddAlt
import androidx.compose.material.icons.outlined.PersonOutline
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.currentBackStackEntryAsState
import augmy.interactive.shared.ui.base.LocalNavController
import augmy.interactive.shared.ui.components.navigation.ActionBarIcon
import augmy.composeapp.generated.resources.Res
import augmy.composeapp.generated.resources.screen_account_title
import augmy.composeapp.generated.resources.screen_login
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
    val currentEntry = navController?.currentBackStackEntryAsState()

    // first action
    when(currentEntry?.value?.destination?.route) {
        // lobby destinations
        NavigationNode.Home.route -> {
            if(isUserSignedIn) {
                ActionBarIcon(
                    text = if(expanded) stringResource(Res.string.screen_account_title) else null,
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
        else -> {
            //enforce the same height of the appbar
            ActionBarIcon(
                modifier = Modifier.requiredWidth(0.dp),
                imageVector = Icons.Outlined.PersonAddAlt,
                text = ""
            )
        }
    }
}