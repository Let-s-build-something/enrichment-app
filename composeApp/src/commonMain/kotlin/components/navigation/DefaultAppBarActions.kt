package components.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.PersonAddAlt
import androidx.compose.material.icons.outlined.PersonOutline
import androidx.compose.runtime.Composable
import androidx.navigation.compose.currentBackStackEntryAsState
import base.LocalNavController
import base.navigation.NavigationNode
import chatenrichment.composeapp.generated.resources.Res
import chatenrichment.composeapp.generated.resources.screen_account_title
import chatenrichment.composeapp.generated.resources.screen_login
import org.jetbrains.compose.resources.stringResource

/**
 * The default layout of action in top action bar
 * @param isUserSignedIn whether user is currently signed in or not
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
    }
}