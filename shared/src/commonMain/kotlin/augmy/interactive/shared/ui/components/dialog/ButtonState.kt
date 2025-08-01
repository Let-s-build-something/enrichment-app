package augmy.interactive.shared.ui.components.dialog

import androidx.compose.ui.graphics.vector.ImageVector

data class ButtonState(
    val text: String,
    val enabled: Boolean = true,
    val imageVector: ImageVector? = null,
    val onClick: () -> Unit = {}
)