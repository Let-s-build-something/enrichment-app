package base.global.verification

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.unit.dp
import augmy.interactive.shared.ui.theme.LocalTheme

@Composable
fun EmojiEntity(
    modifier: Modifier,
    emoji: Pair<String, Map<String, String>>
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = emoji.first,
            style = LocalTheme.current.styles.heading
        )
        Text(
            text = emoji.second[Locale.current.language.lowercase()] ?: emoji.second.values.first(),
            style = LocalTheme.current.styles.title
        )
    }
}