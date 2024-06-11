package lets.build.chatenrichment.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ButtonElevation
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.squadris.squadris.compose.theme.LocalTheme
import com.squadris.squadris.compose.theme.SharedColors
import com.squadris.squadris.ext.scalingClickable
import lets.build.chatenrichment.R

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
    extraContent: @Composable RowScope.() -> Unit = {},
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
                scaleInto = 0.85f,
                onTap = {
                    if(isEnabled) onClick()
                }
            )
            .background(
                color = animContainerColor,
                shape = shape
            )
            .padding(
                vertical = 8.dp,
                horizontal = 16.dp
            ),
        verticalAlignment = Alignment.CenterVertically
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
        extraContent()
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
        extraContent = extraContent,
        endIconVector = startIconVector,
        contentColor = LocalTheme.current.colors.secondary,
        containerColor = LocalTheme.current.colors.onBackgroundComponent
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
        extraContent = extraContent,
        endIconVector = endIconVector,
        contentColor = LocalTheme.current.colors.contrastAction,
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
    onClick: () -> Unit = {}
) {
    HeaderButton(
        modifier = modifier,
        text = text,
        isEnabled = isEnabled,
        onClick = onClick,
        contentColor = LocalTheme.current.colors.tetrial,
        containerColor = LocalTheme.current.colors.brandMain
    )
}