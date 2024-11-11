package ui.network.profile

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.Text
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import augmy.composeapp.generated.resources.Res
import augmy.composeapp.generated.resources.network_inclusion_action_2
import augmy.interactive.shared.ui.base.LocalScreenSize
import augmy.interactive.shared.ui.components.BrandHeaderButton
import augmy.interactive.shared.ui.components.SimpleModalBottomSheet
import augmy.interactive.shared.ui.theme.LocalTheme
import components.UserProfileImage
import data.io.base.BaseResponse
import data.io.user.NetworkItemIO
import future_shared_module.ext.brandShimmerEffect
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
    userProfile: NetworkItemIO? = NetworkItemIO(
        displayName = "Display name",
        tag = "654879",
        photoUrl = "https://augmy.org/storage/peep/peep-9.svg"
    ),
) {
    val pictureSize = LocalScreenSize.current.width.div(5).coerceIn(
        minimumValue = 100,
        maximumValue = 250
    ).dp

    val response = viewModel.response.collectAsState()


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
        Crossfade(targetState = response.value is BaseResponse.Loading) { isLoading ->
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp)
            ) {
                if(isLoading) {
                    ShimmerContent(pictureSize = pictureSize)
                }else {
                    (userProfile ?: response.value.success?.data)?.let { profile ->
                        DataContent(
                            userProfile = profile,
                            pictureSize = pictureSize,
                            viewModel = viewModel
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ColumnScope.ShimmerContent(pictureSize: Dp) {
    Box(
        modifier = Modifier
            .size(pictureSize)
            .zIndex(1f)
            .align(Alignment.CenterHorizontally)
            .brandShimmerEffect(shape = CircleShape)
    )
}

@Composable
private fun DataContent(
    userProfile: NetworkItemIO,
    pictureSize: Dp,
    viewModel: UserProfileViewModel
) {
    Row {
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
        BrandHeaderButton(
            modifier = Modifier.padding(top = 16.dp),
            text = stringResource(Res.string.network_inclusion_action_2),
            onClick = {
                // TODO circle selection popup
            }
        )
    }
}