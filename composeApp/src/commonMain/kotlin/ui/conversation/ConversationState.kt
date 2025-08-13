package ui.conversation

data class ConversationState(
    val conversationId: String,
    val type: ConversationStateType
)

enum class ConversationStateType {
    Enter,
    Leave
}