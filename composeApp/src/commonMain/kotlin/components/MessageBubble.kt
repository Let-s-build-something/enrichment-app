package components

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import augmy.interactive.shared.ui.theme.LocalTheme
import base.theme.Colors
import base.utils.tagToColor
import data.io.social.network.conversation.ConversationMessageIO
import future_shared_module.ext.brandShimmerEffect

/** Horizontal bubble displaying textual content of a message and its reactions */
@Composable
fun MessageBubble(
    modifier: Modifier = Modifier,
    data: ConversationMessageIO?,
    hasPrevious: Boolean = false,
    hasNext: Boolean = false,
    isCurrentUser: Boolean
) {
    Crossfade(targetState = data == null) { isLoading ->
        if(isLoading) {
            val randomFraction = remember { (3..7).random() / 10f }
            Column(
                modifier = modifier
                    .brandShimmerEffect(shape = LocalTheme.current.shapes.circularActionShape)
                    .padding(
                        vertical = 10.dp,
                        horizontal = 12.dp
                    )
                    .fillMaxWidth(randomFraction)
            ) {
                Text(
                    text = "",
                    style = LocalTheme.current.styles.category
                )
            }
        }else {
            Column(
                modifier = modifier
                    .background(
                        color = tagToColor(data?.user?.tag) ?: if(isCurrentUser) {
                            LocalTheme.current.colors.brandMainDark
                        } else LocalTheme.current.colors.disabledComponent,
                        shape = if(isCurrentUser) {
                            RoundedCornerShape(
                                topStart = 24.dp,
                                bottomStart = 24.dp,
                                topEnd = if(hasPrevious) 1.dp else 24.dp,
                                bottomEnd = if(hasNext) 1.dp else 24.dp
                            )
                        }else {
                            RoundedCornerShape(
                                topEnd = 24.dp,
                                bottomEnd = 24.dp,
                                topStart = if(hasPrevious) 1.dp else 24.dp,
                                bottomStart = if(hasNext) 1.dp else 24.dp
                            )
                        }
                    )
                    .padding(
                        vertical = 10.dp,
                        horizontal = 14.dp
                    )
            ) {
                Text(
                    text = data?.content ?: "",
                    style = LocalTheme.current.styles.category.copy(
                        color = if(isCurrentUser) {
                            Colors.GrayLight
                        }else {
                            LocalTheme.current.colors.secondary
                        }
                    )
                )
            }
        }
    }
}