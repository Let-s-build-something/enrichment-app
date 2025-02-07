package data.io.base

data class AppPing(
    val type: AppPingType,
    val identifiers: List<String> = listOf()
)

enum class AppPingType {
    ConversationDashboard,
    Conversation,
    NetworkDashboard
}