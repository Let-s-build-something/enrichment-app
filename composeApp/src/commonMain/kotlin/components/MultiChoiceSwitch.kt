package components

import androidx.compose.animation.Animatable
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import future_shared_module.theme.LocalTheme
import org.jetbrains.compose.ui.tooling.preview.Preview

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
fun rememberTabSwitchState(
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

@Composable
fun MultiChoiceSwitch(
    modifier: Modifier = Modifier,
    unselectedTextColor: Color = LocalTheme.current.colors.brandMain,
    selectedTextColor: Color = LocalTheme.current.colors.tetrial,
    state: TabSwitchState = rememberTabSwitchState(scrollState = rememberScrollState()),
    onItemCreation: (@Composable (Modifier, index: Int, animatedColor: Color) -> Unit)? = null,
    shape: Shape = CircleShape
) {
    val localDensity = LocalDensity.current
    val colors = LocalTheme.current.colors

    val indicatorWidth = remember { mutableStateOf((-1).dp) }
    val offsetX = remember {
        Animatable(indicatorWidth.value.times(state.selectedTabIndex.value).value)
    }


    LaunchedEffect(state.selectedTabIndex.value) {
        offsetX.animateTo(
            targetValue = indicatorWidth.value.times(state.selectedTabIndex.value).value,
            animationSpec = tween(if(offsetX.value == -1f) 0 else DEFAULT_ANIMATION_LENGTH_SHORT)
        )
    }
    LaunchedEffect(key1 = state.selectedTabIndex.value) {
        state.onSelectionChange(state.selectedTabIndex.value)
    }


    Box(
        modifier = modifier
            .shadow(
                elevation = LocalTheme.current.styles.componentElevation,
                shape = shape
            )
            .background(
                color = colors.brandMain,
                shape = shape
            )
            .width(indicatorWidth.value)
            .animateContentSize()
    ) {
        Row(
            modifier = Modifier.wrapContentHeight(unbounded = false),
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
                            .padding(LocalTheme.current.shapes.betweenItemsSpace)
                            .clickable(
                                indication = null,
                                interactionSource = remember { MutableInteractionSource() }
                            ) {
                                state.selectedTabIndex.value = index
                            },
                        text = tab,
                        color = color,
                        style = TextStyle(
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        textAlign = TextAlign.Center
                    )
                }).invoke(
                    Modifier
                        .onGloballyPositioned { coordinates ->
                            if (state.selectedTabIndex.value == index) {
                                indicatorWidth.value = with(localDensity) {
                                    coordinates.size.width.toDp()
                                }
                            }
                        }
                        .weight(1f),
                    index,
                    textColor.value
                )
            }
        }
    }
}

@Preview
@Composable
private fun Preview() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(color = LocalTheme.current.colors.backgroundLight)
    ) {
        MultiChoiceSwitch(
            modifier = Modifier,
            state = rememberTabSwitchState(
                tabs = mutableListOf("tab one", "tab two"),
                selectedTabIndex = mutableIntStateOf(1)
            ),
            onItemCreation = { _, _, _ ->

            }
        )
    }
}