package com.squadris.squadris.compose.components.chips

import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Modifier
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** custom chip group - horizontal scrollable layout containing filtering chips */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CustomChipGroup(
    modifier: Modifier = Modifier,
    state: CustomChipGroupState = rememberCustomChipGroupState(),
    chips: SnapshotStateList<ChipState> = mutableStateListOf(),
    scrollState: LazyListState = rememberLazyListState(),
    sortingStrategy: (suspend (
        chips: SnapshotStateList<ChipState>,
        previousChips: List<ChipState>
    ) -> List<ChipState>)? = null,
    selectionInterceptor: suspend (chipState: ChipState) -> Boolean = { _, -> true }
) {
    val clickScope = rememberCoroutineScope()
    val filterScope = rememberCoroutineScope()

    val finalChips = remember { mutableStateOf(listOf<ChipState>()) }

    val filterChips = {
        filterScope.launch(Dispatchers.Default) {
            finalChips.value = sortingStrategy?.invoke(chips, finalChips.value)
                ?: defaultSortingStrategy(chips, finalChips.value)
        }
    }

    val onChipPressed: (chipState: ChipState) -> Unit = { chipState ->
        clickScope.coroutineContext.cancelChildren()
        clickScope.launch {
            if(state.scrollOnChange) {
                scrollState.scrollToItem(0)
            }
            withContext(Dispatchers.Default) {
                if((state.isSingleSelection || chipState.isSingleSelection) && chipState.isChecked.value) {
                    finalChips.value.forEach { chipStateFromList ->
                        if(chipStateFromList.uid != chipState.uid) {
                            chipStateFromList.isChecked.value = false
                        }
                    }
                    filterChips()
                }else {
                    if(chipState.isChecked.value) {
                        finalChips.value.find { it.isChecked.value && it.isSingleSelection }?.let {
                            it.isChecked.value = false
                        }
                    }
                    filterChips()
                    delay(state.selectionDelay)
                    state.onCheckedChipsChanged(
                        finalChips.value.filter { it.isChecked.value }.map { it.uid }
                    )
                }
            }
        }
    }

    LaunchedEffect(chips.size) {
        filterChips()
    }

    chips.forEach { chipState ->
        if(chipState.type != CustomChipType.SEARCH) {
            LaunchedEffect(chipState.isChecked.value) {
                onChipPressed(chipState)
            }
        }
    }

    SimpleChipGroup(
        modifier = modifier,
        isLoading = state.isLoading.value || finalChips.value.isEmpty(),
        scrollState = scrollState
    ) {
        items(
            finalChips.value,
            key = { chipState -> chipState.uid }
        ) { chipState ->
            if(chipState.isVisibleUnchecked || chipState.isChecked.value) {
                val onChipClicked = {
                    filterScope.launch {
                        if(selectionInterceptor(chipState)) {
                            chipState.onChipPressed(chipState.isChecked.value.not())
                            if(chipState.isCheckable) {
                                chipState.isChecked.value = chipState.isChecked.value.not()
                            }
                        }
                    }
                }
                when(chipState.type) {
                    CustomChipType.SEARCH -> SearchChip(
                        onSearchOutput = chipState.onSearchOutput,
                        onClick = { onChipClicked() },
                        isChecked = chipState.isChecked,
                        text = chipState.chipText.value
                    )
                    CustomChipType.REGULAR -> {
                        SimpleChip(
                            modifier = Modifier.animateItemPlacement(
                                animationSpec = tween(
                                    durationMillis = DEFAULT_ANIMATION_LENGTH_SHORT,
                                    easing
                                    = LinearOutSlowInEasing
                                )
                            ),
                            text = chipState.chipText.value,
                            checked = chipState.isChecked.value,
                            onClick = { onChipClicked() },
                            imageVector = chipState.icon.value
                        )
                    }
                    CustomChipType.SORT -> SortChip(
                        imageVector = chipState.icon.value,
                        text = chipState.chipText.value,
                        onClick = { onChipClicked() }
                    )
                    CustomChipType.MORE -> MoreChip(
                        text = chipState.chipText.value,
                        onClick = { onChipClicked() }
                    )
                }
            }
        }
    }
}

val defaultSortingStrategy: (suspend (
    chips: SnapshotStateList<ChipState>,
    previousChips: List<ChipState>
) -> List<ChipState>) = { chips, _ ->
    chips.toList().sortedWith(
        compareBy <ChipState> { it.type.ordinal }
            .thenByDescending { it.isChecked.value }
            .thenBy { chips.indexOf(it) }
    )
}