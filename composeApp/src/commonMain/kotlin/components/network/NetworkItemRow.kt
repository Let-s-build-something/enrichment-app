package components.network

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Block
import androidx.compose.material.icons.outlined.TrackChanges
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import augmy.composeapp.generated.resources.Res
import augmy.composeapp.generated.resources.network_action_block
import augmy.composeapp.generated.resources.network_action_circle_move
import augmy.interactive.shared.ui.theme.LocalTheme
import augmy.interactive.shared.ui.theme.SharedColors
import components.LoadingIndicator
import components.UserProfileImage
import data.io.base.BaseResponse
import data.io.user.NetworkItemIO
import future_shared_module.ext.brandShimmerEffect
import future_shared_module.ext.scalingClickable
import org.jetbrains.compose.resources.stringResource

@Composable
fun NetworkItemRow(
    modifier: Modifier = Modifier,
    isSelected: Boolean? = null,
    data: NetworkItemIO?,
    onAction: (NetworkAction) -> Unit,
    onCheckChange: (Boolean) -> Unit,
    response: BaseResponse<*>?
) {
    Crossfade(targetState = data != null) { isData ->
        if(isData && data != null) {
            ContentLayout(
                modifier = modifier,
                isSelected = isSelected,
                data = data,
                onAction = onAction,
                onCheckChange = onCheckChange,
                response = response
            )
        }else {
            ShimmerLayout(modifier = modifier)
        }
    }
}

@Composable
private fun ContentLayout(
    modifier: Modifier = Modifier,
    data: NetworkItemIO,
    isSelected: Boolean? = null,
    onCheckChange: (Boolean) -> Unit,
    onAction: (NetworkAction) -> Unit,
    response: BaseResponse<*>?
) {
    val showMoreOptions = rememberSaveable(data.publicId) {
        mutableStateOf(false)
    }

    Column(
        modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .scalingClickable(
                scaleInto = .9f,
                onTap = {
                    if(isSelected == null) showMoreOptions.value = !showMoreOptions.value
                    onCheckChange(false)
                },
                onLongPress = {
                    onCheckChange(true)
                    showMoreOptions.value = false
                }
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp, bottom = 8.dp, end = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                AnimatedVisibility(isSelected == true) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .width(8.dp)
                            .background(color = LocalTheme.current.colors.brandMainDark)
                    )
                }
                UserProfileImage(
                    modifier = Modifier
                        .padding(start = 12.dp)
                        .size(48.dp),
                    model = data.photoUrl,
                    tag = data.tag,
                    contentDescription = null
                )
                Text(
                    modifier = Modifier.padding(start = 12.dp),
                    text = data.displayName ?: "",
                    style = LocalTheme.current.styles.category,
                    overflow = TextOverflow.Ellipsis
                )
            }
            AnimatedVisibility(response != null) {
                LoadingIndicator(
                    modifier = Modifier.requiredSize(32.dp),
                    response = response
                )
            }
        }
        AnimatedVisibility(
            visible = showMoreOptions.value
                    && isSelected == null
                    && response == null
        ) {
            Row(
                modifier = Modifier
                    .padding(end = 20.dp)
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp, alignment = Alignment.End)
            ) {
                IconTextAction(
                    imageVector = Icons.Outlined.Block,
                    text = stringResource(Res.string.network_action_block),
                    onTap = {
                        onAction(NetworkAction.BLOCK)
                    },
                    tint = SharedColors.RED_ERROR
                )
                IconTextAction(
                    imageVector = Icons.Outlined.TrackChanges,
                    text = stringResource(Res.string.network_action_circle_move),
                    onTap = {
                        onAction(NetworkAction.MOVE)
                    },
                    tint = LocalTheme.current.colors.brandMain
                )
            }
        }
    }
}

@Composable
fun IconTextAction(
    modifier: Modifier = Modifier,
    text: String,
    imageVector: ImageVector,
    onTap: () -> Unit,
    tint: Color
) {
    Row(
        modifier = modifier
            .scalingClickable {
                onTap()
            }
            .padding(4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = text,
            style = LocalTheme.current.styles.category.copy(color = tint)
        )
        Icon(
            modifier = Modifier.size(32.dp),
            imageVector = imageVector,
            contentDescription = null,
            tint = tint
        )
    }
}

enum class NetworkAction {
    BLOCK,
    MOVE
}

@Composable
private fun ShimmerLayout(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.padding(vertical = 8.dp, horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                Modifier
                    .size(48.dp)
                    .brandShimmerEffect(shape = CircleShape)
            )
            Text(
                modifier = Modifier
                    .fillMaxWidth((30..62).random().toFloat()/100f)
                    .brandShimmerEffect(),
                text = "",
                style = LocalTheme.current.styles.category,
            )
        }
    }
}