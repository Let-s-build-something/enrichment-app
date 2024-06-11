package lets.build.chatenrichment.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.PersonAddAlt
import androidx.compose.material.icons.outlined.PersonOutline
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.navigation.compose.currentBackStackEntryAsState
import com.squadris.squadris.compose.base.LocalNavController
import com.squadris.squadris.compose.components.navigation.ActionBarIcon
import lets.build.chatenrichment.R

/**
 * The default layout of action in top action bar
 * @param isUserSignedIn whether user is currently signed in or not
 */
@Composable
fun DefaultAppBarActions(
    isUserSignedIn: Boolean = false,
    userPhotoUrl: String? = null
) {
    val navController = LocalNavController.current
    val currentEntry = navController?.currentBackStackEntryAsState()

    // first action
    when(currentEntry?.value?.destination?.id) {
        // lobby destinations
        NavigationTree.Home.id -> {
            /*if(currentEntry.value?.destination?.route != NavigationTree.Settings.route) {
                ActionBarIcon(
                    text = stringResource(id = R.string.screen_settings_title),
                    imageVector = Icons.Outlined.Settings,
                    onClick = {
                        navController.navigate(NavigationTree.Settings)
                    }
                )
            }*/
            if(isUserSignedIn) {
                ActionBarIcon(
                    text = stringResource(id = R.string.screen_account_title),
                    imageUrl = userPhotoUrl,
                    imageVector = Icons.Outlined.PersonOutline,
                    onClick = {
                        navController.navigate(NavigationTree.AccountDashboard)
                    }
                )
            }else {
                ActionBarIcon(
                    text = stringResource(id = R.string.screen_login),
                    imageVector = Icons.Outlined.PersonAddAlt,
                    onClick = {
                        navController.navigate(NavigationTree.Login)
                    }
                )
            }
        }
    }
}