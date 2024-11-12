
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.navigation.compose.rememberNavController
import augmy.interactive.shared.ext.ifNull
import augmy.interactive.shared.ui.base.BaseSnackbarHost
import augmy.interactive.shared.ui.base.LocalDeviceType
import augmy.interactive.shared.ui.base.LocalNavController
import augmy.interactive.shared.ui.base.LocalSnackbarHost
import augmy.interactive.shared.ui.theme.LocalTheme
import base.AugmyTheme
import base.navigation.NavigationNode
import data.io.app.ThemeChoice
import kotlinx.coroutines.flow.collectLatest
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.koin.compose.viewmodel.koinViewModel
import ui.dev.DeveloperContent
import ui.home.HomeViewModel

@OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
@Composable
@Preview
fun App(viewModel: HomeViewModel = koinViewModel()) {
    val localSettings = viewModel.localSettings.collectAsState()
    val windowSizeClass = calculateWindowSizeClass()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.initApp()
    }

    AugmyTheme(
        isDarkTheme = when(localSettings.value?.theme) {
            ThemeChoice.DARK -> true
            ThemeChoice.LIGHT -> false
            else -> isSystemInDarkTheme()
        }
    ) {
        Scaffold(
            snackbarHost = {
                BaseSnackbarHost(hostState = snackbarHostState)
            },
            containerColor = LocalTheme.current.colors.brandMainDark,
            contentColor = LocalTheme.current.colors.brandMainDark
        ) { _ ->
            val navController = rememberNavController()

            CompositionLocalProvider(
                LocalNavController provides navController,
                LocalSnackbarHost provides snackbarHostState,
                LocalDeviceType provides windowSizeClass.widthSizeClass
            ) {
                AppContent(viewModel, navController)
            }
        }
    }
}

@Composable
fun AppContent(
    viewModel: HomeViewModel,
    navController: androidx.navigation.NavHostController
) {
    val currentUser = viewModel.firebaseUser.collectAsState(null)

    val isInternalUser = try {
        currentUser.value?.email
    } catch(e: NotImplementedError) {
        "@augmy.org" // allow all JVM for now
    }?.endsWith("@augmy.org") == true

    val modalDeepLink = rememberSaveable(viewModel) {
        mutableStateOf<String?>(null)
    }


    LaunchedEffect(Unit) {
        viewModel.newDeeplink.collectLatest {
            try {
                NavigationNode.allNodes.find { node ->
                    node.deepLink?.let { link ->
                        it.contains(link)
                    } ?: false
                }?.let { node ->
                    navController.navigate(node)
                }.ifNull {
                    modalDeepLink.value = it
                }
            }catch (e: IllegalArgumentException) {
                modalDeepLink.value = it
            }
        }
    }

    ModalHost(
        deepLink = modalDeepLink.value,
        onDismissRequest = {
            modalDeepLink.value = null
        }
    )

    if(LocalDeviceType.current == WindowWidthSizeClass.Compact) {
        Column {
            if(isInternalUser) DeveloperContent()
            Box {
                NavigationHost(navController = navController)
            }
        }
    }else {
        Row {
            if(isInternalUser) DeveloperContent()
            Box {
                NavigationHost(navController = navController)
            }
        }
    }
}