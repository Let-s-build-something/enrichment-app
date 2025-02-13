package data.io.base

import augmy.interactive.shared.utils.DateUtils

data class AppPing(
    val type: AppPingType,
    val identifiers: List<String> = listOf(),
    val timestamp: Long = DateUtils.now.toEpochMilliseconds()
)

enum class AppPingType {
    ConversationDashboard,
    Conversation,
    NetworkDashboard
}