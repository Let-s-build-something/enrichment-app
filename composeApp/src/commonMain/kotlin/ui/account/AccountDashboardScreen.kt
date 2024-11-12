package ui.account

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.outlined.Brush
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Handshake
import androidx.compose.material.icons.outlined.IosShare
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withLink
import androidx.compose.ui.unit.dp
import augmy.composeapp.generated.resources.Res
import augmy.composeapp.generated.resources.accessibility_change_avatar
import augmy.composeapp.generated.resources.accessibility_change_username
import augmy.composeapp.generated.resources.accessibility_share
import augmy.composeapp.generated.resources.account_dashboard_sign_out
import augmy.composeapp.generated.resources.account_dashboard_theme
import augmy.composeapp.generated.resources.account_dashboard_theme_dark
import augmy.composeapp.generated.resources.account_dashboard_theme_device
import augmy.composeapp.generated.resources.account_dashboard_theme_light
import augmy.composeapp.generated.resources.account_settings_content_invisible
import augmy.composeapp.generated.resources.account_settings_content_offline
import augmy.composeapp.generated.resources.account_settings_content_online
import augmy.composeapp.generated.resources.account_settings_private_content
import augmy.composeapp.generated.resources.account_settings_private_title
import augmy.composeapp.generated.resources.account_settings_public_content
import augmy.composeapp.generated.resources.account_settings_public_title
import augmy.composeapp.generated.resources.account_settings_title_invisible
import augmy.composeapp.generated.resources.account_settings_title_offline
import augmy.composeapp.generated.resources.account_settings_title_online
import augmy.composeapp.generated.resources.account_sign_out_message
import augmy.composeapp.generated.resources.account_username_empty
import augmy.composeapp.generated.resources.action_link_copied
import augmy.composeapp.generated.resources.button_dismiss
import augmy.composeapp.generated.resources.button_yes
import augmy.composeapp.generated.resources.network_action_share
import augmy.composeapp.generated.resources.screen_account_title
import augmy.composeapp.generated.resources.screen_network_management
import augmy.interactive.shared.ui.base.LocalNavController
import augmy.interactive.shared.ui.base.LocalSnackbarHost
import augmy.interactive.shared.ui.base.ModalScreenContent
import augmy.interactive.shared.ui.components.ErrorHeaderButton
import augmy.interactive.shared.ui.components.MinimalisticBrandIcon
import augmy.interactive.shared.ui.components.MinimalisticComponentIcon
import augmy.interactive.shared.ui.components.MultiChoiceSwitch
import augmy.interactive.shared.ui.components.dialog.AlertDialog
import augmy.interactive.shared.ui.components.dialog.ButtonState
import augmy.interactive.shared.ui.components.navigation.ActionBarIcon
import augmy.interactive.shared.ui.components.rememberTabSwitchState
import augmy.interactive.shared.ui.theme.LocalTheme
import base.BrandBaseScreen
import base.navigation.NavigationNode
import components.RowSetting
import components.UserProfileImage
import data.io.social.UserPrivacy
import data.io.social.UserVisibility
import koin.HttpDomain
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import ui.account.profile.DialogPictureChange
import ui.account.profile.UsernameChangeLauncher

/**
 * Screen for the home page
 */
