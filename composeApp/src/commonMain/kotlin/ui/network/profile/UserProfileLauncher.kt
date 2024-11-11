package ui.network.profile

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
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
import augmy.interactive.shared.ui.base.LocalNavController
import augmy.interactive.shared.ui.base.LocalScreenSize
import augmy.interactive.shared.ui.components.SimpleModalBottomSheet
import augmy.interactive.shared.ui.theme.LocalTheme
import base.navigation.NavigationNode
import components.UserProfileImage
import data.io.base.BaseResponse
import data.io.user.NetworkItemIO
import future_shared_module.ext.brandShimmerEffect
import org.koin.compose.viewmodel.koinViewModel

/** Launcher for quick actions relevant to a single user other than currently signed in */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserProfileLauncher(
    modifier: Modifier = Modifier,
    viewModel: UserProfileViewModel = koinViewModel(),
    publicId: String? = null,
    userProfile: NetworkItemIO? = NetworkItemIO(
        displayName = "Display name",
        tag = "654879",
        photoUrl = "https://picsum.photos/100"
    ),
) {
    val pictureSize = LocalScreenSize.current.width.div(10).coerceIn(
        minimumValue = 75,
        maximumValue = 200
    ).dp

    val response = viewModel.response.collectAsState()


    LaunchedEffect(Unit) {
        publicId?.let {
            viewModel.getUserProfile(publicId)
        }
    }

    SimpleModalBottomSheet(
        modifier = modifier,
        onDismissRequest = {
            //TODO
        }
    ) {
        Crossfade(targetState = response.value is BaseResponse.Loading) { isLoading ->
            Column(modifier = Modifier.fillMaxWidth()) {
                if(isLoading) {
                    ShimmerContent(pictureSize = pictureSize)
                }else {
                    (userProfile ?: response.value.success?.data)?.let { profile ->
                        DataContent(
                            userProfile = profile,
                            pictureSize = pictureSize
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
            .offset(y = -pictureSize/2.5f)
            .align(Alignment.CenterHorizontally)
            .brandShimmerEffect(shape = CircleShape)
    )
}

@Composable
private fun ColumnScope.DataContent(
    userProfile: NetworkItemIO,
    pictureSize: Dp
) {
    UserProfileImage(
        modifier = Modifier
            .size(pictureSize)
            .zIndex(1f)
            .offset(y = -pictureSize/2.5f)
            .align(Alignment.CenterHorizontally),
        model = userProfile.photoUrl,
        tag = userProfile.tag
    )

    Text(
        text = userProfile.displayName ?: "",
        style = LocalTheme.current.styles.subheading
    )
}