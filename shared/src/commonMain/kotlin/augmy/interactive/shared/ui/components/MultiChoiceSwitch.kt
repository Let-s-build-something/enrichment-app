package augmy.interactive.shared.ui.components

import androidx.compose.animation.Animatable
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import augmy.interactive.shared.ui.base.LocalDeviceType
import augmy.interactive.shared.ui.theme.LocalTheme
import augmy.interactive.shared.ext.scalingClickable

/** State for communication with [MultiChoiceSwitch] */
data class TabSwitchState(
    /** currently selected tab index */
    val selectedTabIndex: MutableState<Int> = mutableIntStateOf(0),

    /** called whenever tab selection is changed */
    val onSelectionChange: (index: Int) -> Unit = {},

    /** list of all tabs that will be displayed */
    val tabs: MutableList<String> = mutableListOf(),

    /** button scroll state */
    val scrollState: ScrollState
)

/**
 * Remembers the current state
 * @param selectedTabIndex currently selected tab index
 * @param onSelectionChange called whenever tab selection is changed
 * @param tabs list of all tabs that will be displayed
 */
@Composable
fun rememberMultiChoiceState(
    selectedTabIndex: MutableState<Int> = mutableIntStateOf(0),
    onSelectionChange: (index: Int) -> Unit = { index ->
        selectedTabIndex.value = index
    },
    tabs: MutableList<String> = mutableListOf(),
    scrollState: ScrollState = rememberScrollState()
): TabSwitchState {
    val scope = rememberCoroutineScope()
    val state = remember(scope) {
        TabSwitchState(
            selectedTabIndex = selectedTabIndex,
            onSelectionChange = onSelectionChange,
            tabs = tabs,
            scrollState = scrollState
        )
    }
    return state
}

const val DEFAULT_ANIMATION_LENGTH_SHORT = 300
const val DEFAULT_ANIMATION_LENGTH_LONG = 600

/** Single choice between multiple options defined in [TabSwitchState.tabs] */
@Composable
fun MultiChoiceSwitch(
    modifier: Modifier = Modifier,
    indicatorAlignment: Alignment = Alignment.TopCenter,
    unselectedTextColor: Color = LocalTheme.current.colors.brandMainDark,
    selectedTextColor: Color = LocalTheme.current.colors.tetrial,
    selectedBlockColor: Color = LocalTheme.current.colors.brandMain,
    state: TabSwitchState = rememberMultiChoiceState(scrollState = rememberScrollState()),
    onItemCreation: (@Composable RowScope.(Modifier, index: Int, animatedColor: Color) -> Unit)? = null,
    shape: Shape = RoundedCornerShape(
        topStart = if(LocalDeviceType.current == WindowWidthSizeClass.Compact) {
            LocalTheme.current.shapes.screenCornerRadius
        }else 0.dp,
        topEnd = LocalTheme.current.shapes.screenCornerRadius
    )
) {
    val localDensity = LocalDensity.current
    val colors = LocalTheme.current.colors

    val indicatorWidth = remember { mutableStateOf((-1f)) }
    val indicatorHeight = remember { mutableStateOf((-1f)) }
    val offsetX = remember(indicatorWidth.value) {
        Animatable(indicatorWidth.value.times(state.selectedTabIndex.value))
    }

    LaunchedEffect(state.selectedTabIndex.value) {
        offsetX.animateTo(
            targetValue = indicatorWidth.value.times(state.selectedTabIndex.value),
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioLowBouncy,
                stiffness = Spring.StiffnessLow
            )
        )
    }
    LaunchedEffect(key1 = state.selectedTabIndex.value) {
        state.onSelectionChange(state.selectedTabIndex.value)
    }

    Box(
        modifier = modifier
            .background(
                color = selectedTextColor,
                shape = shape
            )
    ) {
        Layout(
            modifier = Modifier.align(indicatorAlignment),
            content = {
                Box(
                    modifier = Modifier
                        .height(
                            with(localDensity) {
                                indicatorHeight.value.toDp()
                            }
                        )
                        .width(
                            with(localDensity) {
                                indicatorWidth.value.toDp() + 2.dp
                            }
                        )
                        .background(unselectedTextColor, shape = shape)
                        .border(
                            width = 0.5.dp,
                            color = selectedBlockColor,
                            shape = shape
                        )
                        .align(Alignment.Center)
                )
            }
        ) { measurables, constraints ->
            val placeables = measurables.map { measurable ->
                measurable.measure(constraints)
            }

            layout(constraints.maxWidth, constraints.minHeight) {
                placeables.forEach { placeable ->
                    placeable.placeRelative(
                        IntOffset(
                            offsetX.value.toInt(),
                            0
                        )
                    )
                }
            }
        }
        Row(
            modifier = Modifier.height(IntrinsicSize.Min),
            horizontalArrangement = Arrangement.SpaceAround,
        ) {
            state.tabs.forEachIndexed { index, tab ->
                val textColor = remember(index) {
                    Animatable(colors.primary)
                }

                LaunchedEffect(state.selectedTabIndex.value) {
                    if(state.selectedTabIndex.value == index) {
                        textColor.animateTo(
                            targetValue = selectedTextColor,
                            animationSpec = tween(DEFAULT_ANIMATION_LENGTH_LONG)
                        )
                    }else textColor.animateTo(
                        unselectedTextColor,
                        animationSpec = tween(DEFAULT_ANIMATION_LENGTH_LONG)
                    )
                }

                (onItemCreation ?: { modifier, _, color ->
                    Text(
                        modifier = modifier
                            .scalingClickable(
                                onTap = {
                                    state.selectedTabIndex.value = index
                                }
                            )
                            .padding(vertical = 2.dp)
                            .padding(LocalTheme.current.shapes.betweenItemsSpace),
                        text = tab,
                        style = TextStyle(
                            fontSize = 16.sp,
                            color = color,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                    )
                }).invoke(
                    this,
                    Modifier
                        .onSizeChanged { coordinates ->
                            if (state.selectedTabIndex.value == index) {
                                indicatorWidth.value = coordinates.width.toFloat()
                                indicatorHeight.value = coordinates.height.toFloat()
                            }
                        }
                        .fillMaxHeight()
                        .weight(1f),
                    index,
                    textColor.value
                )
            }
        }
    }
}

@Composable
fun MultiChoiceSwitchMinimalistic(
    modifier: Modifier = Modifier,
    state: TabSwitchState,
    indicatorColor: Color = LocalTheme.current.colors.brandMain,
    onItemCreation: @Composable ColumnScope.(Modifier, index: Int, animatedColor: Color) -> Unit,
    onClick: (Int) -> Unit
) {
    MultiChoiceSwitch(
        modifier = modifier,
        indicatorAlignment = Alignment.BottomCenter,
        selectedTextColor = Color.Transparent,
        unselectedTextColor = indicatorColor,
        selectedBlockColor = indicatorColor,
        shape = RoundedCornerShape(6.dp),
        state = state,
        onItemCreation = { mod, index, color ->
            Column(
                modifier = Modifier
                    .scalingClickable {
                        onClick(index)
                    }
                    .weight(1f)
                    .align(Alignment.CenterVertically)
            ) {
                onItemCreation(mod, index, color)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false)
                        .height(4.dp)
                        .then(mod)
                )
            }
        }
    )
}