@Composable
fun AccountDashboardScreen(viewModel: AccountDashboardViewModel = koinViewModel()) {
    val navController = LocalNavController.current

    val currentUser = viewModel.currentUser.collectAsState(null)
    val signOutResponse = viewModel.signOutResponse.collectAsState()

    val showSignOutDialog = remember {
        mutableStateOf(false)
    }

    LaunchedEffect(signOutResponse.value, currentUser.value) {
        if(signOutResponse.value && currentUser.value == null) {
            navController?.popBackStack(NavigationNode.Home, inclusive = false)
        }
    }

    if(showSignOutDialog.value) {
        AlertDialog(
            message = stringResource(Res.string.account_sign_out_message),
            confirmButtonState = ButtonState(
                text = stringResource(Res.string.button_yes),
                onClick = {
                    viewModel.logoutCurrentUser()
                }
            ),
            dismissButtonState = ButtonState(text = stringResource(Res.string.button_dismiss)),
            icon = Icons.AutoMirrored.Outlined.Logout,
            onDismissRequest = {
                showSignOutDialog.value = false
            }
        )
    }

    BrandBaseScreen(
        title = stringResource(Res.string.screen_account_title),
        actionIcons = { isExpanded ->
            ActionBarIcon(
                text = if(isExpanded) stringResource(Res.string.screen_network_management) else null,
                imageVector = Icons.Outlined.Handshake,
                onClick = {
                    navController?.navigate(NavigationNode.NetworkManagement())
                }
            )
        }
    ) {
        ModalScreenContent(
            modifier = Modifier
                .padding(start = 16.dp, end = 16.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(
                LocalTheme.current.shapes.betweenItemsSpace * 2
            )
        ) {
            ProfileSection(viewModel)

            SettingsSection(viewModel)

            ErrorHeaderButton(
                modifier = Modifier
                    .padding(top = 32.dp)
                    .fillMaxWidth(),
                text = stringResource(Res.string.account_dashboard_sign_out),
                endIconVector = Icons.AutoMirrored.Outlined.Logout,
                onClick = {
                    showSignOutDialog.value = true
                }
            )
        }
    }
}

@Composable
private fun ColumnScope.SettingsSection(viewModel: AccountDashboardViewModel) {
    val localSettings = viewModel.localSettings.collectAsState()
    val currentUser = viewModel.currentUser.collectAsState()
    val privacyResponse = viewModel.privacyResponse.collectAsState()
    val visibilityResponse = viewModel.visibilityResponse.collectAsState()

    val privacy = currentUser.value?.configuration?.privacy ?: UserPrivacy.PUBLIC
    val visibility = currentUser.value?.configuration?.visibility ?: UserVisibility.ONLINE

    val switchThemeState = rememberTabSwitchState(
        tabs = mutableListOf(
            stringResource(Res.string.account_dashboard_theme_light),
            stringResource(Res.string.account_dashboard_theme_dark),
            stringResource(Res.string.account_dashboard_theme_device)
        ),
        selectedTabIndex = mutableStateOf(localSettings.value?.theme?.ordinal ?: 0),
        onSelectionChange = {
            viewModel.updateTheme(it)
        }
    )

    LaunchedEffect(localSettings.value?.theme) {
        switchThemeState.selectedTabIndex.value = localSettings.value?.theme?.ordinal ?: 0
    }

    RowSetting(
        response = privacyResponse.value,
        title = stringResource(if(privacy == UserPrivacy.PRIVATE) {
            Res.string.account_settings_private_title
        }else Res.string.account_settings_public_title),
        content = stringResource(if(privacy == UserPrivacy.PRIVATE) {
            Res.string.account_settings_private_content
        }else Res.string.account_settings_public_content),
        lottieFileName = "private_public",
        scale = 2f,
        progressValue = if(privacy == UserPrivacy.PRIVATE) .5f else 0f,
        onTap = {
            if(privacyResponse.value == null) {
                viewModel.requestPrivacyChange(
                    if(currentUser.value?.configuration?.privacy == UserPrivacy.PUBLIC) {
                        UserPrivacy.PRIVATE
                    }else UserPrivacy.PUBLIC
                )
            }
        }
    )

    RowSetting(
        response = visibilityResponse.value,
        title = stringResource(when(visibility) {
            UserVisibility.OFFLINE -> Res.string.account_settings_title_offline
            UserVisibility.INVISIBLE -> Res.string.account_settings_title_invisible
            else -> Res.string.account_settings_title_online
        }),
        content = stringResource(when(visibility) {
            UserVisibility.OFFLINE -> Res.string.account_settings_content_offline
            UserVisibility.INVISIBLE -> Res.string.account_settings_content_invisible
            else -> Res.string.account_settings_content_online
        }),
        lottieFileName = "online_offline",
        progressValue = if(visibility == UserVisibility.OFFLINE) 0f else .3f,
        tint = if(visibility == UserVisibility.INVISIBLE) LocalTheme.current.colors.disabled else null,
        onTap = {
            if(visibilityResponse.value == null) {
                val v = currentUser.value?.configuration?.visibility
                viewModel.requestVisibilityChange(
                    if(v?.ordinal == 2) {
                        UserVisibility.entries.first()
                    }else UserVisibility.entries[(v?.ordinal ?: 0) + 1]
                )
            }
        }
    )

    Column(Modifier.fillMaxWidth()) {
        Text(
            modifier = Modifier.align(Alignment.Start),
            text = stringResource(Res.string.account_dashboard_theme),
            style = LocalTheme.current.styles.category
        )
        MultiChoiceSwitch(
            modifier = Modifier.fillMaxWidth(),
            shape = LocalTheme.current.shapes.rectangularActionShape,
            state = switchThemeState
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ColumnScope.ProfileSection(viewModel: AccountDashboardViewModel) {
    val firebaseUser = viewModel.firebaseUser.collectAsState(null)
    val currentUser = viewModel.currentUser.collectAsState(null)

    val isUsernameInEdit = rememberSaveable {
        mutableStateOf(false)
    }
    val isPictureInEdit = rememberSaveable {
        mutableStateOf(false)
    }

    val clipboardManager = LocalClipboardManager.current
    val snackbarHostState = LocalSnackbarHost.current
    val coroutineScope = rememberCoroutineScope()

    if(isUsernameInEdit.value) {
        UsernameChangeLauncher {
            isUsernameInEdit.value = false
        }
    }
    if(isPictureInEdit.value) {
        DialogPictureChange(
            onDismissRequest = {
                isPictureInEdit.value = false
            }
        )
    }

    Box {
        UserProfileImage(
            modifier = Modifier.fillMaxWidth(.4f),
            animate = true,
            model = try { firebaseUser.value?.photoURL }catch (e: NotImplementedError) { null },
            tag = currentUser.value?.tag
        )
        MinimalisticBrandIcon(
            modifier = Modifier
                .padding(bottom = 8.dp, end = 8.dp)
                .align(Alignment.BottomEnd),
            onTap = {
                isPictureInEdit.value = true
            },
            imageVector = Icons.Outlined.Brush,
            contentDescription = stringResource(Res.string.accessibility_change_avatar)
        )
    }

    Row(
        modifier = Modifier.align(Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            modifier = Modifier.padding(end = 8.dp),
            text = currentUser.value?.displayName ?: stringResource(Res.string.account_username_empty),
            style = LocalTheme.current.styles.subheading
        )
        MinimalisticComponentIcon(
            imageVector = Icons.Outlined.Edit,
            contentDescription = stringResource(Res.string.accessibility_change_username),
            onTap = {
                isUsernameInEdit.value = true
            }
        )
        MinimalisticComponentIcon(
            modifier = Modifier.padding(start = 4.dp),
            imageVector = Icons.Outlined.IosShare,
            contentDescription = stringResource(Res.string.accessibility_share),
            onTap = {
                val url = HttpDomain + "/users/" + currentUser.value?.publicId
                coroutineScope.launch {
                    if(!shareLink(
                            title = getString(Res.string.network_action_share),
                            imageUrl = try {
                                firebaseUser.value?.photoURL
                            }catch (e: NotImplementedError) { null },
                            link = url
                        )
                    ) {
                        clipboardManager.setText(buildAnnotatedString {
                            withLink(LinkAnnotation.Url(url)) {
                                append(url)
                            }
                        })
                        snackbarHostState?.showSnackbar(
                            message = getString(Res.string.action_link_copied)
                        )
                    }
                }
            }
        )
    }
}

expect fun shareLink(title: String, imageUrl: String?, link: String): Boolean
