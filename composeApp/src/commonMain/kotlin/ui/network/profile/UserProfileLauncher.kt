package ui.network.profile

import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SheetState
import androidx.compose.material3.SheetValue
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.rememberStandardBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import augmy.composeapp.generated.resources.Res
import augmy.composeapp.generated.resources.error_general
import augmy.composeapp.generated.resources.network_inclusion_action_2
import augmy.composeapp.generated.resources.network_inclusion_success
import augmy.composeapp.generated.resources.network_inclusion_success_action
import augmy.interactive.shared.ext.brandShimmerEffect
import augmy.interactive.shared.ui.base.CustomSnackbarVisuals
import augmy.interactive.shared.ui.base.LocalNavController
import augmy.interactive.shared.ui.base.LocalScreenSize
import augmy.interactive.shared.ui.base.LocalSnackbarHost
import augmy.interactive.shared.ui.components.BrandHeaderButton
import augmy.interactive.shared.ui.components.ContrastHeaderButton
import augmy.interactive.shared.ui.components.SimpleModalBottomSheet
import augmy.interactive.shared.ui.theme.LocalTheme
import base.navigation.NavigationNode
import components.UserProfileImage
import data.io.base.BaseResponse
import data.io.user.NetworkItemIO
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.context.loadKoinModules

