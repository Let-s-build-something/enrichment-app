package ui.conversation.media

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import augmy.composeapp.generated.resources.Res
import augmy.composeapp.generated.resources.download_failure
import augmy.composeapp.generated.resources.download_loading
import augmy.composeapp.generated.resources.download_success
import augmy.interactive.shared.ui.components.DEFAULT_ANIMATION_LENGTH_SHORT
import augmy.interactive.shared.ui.theme.LocalTheme
import augmy.interactive.shared.ui.theme.SharedColors
import org.jetbrains.compose.resources.stringResource

data class DownloadIndicationState(
    val state: MutableState<DownloadState>,
    val progress: MutableState<LongRange>,
    val items: MutableState<Int>,
    val item: MutableState<Int>
)

enum class DownloadState {
    Initialized,
    Progressing,
    Finished,
    Failed
}

@Composable
fun rememberIndicationState(
    state: DownloadState = DownloadState.Initialized,
    progress: LongRange = 0L..0L,
    items: Int = 1,
    item: Int = 1
): DownloadIndicationState {
    return remember {
        DownloadIndicationState(
            state = mutableStateOf(state),
            progress = mutableStateOf(progress),
            items = mutableStateOf(items),
            item = mutableStateOf(item)
        )
    }
}

@Composable
fun DownloadIndication(
    modifier: Modifier = Modifier,
    state: DownloadIndicationState
) {
    AnimatedVisibility(
        modifier = modifier.zIndex(50f),
        enter = slideInVertically(
            initialOffsetY = { it },
            animationSpec = tween(DEFAULT_ANIMATION_LENGTH_SHORT)
        ),
        exit = slideOutVertically(
            targetOffsetY = { it },
            animationSpec = tween(DEFAULT_ANIMATION_LENGTH_SHORT)
        ),
        visible = state.state.value.ordinal > DownloadState.Initialized.ordinal
    ) {
        val color = animateColorAsState(
            targetValue = if(state.state.value == DownloadState.Failed) {
                SharedColors.RED_ERROR
            }else LocalTheme.current.colors.brandMainDark
        )

        Row(
            modifier = Modifier
                .fillMaxWidth(.8f)
                .background(
                    color = color.value,
                    shape = LocalTheme.current.shapes.componentShape
                )
                .padding(vertical = 10.dp, horizontal = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // TODO icon anim
            when(state.state.value) {
                DownloadState.Initialized -> {}
                DownloadState.Progressing -> {
                    CircularProgressIndicator(
                        modifier = Modifier.requiredSize(32.dp),
                        color = LocalTheme.current.colors.brandMainDark,
                        trackColor = LocalTheme.current.colors.tetrial
                    )
                }
                DownloadState.Finished -> {}
                DownloadState.Failed -> {}
            }
            Text(
                text = when(state.state.value) {
                    DownloadState.Initialized -> ""
                    DownloadState.Progressing -> stringResource(Res.string.download_loading) +
                            (if(state.items.value > 1) state.item.value / state.items.value else "") +
                            "${bytesAsRelative(state.progress.value.first)}/${bytesAsRelative(state.progress.value.last)}"
                    DownloadState.Finished -> stringResource(Res.string.download_success)
                    DownloadState.Failed -> stringResource(Res.string.download_failure)
                },
                style = LocalTheme.current.styles.category.copy(
                    color = LocalTheme.current.colors.tetrial
                )
            )
        }
    }
}

/** Formats number of bytes to readable form */
fun bytesAsRelative(bytes: Long): String {
    return when {
        bytes < 1024 * 1024 * 1024 -> "${kotlin.math.round(bytes / (1024f * 1024f))} MB"
        bytes < 1024 * 1024 -> "${kotlin.math.round(bytes / 1024f)} KB"
        else -> "$bytes B"
    }
}
