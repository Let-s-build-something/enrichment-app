package com.squadris.squadris.compose.components.collapsing_layout

import androidx.compose.runtime.MutableDoubleState

/** data for specific element of the [CollapsingLayout] */
data class CollapsingElement(
    val height: MutableDoubleState,
    val offset: MutableDoubleState,
    val behavior: CollapsingBehavior
) {
    /** whether element is in between visible and unvisible */
    val isScrolling: Boolean
        get() = isExpanded.not() && isCollapsed.not()
    
    /** whether element is fully invisible */
    val isCollapsed: Boolean
        get() = offset.doubleValue == -height.doubleValue
    
    /** whether element is fully visible */
    val isExpanded: Boolean
        get() = offset.doubleValue == 0.0
}