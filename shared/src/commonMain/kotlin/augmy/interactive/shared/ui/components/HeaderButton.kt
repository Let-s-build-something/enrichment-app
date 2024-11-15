package augmy.interactive.shared.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import augmy.interactive.shared.ui.theme.LocalTheme
import augmy.interactive.shared.ui.theme.SharedColors
import future_shared_module.ext.scalingClickable
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
    showBorder: Boolean = true,
    endImageVector: ImageVector? = null,
    additionalContent: @Composable RowScope.() -> Unit = {},
    isEnabled: Boolean = true,
    textStyle: TextStyle = LocalTheme.current.styles.category,
    shape: Shape = LocalTheme.current.shapes.circularActionShape,
    onClick: () -> Unit = {}
) {
    val density = LocalDensity.current

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
            else -> LocalTheme.current.colors.disabledComponent
        },
        label = "controlColorChange"
    )

    Row(
        modifier = modifier
            .scalingClickable(
                enabled = isEnabled,
                onTap = {
                    if(isEnabled) onClick()
                },
                scaleInto = 0.9f
            )
            .background(
                color = animContainerColor,
                shape = shape
            )
            .then(
                if(showBorder) {
                    Modifier.border(
                        width = .5.dp,
                        color = animContentColor,
                        shape = shape
                    )
                }else Modifier
            )
            .padding(
                vertical = 14.dp,
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
                style = textStyle.copy(color = animContentColor)
            )
        }
        if(endImageVector != null) {
            Icon(
                modifier = Modifier
                    .padding(start = 4.dp)
                    .requiredSize(
                        with(density) { textStyle.fontSize.toDp() }
                    ),
                imageVector = endImageVector,
                contentDescription = null,
                tint = animContentColor
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
    startIconVector: ImageVector? = null,
    extraContent: @Composable RowScope.() -> Unit = {},
    onClick: () -> Unit = {}
) {
    HeaderButton(
        modifier = modifier,
        text = text,
        shape = shape,
        onClick = onClick,
        additionalContent = extraContent,
        endImageVector = startIconVector,
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
    endIconVector: ImageVector? = null,
    extraContent: @Composable RowScope.() -> Unit = {},
    onClick: () -> Unit = {}
) {
    HeaderButton(
        modifier = modifier,
        text = text,
        shape = shape,
        onClick = onClick,
        showBorder = false,
        additionalContent = extraContent,
        endImageVector = endIconVector,
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
        modifier = modifier,
        text = text,
        isEnabled = isEnabled && isLoading.not(),
        onClick = onClick,
        showBorder = isEnabled,
        additionalContent = {
            AnimatedVisibility(isLoading) {
                val density = LocalDensity.current

                CircularProgressIndicator(
                    modifier = Modifier
                        .padding(start = 12.dp)
                        .requiredSize(
                            with(density) { LocalTheme.current.styles.category.fontSize.toDp() }
                        ),
                    color = LocalTheme.current.colors.brandMainDark,
                    trackColor = LocalTheme.current.colors.tetrial
                )
            }
        },
        contentColor = LocalTheme.current.colors.tetrial,
        containerColor = LocalTheme.current.colors.brandMainDark
    )
}

/**
 * Item displaying collection and shortened information about it
 * @param text text content
 */
@Preview
@Composable
fun ContrastHeaderButton(
    modifier: Modifier = Modifier,
    text: String = "",
    isEnabled: Boolean = true,
    endImageVector: ImageVector? = null,
    contentColor: Color = LocalTheme.current.colors.brandMainDark,
    containerColor: Color = LocalTheme.current.colors.tetrial,
    onClick: () -> Unit = {}
) {
    HeaderButton(
        modifier = modifier,
        text = text,
        isEnabled = isEnabled,
        showBorder = isEnabled,
        endImageVector = endImageVector,
        onClick = onClick,
        contentColor = contentColor,
        containerColor = containerColor
    )
}