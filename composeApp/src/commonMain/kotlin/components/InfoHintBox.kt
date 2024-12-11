package components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import augmy.interactive.shared.ui.components.MinimalisticIcon
import augmy.interactive.shared.ui.theme.LocalTheme

/** Information box short content suggesting user how to control the app */
@Composable
fun InfoHintBox(
    modifier: Modifier = Modifier,
    text: String,
    onDismissRequest: () -> Unit
) {
    Row(
        modifier = modifier.padding(vertical = 6.dp, horizontal = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Icon(
            modifier = Modifier
                .align(Alignment.CenterVertically)
                .size(36.dp),
            imageVector = Icons.Outlined.Lightbulb,
            contentDescription = null,
            tint = LocalTheme.current.colors.primary
        )
        Text(
            modifier = Modifier.weight(1f),
            text = text,
            style = LocalTheme.current.styles.title
        )
        MinimalisticIcon(
            imageVector = Icons.Outlined.Close,
            tint = LocalTheme.current.colors.secondary,
            onTap = {
                onDismissRequest()
            }
        )
    }
}