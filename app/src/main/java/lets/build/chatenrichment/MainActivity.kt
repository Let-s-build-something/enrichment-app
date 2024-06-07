package lets.build.chatenrichment

import android.annotation.SuppressLint
import android.app.Activity
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.squadris.squadris.compose.base.BaseSnackbarHost
import com.squadris.squadris.compose.theme.LocalTheme
import com.squadris.squadris.ext.isTablet
import dagger.hilt.android.AndroidEntryPoint
import lets.build.chatenrichment.navigation.NavigationComponent
import lets.build.chatenrichment.ui.home.HomeScreen
import lets.build.chatenrichment.ui.theme.ChatEnrichmentTheme

/** Main single activity for interaction with the application and hosting the navigation */
@AndroidEntryPoint
class MainActivity: ComponentActivity() {

    @SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        //enableEdgeToEdge()
        setContent {
            ChatEnrichmentTheme {
                val snackbarHostState = remember { SnackbarHostState() }
                val navController = rememberNavController()

                Scaffold(
                    snackbarHost = {
                        BaseSnackbarHost(hostState = snackbarHostState)
                    },
                    containerColor = LocalTheme.current.colors.brandMain,
                    contentColor = LocalTheme.current.colors.brandMain
                ) { _ ->
                    CompositionLocalProvider(
                        LocalNavController provides navController,
                        LocalIsTablet provides isTablet(activity = this),
                        LocalActivity provides this,
                        LocalSnackbarHost provides snackbarHostState,
                        //LocalOverscrollConfiguration provides null,
                    ) {
                        NavHost(
                            navController = navController,
                            startDestination = NavigationComponent.Home
                        ) {
                            composable<NavigationComponent.Home> {
                                HomeScreen()
                            }
                        }
                    }
                }
            }
        }
    }
}

/** current navigation tree and controller */
val LocalNavController = staticCompositionLocalOf<NavController?> { null }

/** current snackbar host for showing snackbars */
val LocalSnackbarHost = staticCompositionLocalOf<SnackbarHostState?> { null }

/** whether device is a tablet or not */
val LocalIsTablet = staticCompositionLocalOf { false }

/** Currently displayed, hosting activity */
val LocalActivity = staticCompositionLocalOf<Activity?> { null }