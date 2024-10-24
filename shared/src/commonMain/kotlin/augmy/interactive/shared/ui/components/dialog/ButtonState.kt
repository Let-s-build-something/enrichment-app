package augmy.interactive.shared.ui.components.dialog

data class ButtonState(
    val text: String,
    val enabled: Boolean = true,
    val onClick: () -> Unit = {}
)