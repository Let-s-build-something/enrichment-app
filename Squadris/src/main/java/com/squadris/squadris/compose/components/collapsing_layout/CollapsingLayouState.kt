package com.squadris.squadris.compose.components.collapsing_layout

import androidx.compose.runtime.mutableStateOf


/** All temporary data related to [CollapsingLayout] including all the scrolling elements */
class CollapsingLayoutState {

    val isEnabled = mutableStateOf(true)

    internal var overallScroll = 0f
    internal var elements: List<CollapsingElement> = listOf()
    private var restorationMap: Pair<Map<Int, Double>, Float>? = null

    /** expands items to a certain index */
    fun expandToItem(index: Int) {
        elements.subList(index, elements.size).forEach {
            it.offset.doubleValue = 0.0
        }
        overallScroll = 0f
    }

    /** collapses item but saves their state for future restoration */
    fun collapseItemsRestorable(vararg indexes: Int) {
        val map = hashMapOf<Int, Double>()
        indexes.forEach { index ->
            elements.getOrNull(index)?.run {
                map[index] = offset.doubleValue.div(1)
                offset.doubleValue = -height.doubleValue
            }
        }
        restorationMap = map to overallScroll
    }

    /** restores previous state before using [collapseItemsRestorable] */
    fun restoreState() {
        restorationMap?.let { restoration ->
            restoration.first.forEach { mapEntry ->
                elements.getOrNull(mapEntry.key)?.run {
                    offset.doubleValue = mapEntry.value
                }
            }
            overallScroll = restoration.second
        }
    }

    /** returns the overall height above the given [index] */
    fun getCollapsingHeightAbove(index: Int): Double {
        return if(elements.lastIndex < index) 0.0
        else elements.take(index.plus(1)).sumOf { it.height.doubleValue }
    }
}