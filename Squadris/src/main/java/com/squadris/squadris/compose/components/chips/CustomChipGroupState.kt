package com.squadris.squadris.compose.components.chips

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope

/** default delay for filtering data */
const val FILTER_DELAY_DEFAULT = 400L

/** state for controlling and listening to [CustomChipGroup] */
data class CustomChipGroupState(

    /** delay of data requesting */
    val selectionDelay: Long = FILTER_DELAY_DEFAULT,

    /** Whether is chipgroup loading */
    val isLoading: MutableState<Boolean> = mutableStateOf(false),

    /** whether chips should be scrolled to the beginning after a change */
    val scrollOnChange: Boolean = true,

    /** whenever any chips has been checked */
    val onCheckedChipsChanged: suspend (chipsUid: List<String>) -> Unit,

    /** whether only 1 chips should be selected at all times */
    val isSingleSelection: Boolean = false
)

/** remembering of a state for [CustomChipGroup] */
@Composable
fun rememberCustomChipGroupState(
    selectionDelay: Long = FILTER_DELAY_DEFAULT,
    isLoading: MutableState<Boolean> = mutableStateOf(false),
    scrollOnChange: Boolean = true,
    onCheckedChipsChanged: suspend (chipsUid: List<String>) -> Unit = { _ -> },
    isSingleSelection: Boolean = false
): CustomChipGroupState {
    val scope = rememberCoroutineScope()
    return remember(scope) {
        CustomChipGroupState(
            selectionDelay = selectionDelay,
            isLoading = isLoading,
            scrollOnChange = scrollOnChange,
            onCheckedChipsChanged = onCheckedChipsChanged,
            isSingleSelection = isSingleSelection
        )
    }
}