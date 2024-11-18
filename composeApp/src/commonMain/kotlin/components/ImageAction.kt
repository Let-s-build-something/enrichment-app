package components

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ButtonDefaults.elevatedButtonElevation
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import augmy.interactive.shared.ui.theme.LocalTheme
import base.theme.DefaultThemeStyles.Companion.fontQuicksandSemiBold

/** Simple action with image and maximum two line text */
@Composable
fun ImageAction(
    modifier: Modifier = Modifier,
    text: String? = null,
    leadingImageVector: ImageVector? = null,
    containerColor: Color = LocalTheme.current.colors.brandMain,
    contentColor: Color = LocalTheme.current.colors.tetrial,
    onClick: () -> Unit
) {
    Button(
        modifier = modifier,
        onClick = onClick,  
        contentPadding = PaddingValues(
            vertical = 0.dp,
            horizontal = 12.dp
        ),
        shape = LocalTheme.current.shapes.circularActionShape,
        colors = ButtonDefaults.buttonColors(
            containerColor = containerColor,
            contentColor = contentColor,
            disabledContainerColor = LocalTheme.current.colors.disabled,
            disabledContentColor = LocalTheme.current.colors.secondary
        ),
        elevation = elevatedButtonElevation()
    ) {
        leadingImageVector?.let { icon ->
            Icon(
                modifier = Modifier.size(ButtonDefaults.IconSize),
                imageVector = icon,
                contentDescription = null
            )
        }
        if(text != null) {
            Text(
                modifier = Modifier
                    .padding(horizontal = 8.dp),
                text = text,
                style = TextStyle(
                    color = contentColor,
                    fontSize = 14.sp,
                    fontFamily = FontFamily(fontQuicksandSemiBold)
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
