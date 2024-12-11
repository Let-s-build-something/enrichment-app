package components.conversation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import augmy.interactive.shared.ui.theme.LocalTheme
import base.theme.Colors
import base.utils.tagToColor
import data.io.social.network.conversation.ConversationMessageIO
import data.io.user.NetworkItemIO
import future_shared_module.ext.brandShimmerEffect
import future_shared_module.ext.scalingClickable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Horizontal bubble displaying textual content of a message and its reactions */
@Composable
fun MessageBubble(
    modifier: Modifier = Modifier,
    data: ConversationMessageIO?,
    users: List<NetworkItemIO>,
    hasPrevious: Boolean = false,
    hasNext: Boolean = false,
    currentUserPublicId: String
) {
    Crossfade(targetState = data == null) { isLoading ->
        if(isLoading) {
            ShimmerLayout(modifier = modifier)
        }else if(data != null) {
            ContentLayout(
                modifier = modifier,
                hasPrevious = hasPrevious,
                hasNext = hasNext,
                currentUserPublicId = currentUserPublicId,
                data = data,
                users = users
            )
        }
    }
}

@Composable
private fun ShimmerLayout(modifier: Modifier = Modifier) {
    val randomFraction = remember { (3..7).random() / 10f }
    Box(
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
}

@Composable
private fun ContentLayout(
    modifier: Modifier = Modifier,
    data: ConversationMessageIO,
    users: List<NetworkItemIO>,
    hasPrevious: Boolean = false,
    hasNext: Boolean = false,
    currentUserPublicId: String
) {
    val density = LocalDensity.current
    val isCurrentUser = data.authorPublicId == currentUserPublicId

    val reactions = remember(data.id) {
        mutableStateOf(listOf<Pair<String?, Pair<List<NetworkItemIO>, Boolean>>>())
    }
    val showDetailDialogOf = remember(data.id) {
        mutableStateOf<String?>(null)
    }

    LaunchedEffect(Unit, data.reactions) {
        withContext(Dispatchers.Default) {
            val map = hashMapOf<String?, Pair<List<NetworkItemIO>, Boolean>>()
            data.reactions?.forEach { reaction ->
                map[reaction.content] = Pair(
                    users.find { it.publicId == reaction.authorPublicId }?.let {
                        map[reaction.content]?.first.orEmpty().plus(it)
                    } ?: map[reaction.content]?.first.orEmpty(),
                    (map[reaction.content]?.second ?: false) || (reaction.authorPublicId == currentUserPublicId)
                )
            }
            reactions.value = map.toList().sortedByDescending { it.second.first.size }
        }
    }

    showDetailDialogOf.value?.let {
        MessageReactionsDialog(
            reactions = reactions,
            users = users,
            reactionsRaw = data.reactions.orEmpty(),
            onDismissRequest = {
                showDetailDialogOf.value = null
            }
        )
    }

    Box(modifier = modifier) {
        Box(
            modifier = Modifier
                .then(
                    if (!data.reactions.isNullOrEmpty()) {
                        Modifier.padding(bottom = with(density) {
                            LocalTheme.current.styles.category.fontSize.toDp() + 6.dp
                        })
                    } else Modifier
                )
                .background(
                    color = tagToColor(data.user?.tag) ?: if(isCurrentUser) {
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
                .animateContentSize()
        ) {
            Text(
                text = data.content ?: "",
                style = LocalTheme.current.styles.category.copy(
                    color = if(isCurrentUser) {
                        Colors.GrayLight
                    }else {
                        LocalTheme.current.colors.secondary
                    }
                )
            )
        }

        AnimatedVisibility(
            modifier = Modifier.align(
                if(isCurrentUser) Alignment.BottomStart else Alignment.BottomEnd
            ),
            visible = !data.reactions.isNullOrEmpty()
        ) {
            Row(
                modifier = Modifier
                    .padding(
                        start = if(isCurrentUser) 0.dp else 12.dp,
                        end = if(isCurrentUser) 12.dp else 0.dp,
                        top = with(density) {
                            LocalTheme.current.styles.category.fontSize.toDp() + 6.dp
                        }
                    )
                    .then(
                        if(reactions.value.size > 1) {
                            Modifier.offset(x = if(isCurrentUser) (-8).dp else 8.dp)
                        }else Modifier
                    ),
                verticalAlignment = Alignment.CenterVertically
            ) {
                reactions.value.take(MaximumReactions).forEach { reaction ->
                    Row(
                        Modifier
                            .scalingClickable {
                                showDetailDialogOf.value = reaction.first
                            }
                            .width(IntrinsicSize.Min)
                            .background(
                                color = LocalTheme.current.colors.disabledComponent,
                                shape = LocalTheme.current.shapes.componentShape
                            )
                            .padding(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                modifier = Modifier.padding(end = 2.dp),
                                text = reaction.first ?: "",
                                style = LocalTheme.current.styles.category.copy(
                                    textAlign = TextAlign.Center
                                )
                            )
                            if(reaction.second.second) {
                                Box(
                                    modifier = Modifier
                                        .height(2.dp)
                                        .fillMaxWidth(.6f)
                                        .background(
                                            color = LocalTheme.current.colors.brandMain,
                                            shape = RoundedCornerShape(8.dp)
                                        )
                                )
                            }
                        }
                        reaction.second.first.size.takeIf { it > 1 }?.let { count ->
                            Text(
                                text = count.toString(),
                                style = LocalTheme.current.styles.regular
                            )
                        }
                    }
                }
            }
        }
    }
}

private const val MaximumReactions = 4