/** Launcher for quick actions relevant to a single user other than currently signed in */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserProfileLauncher(
    modifier: Modifier = Modifier,
    onDismissRequest: () -> Unit,
    state: SheetState =  rememberStandardBottomSheetState(
        confirmValueChange = {
            it != SheetValue.PartiallyExpanded
        },
        initialValue = SheetValue.Expanded,
        skipHiddenState = false
    ),
    user: NetworkItemIO? = null
) {
    loadKoinModules(userProfileModule)
    val viewModel: UserProfileModel = koinViewModel()

    val snackbarHostState = LocalSnackbarHost.current
    val navController = LocalNavController.current
    val pictureSize = LocalScreenSize.current.width.div(5).coerceIn(
        minimumValue = 100,
        maximumValue = 150
    ).dp


    val responseProfile = viewModel.responseProfile.collectAsState()
    val responseInclusion = viewModel.responseInclusion.collectAsState()


    /*
    val showActionDialog = rememberSaveable {
        mutableStateOf<OptionsLayoutAction?>(null)
    }
    showActionDialog.value?.let { action ->
        AlertDialog(
            title = stringResource(
                if(action == OptionsLayoutAction.Mute) {
                    Res.string.network_dialog_title_mute
                }else Res.string.network_dialog_title_block
            ),
            message = stringResource(
                if(action == OptionsLayoutAction.Mute) {
                    Res.string.network_dialog_message_mute
                }else Res.string.network_dialog_message_block
            ),
            icon = action.leadingImageVector,
            confirmButtonState = ButtonState(
                text = stringResource(Res.string.button_confirm)
            ) {
                viewModel.requestProximityChange(
                    selectedConnections = checkedItems,
                    proximity = if(action == OptionsLayoutAction.Mute) {
                        NetworkProximityCategory.Public.range.start
                    }else BlockedProximityValue,
                    onOperationDone = {
                        networkItems.refresh()
                    }
                )
                checkedItems.clear()
            },
            dismissButtonState = ButtonState(
                text = stringResource(Res.string.button_dismiss)
            ),
            onDismissRequest = {
                showActionDialog.value = null
            }
        )
    }*/
    /*data object Mute: OptionsLayoutAction(
        textRes = Res.string.button_mute,
        leadingImageVector = Icons.Outlined.VoiceOverOff,
        containerColor = SharedColors.RED_ERROR
    )
    data object Block: OptionsLayoutAction(
        textRes = Res.string.button_block,
        leadingImageVector = Icons.Outlined.FaceRetouchingOff,
        containerColor = SharedColors.RED_ERROR.copy(alpha = 0.6f)
    )
    Icons.Outlined.TrackChanges*/

    LaunchedEffect(Unit) {
        viewModel.responseInclusion.collectLatest {
            if(it is BaseResponse.Success) {
                CoroutineScope(Dispatchers.Main).launch {
                    if(snackbarHostState?.showSnackbar(
                            message = getString(Res.string.network_inclusion_success),
                            actionLabel = if(it.data.isInclusionImmediate == true && it.data.targetPublicId != null) {
                                getString(Res.string.network_inclusion_success_action)
                            }else null,
                            duration = SnackbarDuration.Short
                        ) == SnackbarResult.ActionPerformed
                    ) {
                        onDismissRequest()
                        navController?.navigate(
                            NavigationNode.Conversation(
                                conversationId = it.data.targetPublicId,
                                name = responseProfile.value.success?.data?.displayName
                            )
                        )
                    }
                }
                onDismissRequest()
            }
        }
    }

    LaunchedEffect(responseInclusion) {
        viewModel.responseProfile.collectLatest {
            if(it is BaseResponse.Error) {
                CoroutineScope(Dispatchers.Main).launch {
                    snackbarHostState?.showSnackbar(
                        visuals = CustomSnackbarVisuals(
                            message = getString(Res.string.error_general),
                            isError = true
                        )
                    )
                }
                onDismissRequest()
            }
        }
    }

    LaunchedEffect(Unit) {
        user?.publicId?.takeIf { it.isNotBlank() }?.let {
            viewModel.getUserProfile(it)
        }
    }

    SimpleModalBottomSheet(
        modifier = modifier,
        onDismissRequest = onDismissRequest,
        sheetState = state,
        dragHandle = null
    ) {
        Crossfade(targetState = responseProfile.value is BaseResponse.Loading) { isLoading ->
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .animateContentSize()
            ) {
                if(isLoading) {
                    ShimmerContent(pictureSize = pictureSize)
                }else {
                    (user ?: responseProfile.value.success?.data)?.let { profile ->
                        DataContent(
                            userProfile = profile,
                            pictureSize = pictureSize,
                            viewModel = viewModel,
                            onDismissRequest = onDismissRequest
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ShimmerContent(pictureSize: Dp) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(pictureSize)
                .zIndex(1f)
                .brandShimmerEffect(CircleShape)
        )
        Text(
            modifier = Modifier
                .fillMaxWidth(.4f)
                .brandShimmerEffect()
                .padding(start = 16.dp),
            text = "",
            style = LocalTheme.current.styles.subheading
        )
    }
}

@Composable
private fun DataContent(
    userProfile: NetworkItemIO,
    pictureSize: Dp,
    viewModel: UserProfileModel,
    onDismissRequest: () -> Unit
) {
    val navController = LocalNavController.current
    val responseInclusion = viewModel.responseInclusion.collectAsState()
    val currentUser = viewModel.currentUser.collectAsState()

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.height(pictureSize * 1.15f),
            contentAlignment = Alignment.Center
        ) {
            UserProfileImage(
                modifier = Modifier
                    .size(pictureSize)
                    .zIndex(1f),
                animate = true,
                media = userProfile.avatar,
                tag = userProfile.tag,
                name = userProfile.displayName
            )
        }
        Text(
            modifier = Modifier.padding(start = 16.dp),
            text = userProfile.displayName ?: "",
            style = LocalTheme.current.styles.subheading
        )
    }
    if(currentUser.value?.publicId != userProfile.publicId) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp, alignment = Alignment.End)
        ) {
            if(userProfile.isPublic == true || userProfile.isMutual == true) {
                ContrastHeaderButton(
                    text = stringResource(Res.string.network_inclusion_success_action),
                    endImageVector = Icons.AutoMirrored.Outlined.Send,
                    onClick = {
                        onDismissRequest()
                        navController?.navigate(
                            NavigationNode.Conversation(
                                conversationId = userProfile.publicId,
                                name = userProfile.displayName
                            )
                        )
                    }
                )
            }
            // TODO check whether the user is already included from local database
            if(userProfile.isMutual != true) {
                BrandHeaderButton(
                    isLoading = responseInclusion.value is BaseResponse.Loading,
                    text = stringResource(Res.string.network_inclusion_action_2),
                    onClick = {
                        viewModel.includeNewUser(displayName = userProfile.displayName ?: "")
                    }
                )
            }
        }
    }
}