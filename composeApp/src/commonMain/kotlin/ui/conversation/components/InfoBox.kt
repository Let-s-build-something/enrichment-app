package ui.conversation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import augmy.composeapp.generated.resources.Res
import augmy.composeapp.generated.resources.accessibility_info_box
import augmy.interactive.shared.ui.theme.LocalTheme
import augmy.interactive.shared.ui.theme.SharedColors
import org.jetbrains.compose.resources.stringResource

@Composable
fun InfoBox(
    modifier: Modifier = Modifier,
    message: String,
    paddingValues: PaddingValues = PaddingValues(
        vertical = 18.dp,
        horizontal = 12.dp
    ),
    boxColor: Color = LocalTheme.current.colors.disabledComponent,
    textColor: Color = LocalTheme.current.colors.secondary,
    additionalContent: (@Composable () -> Unit)? = {
        Icon(
            imageVector = Icons.Outlined.Lightbulb,
            contentDescription = stringResource(Res.string.accessibility_info_box),
            tint = textColor
        )
    }
) {
    Row(
        modifier = modifier
            .padding(vertical = 4.dp)
            .fillMaxWidth(.6f)
            .background(
                color = boxColor,
                shape = LocalTheme.current.shapes.rectangularActionShape
            )
            .padding(paddingValues),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        additionalContent?.invoke()
        SelectionContainer {
            Text(
                modifier = Modifier.padding(start = if (additionalContent != null) 8.dp else 0.dp),
                text = message,
                style = LocalTheme.current.styles.regular.copy(color = textColor)
            )
        }
    }
}

@Composable
fun ErrorInfoBox(modifier: Modifier, message: String) {
    InfoBox(
        modifier = modifier,
        boxColor = SharedColors.RED_ERROR,
        textColor = Color.White,
        message = message,
        paddingValues = PaddingValues(
            vertical = 12.dp,
            horizontal = 8.dp
        )
    ) {
        Icon(
            imageVector = Icons.Outlined.Lightbulb,
            contentDescription = stringResource(Res.string.accessibility_info_box),
            tint = Color.White
        )
    }
}
