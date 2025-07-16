
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DoorBack
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import androidx.core.uri.UriUtils
import androidx.navigation.compose.rememberNavController
import augmy.composeapp.generated.resources.Res
import augmy.composeapp.generated.resources.button_confirm
import augmy.composeapp.generated.resources.button_dismiss
import augmy.composeapp.generated.resources.hard_logout_action
import augmy.composeapp.generated.resources.hard_logout_message
import augmy.composeapp.generated.resources.leave_app_dialog_message
import augmy.composeapp.generated.resources.leave_app_dialog_show_again
import augmy.composeapp.generated.resources.leave_app_dialog_title
import augmy.interactive.com.BuildKonfig
import augmy.interactive.shared.ext.ifNull
import augmy.interactive.shared.ui.base.BaseSnackbarHost
import augmy.interactive.shared.ui.base.LocalBackPressDispatcher
import augmy.interactive.shared.ui.base.LocalDeviceType
import augmy.interactive.shared.ui.base.LocalIsMouseUser
import augmy.interactive.shared.ui.base.LocalNavController
import augmy.interactive.shared.ui.base.LocalSnackbarHost
import augmy.interactive.shared.ui.base.OnBackHandler
import augmy.interactive.shared.ui.base.PlatformType
import augmy.interactive.shared.ui.base.currentPlatform
import augmy.interactive.shared.ui.components.dialog.AlertDialog
import augmy.interactive.shared.ui.components.dialog.ButtonState
import augmy.interactive.shared.ui.theme.LocalTheme
import base.AugmyTheme
import base.global.InformationLines
import base.global.InformationPopUps
import base.navigation.NavigationNode
import data.io.app.ClientStatus
import data.io.app.ThemeChoice
import data.io.base.AppPingType
import data.shared.AppServiceModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import ui.dev.DeveloperHolderLayout

@OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
@Composable
fun App(model: AppServiceModel = koinViewModel()) {
    val localSettings = model.localSettings.collectAsState()
    val windowSizeClass = calculateWindowSizeClass()
    val snackbarHostState = remember { SnackbarHostState() }

    // iOS awaits the APNS in order to retrieve an FCM token
    if(currentPlatform != PlatformType.Native) {
        LaunchedEffect(Unit) {
            model.initApp()
        }
    }

    // we attempt to detect mouse events
    val mouseUser = rememberSaveable {
        mutableStateOf(false)
    }
    val hoverInteractionSource = if(!mouseUser.value) {
        remember { MutableInteractionSource() }
    }else null
    val isHovered = hoverInteractionSource?.collectIsHoveredAsState()

    LaunchedEffect(isHovered?.value) {
        if(isHovered?.value == true) {
            mouseUser.value = true
        }
    }

    AugmyTheme(
        isDarkTheme = when(localSettings.value?.theme) {
            ThemeChoice.DARK -> true
            ThemeChoice.LIGHT -> false
            else -> isSystemInDarkTheme()
        }
    ) {
        Scaffold(
            modifier = Modifier
                .pointerInput(Unit) {
                }
                .then(
                    if(hoverInteractionSource != null) {
                        Modifier.hoverable(
                            enabled = !mouseUser.value,
                            interactionSource = hoverInteractionSource
                        )
                    }else Modifier
                ),
            snackbarHost = {
                BaseSnackbarHost(hostState = snackbarHostState)
            },
            containerColor = LocalTheme.current.colors.appbarBackground,
            contentColor = LocalTheme.current.colors.appbarBackground
        ) { _ ->
            val navController = rememberNavController()

            CompositionLocalProvider(
                LocalSnackbarHost provides snackbarHostState,
                LocalDeviceType provides windowSizeClass.widthSizeClass,
                LocalIsMouseUser provides mouseUser.value
            ) {
                AppContent(model, navController)
            }
        }
    }
}

