package com.squadris.squadris.compose.components.chips

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.squadris.squadris.compose.theme.LocalTheme
import com.squadris.squadris.ext.brandShimmerEffect

const val DEFAULT_ANIMATION_LENGTH_SHORT = 300
const val DEFAULT_ANIMATION_LENGTH_LONG = 600

@Composable
fun SimpleChipGroup(
    modifier: Modifier = Modifier,
    isLoading: Boolean = true,
    scrollState: LazyListState = rememberLazyListState(),
    content: LazyListScope.() -> Unit = {}
) {
    Crossfade(
        targetState = isLoading,
        animationSpec = tween(durationMillis = DEFAULT_ANIMATION_LENGTH_SHORT),
        label = ""
    ) { loading ->
        LazyRow(
            modifier = modifier,
            state = scrollState,
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if(loading) {
                items(count = 8) {
                    ShimmerChip()
                }
            }else content()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ShimmerChip() {
    Box(
        modifier = Modifier
            .padding(horizontal = 4.dp, vertical = 8.dp)
            .height(FilterChipDefaults.Height)
            .width(FilterChipDefaults.Height.times(1.75f))
            .brandShimmerEffect(shape = LocalTheme.current.shapes.chipShape)
    )
}