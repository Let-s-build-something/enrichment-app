package components.network

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateContentSize
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
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import augmy.interactive.shared.ui.theme.LocalTheme
import components.LoadingIndicator
import components.OptionsLayout
import components.OptionsLayoutAction
import components.UserProfileImage
import data.io.base.BaseResponse
import data.io.user.NetworkItemIO
import future_shared_module.ext.brandShimmerEffect
import future_shared_module.ext.scalingClickable

@Composable
fun NetworkItemRow(
    modifier: Modifier = Modifier,
    isChecked: Boolean? = null,
    isSelected: Boolean,
    data: NetworkItemIO?,
    onAction: (OptionsLayoutAction) -> Unit,
    onCheckChange: (Boolean) -> Unit,
    response: BaseResponse<*>?
) {
    Crossfade(targetState = data != null) { isData ->
        if(isData && data != null) {
            ContentLayout(
                modifier = modifier
                    .scalingClickable(
                        scaleInto = .9f,
                        onTap = {
                            onCheckChange(isChecked == false)
                        },
                        onLongPress = {
                            onCheckChange(true)
                        }
                    ),
                isChecked = isChecked,
                isSelected = isSelected,
                data = data,
                onAction = onAction,
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
    isChecked: Boolean? = null,
    isSelected: Boolean = false,
    onAction: (OptionsLayoutAction) -> Unit,
    response: BaseResponse<*>?
) {
    Column(
        modifier
            .fillMaxWidth()
            .animateContentSize()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(color = LocalTheme.current.colors.backgroundLight)
                .padding(top = 8.dp, bottom = 8.dp, end = 12.dp)
                .height(IntrinsicSize.Min),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                AnimatedVisibility(isChecked == true) {
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
        OptionsLayout(
            show = isSelected && isChecked == null && response == null,
            onClick = onAction,
            zIndex = -1f
        )
    }
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