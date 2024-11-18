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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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

/**
 * Horizontal layout visualizing a user with ability for actions and checked state
 * @param isSelected whether additional information should be displayed about this user
 * @param isChecked whether this user is checked and it should be indicated
 * @param onAction callback for actions from a selected state
 * @param response currently pending response to any action relevant to this user
 */
@Composable
fun NetworkItemRow(
    modifier: Modifier = Modifier,
    isChecked: Boolean? = null,
    isSelected: Boolean = false,
    data: NetworkItemIO?,
    color: Color? = null,
    onAction: (OptionsLayoutAction) -> Unit = {},
    onCheckChange: (Boolean) -> Unit = {},
    response: BaseResponse<*>? = null
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
                color = color,
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
    color: Color?,
    isChecked: Boolean?,
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
                .padding(top = 8.dp, bottom = 8.dp, end = 4.dp)
                .height(IntrinsicSize.Min),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = if(data.lastMessage.isNullOrBlank()) {
                    Alignment.CenterVertically
                }else Alignment.Top
            ) {
                color?.let { color ->
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .width(8.dp)
                            .background(
                                color = color,
                                shape = RoundedCornerShape(
                                    bottomEnd = LocalTheme.current.shapes.screenCornerRadius,
                                    topEnd = LocalTheme.current.shapes.screenCornerRadius,
                                    bottomStart = 0.dp,
                                    topStart = 0.dp
                                )
                            )
                    )
                }
                UserProfileImage(
                    modifier = Modifier
                        .padding(start = LocalTheme.current.shapes.betweenItemsSpace)
                        .size(48.dp),
                    model = data.photoUrl,
                    tag = data.tag,
                    contentDescription = null
                )
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = LocalTheme.current.shapes.betweenItemsSpace)
                        .padding(vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = data.displayName ?: "",
                        style = LocalTheme.current.styles.category,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = data.lastMessage ?: "",
                        style = LocalTheme.current.styles.regular,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                AnimatedVisibility(isChecked == true) {
                    Icon(
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .size(32.dp),
                        imageVector = Icons.Filled.Check,
                        tint = LocalTheme.current.colors.secondary,
                        contentDescription = null
                    )
                }
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
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                Modifier
                    .padding(start = LocalTheme.current.shapes.betweenItemsSpace)
                    .size(48.dp)
                    .brandShimmerEffect(shape = CircleShape)
            )
            Column(
                modifier = Modifier
                    .padding(start = LocalTheme.current.shapes.betweenItemsSpace)
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    modifier = Modifier
                        .fillMaxWidth((30..40).random().toFloat()/100f)
                        .brandShimmerEffect(),
                    text = "",
                    style = LocalTheme.current.styles.category
                )
                Text(
                    modifier = Modifier
                        .fillMaxWidth((40..70).random().toFloat()/100f)
                        .brandShimmerEffect(),
                    text = "",
                    style = LocalTheme.current.styles.regular
                )
            }
        }
    }
}