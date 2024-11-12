package ui.network.profile

import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarResult
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
import data.io.user.PublicUserProfileIO
import future_shared_module.ext.brandShimmerEffect
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

/** Launcher for quick actions relevant to a single user other than currently signed in */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserProfileLauncher(
    modifier: Modifier = Modifier,
    viewModel: UserProfileViewModel = koinViewModel(),
    onDismissRequest: () -> Unit,
    publicId: String? = null,
    userProfile: PublicUserProfileIO? = null
) {
    val snackbarHostState = LocalSnackbarHost.current
    val navController = LocalNavController.current
    val pictureSize = LocalScreenSize.current.width.div(5).coerceIn(
        minimumValue = 100,
        maximumValue = 150
    ).dp

    val responseProfile = viewModel.responseProfile.collectAsState()
    val responseInclusion = viewModel.responseInclusion.collectAsState()

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
                        navController?.navigate(NavigationNode.Conversation(userUid = it.data.targetPublicId))
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
        publicId?.let {
            viewModel.getUserProfile(publicId)
        }
    }

    SimpleModalBottomSheet(
        modifier = modifier,
        onDismissRequest = onDismissRequest,
        dragHandle = null
    ) {
        Crossfade(targetState = responseProfile.value is BaseResponse.Loading) { isLoading ->
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp)
                    .animateContentSize()
            ) {
                if(isLoading) {
                    ShimmerContent(pictureSize = pictureSize)
                }else {
                    (userProfile ?: responseProfile.value.success?.data)?.let { profile ->
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
    userProfile: PublicUserProfileIO,
    pictureSize: Dp,
    viewModel: UserProfileViewModel,
    onDismissRequest: () -> Unit
) {
    val navController = LocalNavController.current
    val responseInclusion = viewModel.responseInclusion.collectAsState()
    val currentUser = viewModel.currentUser.collectAsState()

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        UserProfileImage(
            modifier = Modifier
                .size(pictureSize)
                .zIndex(1f),
            model = userProfile.photoUrl,
            tag = userProfile.tag
        )
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
            if(userProfile.isPublic == true) {
                ContrastHeaderButton(
                    text = stringResource(Res.string.network_inclusion_success_action),
                    endImageVector = Icons.AutoMirrored.Outlined.Send,
                    onClick = {
                        onDismissRequest()
                        navController?.navigate(
                            NavigationNode.Conversation(userUid = userProfile.publicId)
                        )
                    }
                )
            }
            BrandHeaderButton(
                isLoading = responseInclusion.value is BaseResponse.Loading,
                text = stringResource(Res.string.network_inclusion_action_2),
                onClick = {
                    viewModel.includeNewUser(
                        displayName = userProfile.displayName ?: "",
                        tag = userProfile.tag ?: ""
                    )
                }
            )
        }
    }
}