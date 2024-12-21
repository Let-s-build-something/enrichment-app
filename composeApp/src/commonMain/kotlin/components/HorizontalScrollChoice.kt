package components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandIn
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Text
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import augmy.interactive.shared.ui.base.LocalDeviceType
import augmy.interactive.shared.ui.theme.LocalTheme
import base.isDarkTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs

/** Multi-choice component with horizontal drag, which selected horizontally on a contagion-basis */
@Composable
fun <T> HorizontalScrollChoice(
    modifier: Modifier = Modifier,
    selectedItems: List<T>,
    choices: List<ScrollChoice<T>>,
    onSelectionChange: (item: T, isSelected: Boolean) -> Unit,
    trackColor: Color = LocalTheme.current.colors.backgroundLight,
    selectedColor: Color = if(isDarkTheme) LocalTheme.current.colors.primary else trackColor,
    color: Color = if(isDarkTheme) LocalTheme.current.colors.brandMainDark else LocalTheme.current.colors.brandMain,
    deselectedColor: Color = LocalTheme.current.colors.secondary,
    borderColor: Color = if(isDarkTheme) deselectedColor else LocalTheme.current.colors.backgroundLight
) {
    val coroutineScope = rememberCoroutineScope()
    val isCompact = LocalDeviceType.current == WindowWidthSizeClass.Compact
    val defaultShape = RoundedCornerShape(
        topEnd = LocalTheme.current.shapes.screenCornerRadius,
        topStart = if(isCompact) LocalTheme.current.shapes.screenCornerRadius else 0.dp,
        bottomEnd = LocalTheme.current.shapes.componentCornerRadius,
        bottomStart = if(isCompact) LocalTheme.current.shapes.componentCornerRadius else 0.dp
    )

    val componentWidth = remember { mutableStateOf(0f) }
    val progress = remember { mutableStateOf(0f) }
    val isLocked = remember { mutableStateOf(false) }


    LaunchedEffect(progress.value) {
        if(!isLocked.value) {
            coroutineScope.launch {
                if(abs(progress.value) > componentWidth.value / choices.size / 4) {
                    isLocked.value = true

                    val isReversed = progress.value < 0
                    val isUnselect = (if(isReversed) {
                        !selectedItems.contains(choices.last().id)
                    }else !selectedItems.contains(choices.first().id)) || selectedItems.size == choices.size

                    (if(isReversed) choices.reversed() else choices).forEach { choice ->
                        if(isUnselect == selectedItems.contains(choice.id)) {
                            progress.value = 0f
                            onSelectionChange(choice.id, !isUnselect)
                            delay(250)
                            isLocked.value = false
                            return@launch
                        }
                    }
                }
            }
        }
    }

    Box(
        modifier = modifier
            .height(IntrinsicSize.Min)
            .fillMaxWidth()
            .clip(defaultShape)
            .background(
                color = borderColor,
                shape = RoundedCornerShape(
                    topEnd = LocalTheme.current.shapes.screenCornerRadius,
                    topStart = if(isCompact) LocalTheme.current.shapes.screenCornerRadius else 0.dp,
                    bottomEnd = 50.dp,
                    bottomStart = 50.dp
                )
            )
            .padding(top = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxHeight()
                .height(IntrinsicSize.Min)
                .fillMaxWidth()
                .background(color = trackColor, shape = defaultShape)
                .draggable(
                    orientation = Orientation.Horizontal,
                    state = rememberDraggableState { delta ->
                        progress.value = delta
                    }
                )
                .onSizeChanged {
                    componentWidth.value = it.width.toFloat()
                },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            choices.forEachIndexed { index, item ->
                val isSelected = selectedItems.contains(item.id)
                val backgroundColor = animateColorAsState(
                    targetValue = if(isSelected) color else trackColor,
                    label = "backgroundColor$index"
                )
                val textColor = animateColorAsState(
                    targetValue = if(isSelected) selectedColor else deselectedColor,
                    label = "textColor$index"
                )

                val isSelectedBefore = selectedItems.contains(choices.getOrNull(index -1)?.id)
                val isSelectedAfter = selectedItems.contains(choices.getOrNull(index + 1)?.id)

                val shape = if(isSelected) {
                    val cornersRadiusStart = animateFloatAsState(
                        if (isSelectedBefore) 0f else LocalTheme.current.shapes.screenCornerRadius.value,
                        label = "animCornersRadiusStart$index",
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessMedium
                        )
                    )
                    val cornersRadiusEnd = animateFloatAsState(
                        if (isSelectedAfter) 0f else LocalTheme.current.shapes.screenCornerRadius.value,
                        label = "animCornersRadiusEnd$index",
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessMedium
                        )
                    )
                    RoundedCornerShape(
                        topEnd = cornersRadiusEnd.value.coerceAtLeast(0f).dp,
                        topStart = if(isCompact || index != 0) {
                            cornersRadiusStart.value.coerceAtLeast(0f).dp
                        }else 0.dp,
                        bottomEnd = cornersRadiusEnd.value.coerceIn(0f, LocalTheme.current.shapes.componentCornerRadius.value).dp,
                        bottomStart = if(isCompact || index != 0) {
                            cornersRadiusStart.value.coerceIn(0f, LocalTheme.current.shapes.componentCornerRadius.value).dp
                        }else 0.dp
                    )
                }else null

                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .weight(1f)
                        .clickable(
                            enabled = true,
                            interactionSource = null,
                            indication = null
                        ) {
                            onSelectionChange(item.id, !isSelected)
                        },
                    contentAlignment = Alignment.Center
                ) {
                    androidx.compose.animation.AnimatedVisibility(
                        visible = isSelected,
                        enter = when {
                            isSelectedBefore && isSelectedAfter -> expandIn()
                            isSelectedAfter || index == choices.lastIndex || (isSelectedAfter && index == 0) -> {
                                slideInHorizontally { it }
                            }
                            isSelectedBefore || index == 0 -> slideInHorizontally { -it }
                            else -> expandIn()
                        } + fadeIn(),
                        exit = when {
                            isSelectedBefore && isSelectedAfter -> shrinkOut()
                            isSelectedAfter || index == choices.lastIndex || (isSelectedAfter && index == 0) -> {
                                slideOutHorizontally { it }
                            }
                            isSelectedBefore || index == 0 -> slideOutHorizontally { -it }
                            else -> shrinkOut()
                        } + fadeOut()
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    color = backgroundColor.value,
                                    shape = shape ?: defaultShape
                                )
                        )
                    }
                    Text(
                        modifier = Modifier.padding(
                            horizontal = 0.dp,
                            vertical = 12.dp
                        ),
                        text = item.text,
                        style = LocalTheme.current.styles.category.copy(
                            color = textColor.value,
                            textAlign = TextAlign.Center
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

/** Individual item for the [HorizontalScrollChoice] */
data class ScrollChoice<T>(
    /** Identifier unique within the choices */
    val id: T,

    /** Textual content of the choice */
    val text: String,
)
