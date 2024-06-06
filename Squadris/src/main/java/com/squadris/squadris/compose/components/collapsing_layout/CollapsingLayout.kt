package com.squadris.squadris.compose.components.collapsing_layout

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.zIndex
import kotlin.math.roundToInt

/** Collapsing layout with specific behaviors related to each element */
@Composable
fun CollapsingLayout(
    modifier: Modifier = Modifier,
    state: CollapsingLayoutState = CollapsingLayoutState(),
    content: List<Pair<@Composable () -> Unit, CollapsingBehavior>> = listOf()
) {
    if(state.elements.size != content.size) {
        state.elements = content.map {
            CollapsingElement(
                height = remember(content) { mutableDoubleStateOf(0.0) },
                offset = remember(content) { mutableDoubleStateOf(0.0) },
                behavior = it.second
            )
        }
    }
    val scrollableElements = remember {
        derivedStateOf {
            state.elements.filter { it.behavior != CollapsingBehavior.NONE }
        }
    }
    val alwaysScrollableElements = remember {
        derivedStateOf {
            state.elements.filter { it.behavior == CollapsingBehavior.ALWAYS }
        }
    }

    var unConsumed = remember { 0 }

    fun calculateOffset(availableY: Float): Offset {
        if(state.isEnabled.value.not()) return Offset.Zero

        var offsetY = 0.0
        // scrolling up - thus we are looking for the first [CollapsingBehavior.ALWAYS]
        if(availableY > 0) {
            // looking for the first zero, which we will increase, or value that is yet expanding
            (if(state.elements.any { it.behavior == CollapsingBehavior.ALWAYS && it.isCollapsed }) {
                alwaysScrollableElements.value
            }else state.elements.filter {
                // ALWAYS any time, ON_TOP only if we're on top
                it.behavior == CollapsingBehavior.ALWAYS
                        || (unConsumed > 2 && it.behavior == CollapsingBehavior.ON_TOP)
                        || (state.overallScroll == 0f && it.behavior == CollapsingBehavior.ON_TOP)
            }).also { list ->
                (list.find { it.isScrolling } ?: list.lastOrNull { it.isCollapsed })?.run {
                    val oldOffset = offset.doubleValue
                    val newOffset = (oldOffset + availableY).coerceIn(-height.doubleValue, 0.0)
                    offset.doubleValue = newOffset
                    offsetY = newOffset - oldOffset
                }
            }
        // scrolling down - setting offset on everything collapsable
        }else {
            // looking for a zero, or value in scroll which isn't the offset of its height
            (scrollableElements.value.find { it.isScrolling } ?: scrollableElements.value.firstOrNull { it.isExpanded })?.run {
                val oldOffset = offset.doubleValue
                val newOffset = (oldOffset + availableY).coerceIn(-height.doubleValue, 0.0)
                offset.doubleValue = newOffset
                offsetY = newOffset - oldOffset
            }
        }
        return Offset(0f, offsetY.toFloat())
    }


    val nestedScrollConnection = remember(content) {
        object: NestedScrollConnection {
            override fun onPreScroll(
                available: Offset,
                source: NestedScrollSource
            ): Offset {
                if(available.y < 0f || available.y == 0f) unConsumed = 0
                return calculateOffset(available.y)
            }

            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource
            ): Offset {
                if(available.y < 0f) unConsumed = 0
                if(consumed.y == 0f) unConsumed++ else unConsumed = 0
                state.overallScroll = if(consumed.y == 0f && (state.overallScroll >= -30f || unConsumed > 2)) 0.0f
                else (state.overallScroll + consumed.y).coerceAtMost(0f)

                return calculateOffset(available.y)
            }
        }
    }

    Box(
        modifier = modifier.nestedScroll(nestedScrollConnection)
    ) {
        content.forEachIndexed { index, pair ->
            // every previous elements except this one
            val heightAbove = derivedStateOf {
                when(index) {
                    0 -> 0.0
                    1 -> state.elements.getOrNull(0)?.height?.doubleValue ?: 0.0
                    else -> state.elements.subList(0, index).sumOf { it.height.doubleValue }
                }
            }
            val offSetAbove = derivedStateOf {
                when(index) {
                    0 -> 0.0
                    1 -> state.elements.getOrNull(0)?.offset?.doubleValue ?: 0.0
                    else -> state.elements.subList(0, index).sumOf {
                        it.offset.doubleValue.coerceIn(-it.height.doubleValue, 0.0)
                    }
                }
            }
            // current item's offset
            val offSet = state.elements.getOrNull(index)?.offset

            Box(
                modifier = Modifier
                    // measurement of height of this element
                    .onSizeChanged { size ->
                        state.elements.getOrNull(index)?.height?.doubleValue =
                            size.height.toDouble()
                    }
                    .offset {
                        IntOffset(
                            x = 0,
                            y = (heightAbove.value + offSetAbove.value + (offSet?.doubleValue
                                ?: 0.0)).roundToInt()
                        )
                    }
                    // reverse order - top elements should be on the top on Z coordinate
                    .zIndex(
                        state.elements.size
                            .minus(index)
                            .toFloat()
                    )
            ) {
                pair.first()
            }
        }
    }
}

@Composable
fun rememberCollapsingLayout(): CollapsingLayoutState {
    return remember { CollapsingLayoutState() }
}