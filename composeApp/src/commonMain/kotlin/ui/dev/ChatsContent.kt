package ui.dev

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import augmy.interactive.shared.ui.theme.LocalTheme
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun ChatsContent(model: DeveloperConsoleModel = koinViewModel()) {
    val isObserving = model.isObservingChats.collectAsState()
    val observedEntities = model.observedEntities.collectAsState()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        stickyHeader(key = "switch") {
            Row(
                modifier = Modifier.padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Stream new messages",
                    style = LocalTheme.current.styles.category
                )
                Switch(
                    colors = LocalTheme.current.styles.switchColorsDefault,
                    onCheckedChange = { model.observerChats(it) },
                    checked = isObserving.value
                )
            }
        }
        items(
            items = observedEntities.value,
            key = { it }
        ) { entity ->
            Text(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        color = LocalTheme.current.colors.backgroundDark,
                        shape = LocalTheme.current.shapes.rectangularActionShape
                    )
                    .padding(horizontal = 12.dp),
                text = entity,
                style = LocalTheme.current.styles.regular.copy(
                    color = LocalTheme.current.colors.disabled
                )
            )
        }
    }
}
