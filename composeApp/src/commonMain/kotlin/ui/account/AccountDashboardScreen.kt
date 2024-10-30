package ui.account

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Handshake
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import augmy.composeapp.generated.resources.Res
import augmy.composeapp.generated.resources.accessibility_change_avatar
import augmy.composeapp.generated.resources.accessibility_change_username
import augmy.composeapp.generated.resources.account_dashboard_fcm
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
import augmy.composeapp.generated.resources.button_dismiss
import augmy.composeapp.generated.resources.button_yes
import augmy.composeapp.generated.resources.screen_account_title
import augmy.composeapp.generated.resources.screen_network_management
import augmy.interactive.shared.ui.base.LocalNavController
import augmy.interactive.shared.ui.base.ModalScreenContent
import augmy.interactive.shared.ui.components.ErrorHeaderButton
import augmy.interactive.shared.ui.components.MinimalisticBrandIcon
import augmy.interactive.shared.ui.components.MultiChoiceSwitch
import augmy.interactive.shared.ui.components.dialog.AlertDialog
import augmy.interactive.shared.ui.components.dialog.ButtonState
import augmy.interactive.shared.ui.components.navigation.ActionBarIcon
import augmy.interactive.shared.ui.components.rememberTabSwitchState
import augmy.interactive.shared.ui.theme.LocalTheme
import base.BrandBaseScreen
import base.navigation.NavigationNode
import components.AsyncSvgImage
import components.RowSetting
import data.io.social.UserPrivacy
import data.io.social.UserVisibility
import data.io.user.tagToColor
import future_shared_module.ext.scalingClickable
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
                    navController?.navigate(NavigationNode.NetworkManagement)
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

            DeveloperSection(viewModel)

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

    val infiniteTransition = rememberInfiniteTransition(label = "infiniteScaleBackground")
    val liveScaleBackground by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = 7000
                1.15f at 2500 using LinearEasing // Takes 2.5 seconds to reach 1.15f
                1f at 7000 using LinearEasing // Takes 4.5 seconds to return to 1f
            },
            repeatMode = RepeatMode.Restart
        ),
        label = "liveScaleBackground"
    )

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
        Box(
            Modifier
                .fillMaxWidth(.4f)
                .aspectRatio(1f)
                .scale(liveScaleBackground)
                .background(
                    color = tagToColor(currentUser.value?.tag) ?: LocalTheme.current.colors.tetrial,
                    shape = CircleShape
                )
        )
        AsyncSvgImage(
            modifier = Modifier
                .fillMaxWidth(.4f)
                .aspectRatio(1f)
                .clip(CircleShape)
                .background(
                    color = LocalTheme.current.colors.brandMain,
                    shape = CircleShape
                ),
            model = try {
                firebaseUser.value?.photoURL
            }catch (e: NotImplementedError) { null },
            contentDescription = null
        )
        MinimalisticBrandIcon(
            modifier = Modifier
                .padding(bottom = 8.dp, end = 8.dp)
                .align(Alignment.BottomEnd),
            onTap = {
                isPictureInEdit.value = true
            },
            imageVector = Icons.Outlined.Edit,
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
        MinimalisticBrandIcon(
            onTap = {
                isUsernameInEdit.value = true
            },
            imageVector = Icons.Outlined.Edit,
            contentDescription = stringResource(Res.string.accessibility_change_username)
        )
    }
}

@Composable
private fun ColumnScope.DeveloperSection(viewModel: AccountDashboardViewModel) {
    val localSettings = viewModel.localSettings.collectAsState()
    val clipboardManager = LocalClipboardManager.current

    AnimatedVisibility(
        modifier = Modifier.padding(top = LocalTheme.current.shapes.betweenItemsSpace),
        visible = localSettings.value?.fcmToken != null
    ) {
        Column {
            Text(
                stringResource(Res.string.account_dashboard_fcm),
                style = LocalTheme.current.styles.category
            )
            Text(
                modifier = Modifier.scalingClickable(
                    onTap = {
                        clipboardManager.setText(
                            AnnotatedString(localSettings.value?.fcmToken ?: "")
                        )
                    }
                ),
                text = localSettings.value?.fcmToken ?: "",
                style = LocalTheme.current.styles.title
            )
        }
    }
}