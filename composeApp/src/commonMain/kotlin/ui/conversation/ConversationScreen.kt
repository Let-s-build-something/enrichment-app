package ui.conversation

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import augmy.composeapp.generated.resources.Res
import augmy.composeapp.generated.resources.work_in_progress
import augmy.interactive.shared.ui.theme.LocalTheme
import base.BrandBaseScreen
import base.navigation.NavIconType
import org.jetbrains.compose.resources.stringResource

/** Screen displaying a conversation */
@Composable
fun ConversationScreen(
    userPublicId: String? = null,
    conversationId: String? = null
) {
    BrandBaseScreen(
        navIconType = NavIconType.CLOSE
    ) {
        Text(
            modifier = Modifier.align(Alignment.Center),
            text = stringResource(Res.string.work_in_progress),
            style = LocalTheme.current.styles.subheading
        )
    }
}