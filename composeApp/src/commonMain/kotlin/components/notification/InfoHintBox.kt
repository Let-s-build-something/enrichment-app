package components.notification

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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

/**
 * Information box with short content suggesting user how to control the app or to inform them of something.
 */
@Composable
fun InfoHintBox(
    modifier: Modifier = Modifier,
    title: String? = null,
    text: String,
    icon: @Composable (() -> Unit)? = null,
    onDismissRequest: (() -> Unit)? = null
) {
    Row(
        modifier = modifier.padding(vertical = 6.dp, horizontal = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        icon?.invoke() ?: Icon(
            modifier = Modifier.size(36.dp),
            imageVector = Icons.Outlined.Lightbulb,
            contentDescription = null,
            tint = LocalTheme.current.colors.primary
        )
        Column(modifier = Modifier.weight(1f)) {
            if(title != null) {
                Text(
                    modifier = Modifier.padding(bottom = 2.dp),
                    text = title,
                    style = LocalTheme.current.styles.title
                )
            }
            Text(
                text = text,
                style = LocalTheme.current.styles.regular
            )
        }
        if(onDismissRequest != null) {
            MinimalisticIcon(
                imageVector = Icons.Outlined.Close,
                tint = LocalTheme.current.colors.secondary,
                onTap = {
                    onDismissRequest()
                }
            )
        }
    }
}