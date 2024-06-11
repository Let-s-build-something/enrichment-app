package lets.build.chatenrichment.ui

import android.annotation.SuppressLint
import android.app.Activity
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.squadris.squadris.compose.base.BaseSnackbarHost
import com.squadris.squadris.compose.base.LocalActivity
import com.squadris.squadris.compose.base.LocalIsTablet
import com.squadris.squadris.compose.base.LocalNavController
import com.squadris.squadris.compose.base.LocalSnackbarHost
import com.squadris.squadris.compose.theme.LocalTheme
import com.squadris.squadris.ext.isTablet
import lets.build.chatenrichment.navigation.NavigationTree
import lets.build.chatenrichment.ui.account.AccountDashboardScreen
import lets.build.chatenrichment.ui.home.HomeScreen
import lets.build.chatenrichment.ui.login.LoginScreen
import lets.build.chatenrichment.ui.login.password.LoginPasswordScreen

/** Main navigation component for general navigation */
@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@Composable
fun NavigationComponent(activity: Activity) {
    val navController = rememberNavController()
    val snackbarHostState = remember { SnackbarHostState() }

    Scaffold(
        snackbarHost = {
            BaseSnackbarHost(hostState = snackbarHostState)
        },
        containerColor = LocalTheme.current.colors.brandMain,
        contentColor = LocalTheme.current.colors.brandMain
    ) { _ ->
        CompositionLocalProvider(
            LocalNavController provides navController,
            LocalIsTablet provides isTablet(activity = activity),
            LocalActivity provides activity,
            LocalSnackbarHost provides snackbarHostState,
            //LocalOverscrollConfiguration provides null,
        ) {
            NavHost(
                navController = navController,
                startDestination = NavigationTree.Home
            ) {
                composable<NavigationTree.Home> {
                    HomeScreen()
                }
                composable<NavigationTree.Login> {
                    LoginScreen()
                }
                composable<NavigationTree.LoginPassword> {
                    LoginPasswordScreen()
                }
                composable<NavigationTree.AccountDashboard> {
                    AccountDashboardScreen()
                }
            }
        }
    }
}