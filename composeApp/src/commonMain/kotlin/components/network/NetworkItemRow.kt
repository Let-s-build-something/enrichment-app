package components.network

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import augmy.interactive.shared.ext.brandShimmerEffect
import augmy.interactive.shared.ext.scalingClickable
import augmy.interactive.shared.ui.components.DEFAULT_ANIMATION_LENGTH_SHORT
import augmy.interactive.shared.ui.components.highlightedText
import augmy.interactive.shared.ui.theme.LocalTheme
import components.AvatarImage
import data.io.social.network.conversation.message.MediaIO
import data.io.user.NetworkItemIO

/**
 * Horizontal layout visualizing a user with ability for actions and checked state
 * @param isSelected whether additional information should be displayed about this user
 * @param isChecked whether this user is checked and it should be indicated
 */
@Composable
fun NetworkItemRow(
    modifier: Modifier = Modifier,
    isChecked: Boolean? = null,
    data: NetworkItemIO?,
    avatarSize: Dp = 48.dp,
    highlight: String? = null,
    isSelected: Boolean = false,
    highlightTitle: Boolean = true,
    indicatorColor: Color? = null,
    onAvatarClick: (() -> Unit)? = null,
    actions: @Composable () -> Unit = {},
    content: @Composable RowScope.() -> Unit = {}
) {
    Crossfade(
        modifier = modifier,
        targetState = data != null
    ) { isData ->
        if(isData && data != null) {
            ContentLayout(
                indicatorColor = indicatorColor,
                isChecked = isChecked,
                isSelected = isSelected,
                avatarSize = avatarSize,
                highlight = highlight,
                matchTitle = highlightTitle,
                actions = actions,
                onAvatarClick = onAvatarClick,
                data = data,
                content = content
            )
        }else {
            ShimmerLayout()
        }
    }
}

@Composable
private fun ContentLayout(
    indicatorColor: Color?,
    isChecked: Boolean?,
    isSelected: Boolean = false,
    matchTitle: Boolean = true,
    highlight: String? = null,
    avatarSize: Dp = 48.dp,
    data: NetworkItemIO,
    onAvatarClick: (() -> Unit)? = null,
    content: @Composable RowScope.() -> Unit = {},
    actions: @Composable () -> Unit = {}
) {
    Column(Modifier.animateContentSize()) {
        Row(
            modifier = Modifier
                .animateContentSize()
                .padding(top = 8.dp, bottom = 8.dp, end = 4.dp)
                .height(IntrinsicSize.Min),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            indicatorColor?.let { color ->
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
            AvatarImage(
                modifier = Modifier
                    .scalingClickable(enabled = onAvatarClick != null) {
                        onAvatarClick?.invoke()
                    }
                    .padding(start = LocalTheme.current.shapes.betweenItemsSpace)
                    .size(avatarSize),
                media = data.avatar ?: data.avatarUrl?.let { MediaIO(url = it) },
                tag = data.tag,
                name = data.displayName
            )
            Column(
                modifier = Modifier
                    .width(IntrinsicSize.Max)
                    .padding(start = LocalTheme.current.shapes.betweenItemsSpace)
                    .padding(vertical = 4.dp)
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = if (matchTitle) {
                        highlightedText(
                            highlight = highlight,
                            annotatedString = AnnotatedString(data.displayName ?: "")
                        )
                    } else AnnotatedString(data.displayName ?: ""),
                    style = LocalTheme.current.styles.category.let {
                        if (!matchTitle && highlight != null) {
                            it.copy(color = it.color.copy(alpha = 0.4f))
                        } else it
                    },
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if(data.lastMessage != null) {
                    Text(
                        text = highlightedText(
                            highlight = highlight,
                            annotatedString = AnnotatedString(data.lastMessage)
                        ),
                        style = LocalTheme.current.styles.regular,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
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
            content()
        }
        AnimatedVisibility(
            modifier = Modifier.zIndex(-1f),
            visible = isSelected,
            enter = slideInVertically (
                initialOffsetY = { -it },
                animationSpec = tween(DEFAULT_ANIMATION_LENGTH_SHORT)
            ) + fadeIn(),
            exit = slideOutVertically (
                targetOffsetY = { -it },
                animationSpec = tween(DEFAULT_ANIMATION_LENGTH_SHORT)
            ) + fadeOut()
        ) {
            actions()
        }
    }
}

@Composable
private fun ShimmerLayout(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
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
                val randomFractionTitle = remember { (3..4).random() / 10f }
                val randomFractionMessage = remember { (4..7).random() / 10f }

                Text(
                    modifier = Modifier
                        .fillMaxWidth(randomFractionTitle)
                        .brandShimmerEffect(),
                    text = "",
                    style = LocalTheme.current.styles.category
                )
                Text(
                    modifier = Modifier
                        .fillMaxWidth(randomFractionMessage)
                        .brandShimmerEffect(),
                    text = "",
                    style = LocalTheme.current.styles.regular
                )
            }
        }
    }
}