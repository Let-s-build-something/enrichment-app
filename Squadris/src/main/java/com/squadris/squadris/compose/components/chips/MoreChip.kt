package com.squadris.squadris.compose.components.chips

import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FilterList
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.squadris.squadris.compose.theme.LocalTheme

@Composable
fun MoreChip(
    modifier: Modifier = Modifier,
    text: String,
    imageVector: ImageVector? = null,
    onClick: () -> Unit = {}
) {
    val localDensity = LocalDensity.current
    FilterChip(
        modifier = modifier
            .padding(horizontal = 4.dp)
            .height(with(localDensity) { 32.sp.toDp() }),
        shape = LocalTheme.current.shapes.chipShape,
        selected = false,
        onClick = onClick,
        label = {
            Text(
                text = text,
                fontSize = 14.sp
            )
        },
        trailingIcon = {
            Icon(
                modifier = Modifier
                    .size(18.dp),
                imageVector = imageVector ?: Icons.Outlined.FilterList,
                tint = LocalTheme.current.colors.tetrial,
                contentDescription = "Sort" //TODO string resources
            )
        },
        colors = FilterChipDefaults.filterChipColors(
            containerColor = LocalTheme.current.colors.brandMain,
            labelColor = LocalTheme.current.colors.tetrial,
            selectedContainerColor = LocalTheme.current.colors.brandMain,
            selectedLabelColor = LocalTheme.current.colors.tetrial
        ),
        border = LocalTheme.current.styles.chipBorderDefault
    )
}