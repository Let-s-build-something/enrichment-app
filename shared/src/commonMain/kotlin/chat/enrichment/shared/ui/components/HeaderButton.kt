package chat.enrichment.shared.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import future_shared_module.ext.scalingClickable
import chat.enrichment.shared.ui.theme.LocalTheme
import chat.enrichment.shared.ui.theme.SharedColors
import org.jetbrains.compose.ui.tooling.preview.Preview

/**
 * Item displaying collection and shortened information about it
 * @param text text content
 */
@Composable
private fun HeaderButton(
    modifier: Modifier = Modifier,
    text: String = "",
    contentColor: Color,
    containerColor: Color,
    endIconVector: ImageVector? = null,
    additionalContent: @Composable RowScope.() -> Unit = {},
    isEnabled: Boolean = true,
    textStyle: TextStyle? = null,
    shape: Shape = LocalTheme.current.shapes.circularActionShape,
    onClick: () -> Unit = {}
) {
    val animContentColor by animateColorAsState(
        when {
            isEnabled -> contentColor
            else -> LocalTheme.current.colors.secondary
        },
        label = "animContentColor"
    )
    val animContainerColor by animateColorAsState(
        when {
            isEnabled -> containerColor
            else -> LocalTheme.current.colors.disabled
        },
        label = "controlColorChange"
    )

    Row(
        modifier = modifier
            .scalingClickable(
                enabled = isEnabled,
                onTap = {
                    if(isEnabled) onClick()
                }
            )
            .background(
                color = animContainerColor,
                shape = shape
            )
            .padding(
                vertical = 16.dp,
                horizontal = 24.dp
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        if(text.isNotEmpty()) {
            Text(
                modifier = Modifier
                    .padding(end = 6.dp),
                text = text,
                style = textStyle ?: TextStyle(
                    fontSize = 16.sp,
                    color = animContentColor,
                    fontWeight = FontWeight.Bold
                )
            )
        }
        if(endIconVector != null) {
            Icon(
                endIconVector,
                contentDescription = null,
                tint = animContentColor,
                modifier = Modifier
                    .requiredSize(24.dp)
            )
        }
        additionalContent()
    }
}

/**
 * Item displaying collection and shortened information about it
 * @param text text content
 */
@Preview
@Composable
fun ComponentHeaderButton(
    modifier: Modifier = Modifier,
    text: String = "",
    shape: Shape = LocalTheme.current.shapes.circularActionShape,
    textStyle: TextStyle? = null,
    startIconVector: ImageVector? = null,
    extraContent: @Composable RowScope.() -> Unit = {},
    onClick: () -> Unit = {}
) {
    HeaderButton(
        modifier = modifier,
        text = text,
        shape = shape,
        onClick = onClick,
        textStyle = textStyle,
        additionalContent = extraContent,
        endIconVector = startIconVector,
        contentColor = LocalTheme.current.colors.secondary,
        containerColor = LocalTheme.current.colors.backgroundLight
    )
}

/**
 * Item displaying collection and shortened information about it
 * @param text text content
 */
@Preview
@Composable
fun ErrorHeaderButton(
    modifier: Modifier = Modifier,
    text: String = "",
    shape: Shape = LocalTheme.current.shapes.circularActionShape,
    textStyle: TextStyle? = null,
    endIconVector: ImageVector? = null,
    extraContent: @Composable RowScope.() -> Unit = {},
    onClick: () -> Unit = {}
) {
    HeaderButton(
        modifier = modifier,
        text = text,
        shape = shape,
        onClick = onClick,
        textStyle = textStyle,
        additionalContent = extraContent,
        endIconVector = endIconVector,
        contentColor = Color.White,
        containerColor = SharedColors.RED_ERROR
    )
}

/**
 * Item displaying collection and shortened information about it
 * @param text text content
 */
@Preview
@Composable
fun BrandHeaderButton(
    modifier: Modifier = Modifier,
    text: String = "",
    isEnabled: Boolean = true,
    isLoading: Boolean = false,
    onClick: () -> Unit = {}
) {
    HeaderButton(
        modifier = modifier.animateContentSize(),
        text = text,
        isEnabled = isEnabled,
        onClick = onClick,
        additionalContent = {
            if(isLoading) {
                // Loading indicator
                CircularProgressIndicator(
                    modifier = Modifier
                        .padding(start = 12.dp)
                        .requiredSize(24.dp),
                    color = LocalTheme.current.colors.brandMainDark,
                    trackColor = LocalTheme.current.colors.tetrial
                )
            }
        },
        contentColor = LocalTheme.current.colors.tetrial,
        containerColor = LocalTheme.current.colors.brandMain
    )
}