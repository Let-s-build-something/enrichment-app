package com.squadris.squadris.compose.components.chips

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.vector.ImageVector
import java.util.UUID

/** State for remembering a chip state for [CustomChipGroup] */
data class ChipState(

    /** text displayed in the chip */
    val chipText: MutableState<String> = mutableStateOf(""),

    /** whether this chip is exclusive */
    val isSingleSelection: Boolean = false,

    /** identifier */
    val uid: String = UUID.randomUUID().toString(),

    /** whether this chip is checked or not */
    val isChecked: MutableState<Boolean> = mutableStateOf(false),

    /** type of chip */
    val type: CustomChipType = CustomChipType.REGULAR,

    /** if [type] is [CustomChipType.SEARCH], we receive result from searching here */
    val onSearchOutput: (String) -> Unit = {},

    /** whether this chip can be checked */
    val isCheckable: Boolean = type == CustomChipType.REGULAR,

    /** whether the chip should be visible when not checked */
    val isVisibleUnchecked: Boolean = true,

    /** icon of this chip */
    val icon: MutableState<ImageVector?> = mutableStateOf(null),

    /** called whenever chip is pressed */
    val onChipPressed: (isChecked: Boolean) -> Unit = {}
) {

    /** whether this chip isn't regular */
    val isSpecial: Boolean
        get() = type != CustomChipType.REGULAR
}

enum class CustomChipType {
    SEARCH,
    REGULAR,
    SORT,
    MORE
}