package chat.enrichment.shared.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.QuestionMark
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import chat.enrichment.shared.ui.theme.LocalTheme
import chat.enrichment.shared.ui.theme.SharedColors

/** Simple string text with colored text and icon based on the property [isCorrect] */
@Composable
fun CorrectionText(
    modifier: Modifier = Modifier,
    text: String,
    isCorrect: Boolean,
    isRequired: Boolean = true
) {
    val color = animateColorAsState(
        targetValue = when {
            isRequired && isCorrect.not() -> SharedColors.RED_ERROR
            isCorrect -> SharedColors.GREEN_CORRECT
            else -> LocalTheme.current.colors.secondary
        },
        label = "correctionTextColorChange"
    )

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            modifier = Modifier.size(16.dp),
            imageVector = when {
                isRequired && isCorrect.not() -> Icons.Outlined.Close
                isCorrect -> Icons.Outlined.Check
                else -> Icons.Outlined.QuestionMark
            },
            tint = color.value,
            contentDescription = "confirm"
        )
        Text(
            text = text,
            style = LocalTheme.current.styles.regular.copy(color = color.value)
        )
    }
}