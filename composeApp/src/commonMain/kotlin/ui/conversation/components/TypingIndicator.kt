package ui.conversation.components

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import augmy.interactive.shared.ui.theme.LocalTheme
import components.UserProfileImage
import data.io.social.network.conversation.ConversationTypingIndicator
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val ANIMATION_LENGTH = 400L * 12 + 800L

/**
 * Animated visualization of other user typing in a conversation
 * Each update resets the animation's length, but not progress
 */
@Composable
fun TypingIndicator(
    modifier: Modifier = Modifier,
    key: Int,
    data: ConversationTypingIndicator,
    hasPrevious: Boolean,
    hasNext: Boolean,
    onFinish: () -> Unit = {}
) {
    val density = LocalDensity.current
    val userProfileSize = remember {
        with(density) { 38.sp.toDp() }
    }

    Row (
        modifier = modifier
            .height(48.dp)
            .widthIn(min = 96.dp + userProfileSize)
            .background(
                color = LocalTheme.current.colors.backgroundContrast,
                shape = RoundedCornerShape(
                    topEnd = 24.dp,
                    bottomEnd = 24.dp,
                    topStart = if(hasPrevious) 1.dp else 24.dp,
                    bottomStart = if(hasNext) 1.dp else 24.dp
                )
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        UserProfileImage(
            modifier = Modifier.size(userProfileSize),
            media = data.user?.avatar,
            tag = data.user?.tag
        )
        LoadingMessageBubble(
            key = key,
            data = data,
            onFinish = onFinish
        )
    }
}

@Composable
fun LoadingMessageBubble(
    modifier: Modifier = Modifier,
    key: Int,
    data: ConversationTypingIndicator,
    onFinish: () -> Unit = {}
) {
    val coroutineScope = rememberCoroutineScope()
    val cancellableScope = rememberCoroutineScope()

    val dotCount = rememberSaveable(data.authorPublicId) {
        mutableStateOf(0)
    }
    val isTicked = rememberSaveable(data.authorPublicId) {
        mutableStateOf(false)
    }

    LaunchedEffect(data.authorPublicId, key, data.content) {
        cancellableScope.coroutineContext.cancelChildren()
        coroutineScope.coroutineContext.cancelChildren()
        if(dotCount.value == -1) dotCount.value = 0
        isTicked.value = true

        coroutineScope.launch {
            while(dotCount.value != -1) {
                delay(100L)
                dotCount.value = when(dotCount.value) {
                    0 -> 1
                    1 -> 2
                    2 -> 3
                    else -> 0
                }
                delay(300L)
            }
        }
        cancellableScope.launch {
            delay(ANIMATION_LENGTH - dotCount.value * 400L)
            dotCount.value = -1
            while(dotCount.value == -1) {
                isTicked.value = !isTicked.value
                delay(400L)
            }
            onFinish()
        }
    }

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(3) { index ->
            val isVisible = index <= (dotCount.value - 1) || dotCount.value == -1

            Box(
                modifier = Modifier
                    .animateContentSize()
                    .padding(start = if(index != 0 && isVisible) 6.dp else 0.dp)
                    .size(if(isVisible) 10.dp else 0.dp)
                    .background(
                        color = LocalTheme.current.colors.disabled,
                        shape = RoundedCornerShape(3.dp)
                    )
            )
        }
        if(isTicked.value) {
            Box(
                modifier = Modifier
                    .padding(start = 4.dp)
                    .size(height = 16.dp, width = 4.dp)
                    .background(
                        color = LocalTheme.current.colors.disabled,
                        shape = RoundedCornerShape(2.dp)
                    )
            )
        }
    }
}