@Composable
private fun AppContent(
    model: AppServiceModel,
    navController: androidx.navigation.NavHostController
) {
    val backPressDispatcher = LocalBackPressDispatcher.current
    val deviceType = LocalDeviceType.current
    val snackbarHost = LocalSnackbarHost.current
    val scope = rememberCoroutineScope()

    val isCompact = LocalDeviceType.current == WindowWidthSizeClass.Compact

    val modalDeepLink = rememberSaveable(model) {
        mutableStateOf<String?>(null)
    }
    val showDialogLeave = rememberSaveable(model) {
        mutableStateOf(false)
    }
    val showDialogAgain = remember {
        mutableStateOf(true)
    }

    OnBackHandler {
        if(currentPlatform == PlatformType.Jvm || navController.previousBackStackEntry == null) {
            if(model.showLeaveDialog) {
                showDialogLeave.value = !showDialogLeave.value
            }else {
                backPressDispatcher?.executeSystemBackPress()
            }
        }else navController.popBackStack()
    }

    LaunchedEffect(Unit) {
        model.newDeeplink.collectLatest { deeplink ->
            try {
                NavigationNode.allNodes.find { node ->
                    node.deepLink?.let { link ->
                        deeplink.contains(link)
                    } == true
                }?.let { node ->
                    val link = UriUtils.parse(deeplink)
                    when(node) {
                        is NavigationNode.Login -> {
                            navController.navigate(
                                NavigationNode.Login(
                                    nonce = link.getQueryParameters("nonce").firstOrNull(),
                                    loginToken = link.getQueryParameters("loginToken").firstOrNull()
                                )
                            ) {
                                launchSingleTop = true
                                popUpTo(NavigationNode.Home) {
                                    inclusive = false
                                }
                            }
                        }
                        else -> navController.navigate(link)
                    }
                }.ifNull {
                    modalDeepLink.value = deeplink
                }
            }catch (e: IllegalArgumentException) {
                e.printStackTrace()
                modalDeepLink.value = deeplink
            }
        }
    }

    LaunchedEffect(Unit) {
        model.pingStream.collectLatest { stream ->
            withContext(Dispatchers.Default) {
                if(stream.any { it.type == AppPingType.HardLogout }) {
                    scope.launch {
                        if(snackbarHost?.showSnackbar(
                                message = getString(Res.string.hard_logout_message),
                                actionLabel = getString(Res.string.hard_logout_action),
                                withDismissAction = true,
                            ) == SnackbarResult.ActionPerformed
                        ) {
                            navController.navigate(NavigationNode.Login())
                        }
                    }
                    CoroutineScope(Job()).launch {
                        model.logoutCurrentUser()
                    }
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        model.clientStatus.collectLatest {
            if(it == ClientStatus.NEW) {
                navController.navigate(NavigationNode.Login())
            }
        }
    }

    if(showDialogLeave.value) {
        AlertDialog(
            title = stringResource(Res.string.leave_app_dialog_title),
            message = AnnotatedString(stringResource(Res.string.leave_app_dialog_message)),
            icon = Icons.Outlined.DoorBack,
            confirmButtonState = ButtonState(
                text = stringResource(Res.string.button_confirm),
            ) {
                model.saveDialogSetting(showDialogAgain.value)
                backPressDispatcher?.executeSystemBackPress()
            },
            dismissButtonState = ButtonState(
                text = stringResource(Res.string.button_dismiss),
            ),
            additionalContent = {
                if(deviceType == WindowWidthSizeClass.Expanded) {
                    Row(
                        modifier = Modifier
                            .padding(8.dp)
                            .fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(LocalTheme.current.shapes.betweenItemsSpace),
                    ) {
                        Checkbox(
                            checked = !showDialogAgain.value,
                            onCheckedChange = { isChecked ->
                                showDialogAgain.value = !isChecked
                            },
                            colors = LocalTheme.current.styles.checkBoxColorsDefault
                        )
                        Text(
                            text = stringResource(Res.string.leave_app_dialog_show_again),
                        )
                    }
                }
            },
            onDismissRequest = {
                showDialogLeave.value = false
            }
        )
    }

    CompositionLocalProvider(LocalNavController provides navController) {
        ModalHost(
            deepLink = modalDeepLink.value,
            onDismissRequest = {
                modalDeepLink.value = null
            }
        )
    }

    Column {
        if(isCompact) {
            val content = @Composable {
                InformationPopUps()
                InformationLines(sharedModel = model)
                Box {
                    NavigationHost(navController = navController)
                }
            }

            if (BuildKonfig.isDevelopment) DeveloperHolderLayout(
                modifier = Modifier.statusBarsPadding(),
                appContent = content
            ) else content()
        }else {
            InformationPopUps()
            InformationLines(sharedModel = model)


            val content = @Composable {
                Box {
                    NavigationHost(navController = navController)
                }
            }

            if(BuildKonfig.isDevelopment) {
                DeveloperHolderLayout(appContent = content)
            }else content()
        }
    }
}
