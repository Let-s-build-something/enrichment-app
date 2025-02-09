
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.rememberNavController
import augmy.composeapp.generated.resources.Res
import augmy.composeapp.generated.resources.button_confirm
import augmy.composeapp.generated.resources.button_dismiss
import augmy.composeapp.generated.resources.leave_app_dialog_message
import augmy.composeapp.generated.resources.leave_app_dialog_show_again
import augmy.composeapp.generated.resources.leave_app_dialog_title
import augmy.interactive.shared.ext.ifNull
import augmy.interactive.shared.ui.base.BaseSnackbarHost
import augmy.interactive.shared.ui.base.LocalBackPressDispatcher
import augmy.interactive.shared.ui.base.LocalDeviceType
import augmy.interactive.shared.ui.base.LocalHeyIamScreen
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
import base.navigation.NavigationNode
import data.io.app.ClientStatus
import data.io.app.ThemeChoice
import data.shared.AppServiceViewModel
import kotlinx.coroutines.flow.collectLatest
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.koin.compose.viewmodel.koinViewModel
import ui.dev.DeveloperContent

@OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
@Composable
@Preview
fun App(viewModel: AppServiceViewModel = koinViewModel()) {
    val localSettings = viewModel.localSettings.collectAsState()
    val windowSizeClass = calculateWindowSizeClass()
    val snackbarHostState = remember { SnackbarHostState() }

    // iOS awaits the APNS in order to retrieve an FCM token
    if(currentPlatform != PlatformType.Native) {
        LaunchedEffect(Unit) {
            viewModel.initApp()
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
                LocalNavController provides navController,
                LocalSnackbarHost provides snackbarHostState,
                LocalDeviceType provides windowSizeClass.widthSizeClass,
                LocalIsMouseUser provides mouseUser.value
            ) {
                AppContent(viewModel, navController)
            }
        }
    }
}

@Composable
private fun AppContent(
    viewModel: AppServiceViewModel,
    navController: androidx.navigation.NavHostController
) {
    val backPressDispatcher = LocalBackPressDispatcher.current
    val deviceType = LocalDeviceType.current
    val currentUser = viewModel.firebaseUser.collectAsState(null)

    val isPhone = LocalDeviceType.current == WindowWidthSizeClass.Compact
    val isInternalUser = try {
        currentUser.value?.email
    } catch(e: NotImplementedError) {
        "@augmy.org" // allow all JVM for now
    }?.endsWith("@augmy.org") == true

    val modalDeepLink = rememberSaveable(viewModel) {
        mutableStateOf<String?>(null)
    }
    val showDialogLeave = rememberSaveable(viewModel) {
        mutableStateOf(false)
    }
    val showDialogAgain = remember {
        mutableStateOf(true)
    }

    OnBackHandler {
        if(currentPlatform == PlatformType.Jvm || !navController.popBackStack()) {
            if(viewModel.showLeaveDialog) {
                showDialogLeave.value = !showDialogLeave.value
            }else {
                backPressDispatcher?.executeSystemBackPress()
            }
        }
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

    LaunchedEffect(Unit) {
        viewModel.clientStatus.collectLatest {
            if(it == ClientStatus.NEW) {
                navController.navigate(NavigationNode.Login)
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
                viewModel.saveDialogSetting(showDialogAgain.value)
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

    ModalHost(
        deepLink = modalDeepLink.value,
        onDismissRequest = {
            modalDeepLink.value = null
        }
    )

    CompositionLocalProvider(
        LocalHeyIamScreen provides (isInternalUser && isPhone),
    ) {
        if(isPhone) {
            Column {
                if(isInternalUser) DeveloperContent(
                    modifier = Modifier.statusBarsPadding(),
                )
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
}
