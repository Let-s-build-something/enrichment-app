package base.utils

fun extractSnippetAroundHighlight(
    message: String?,
    highlight: String,
    maxChars: Int = 20
): String {
    if (message.isNullOrBlank()) return ""
    val index = message.indexOf(highlight, ignoreCase = true)
    if (index == -1) message
    val start = (index - maxChars).coerceAtLeast(0)
    val end = (index + maxChars + highlight.length).coerceAtMost(message.length)

    val prefix = if (start > 0) "..." else ""
    val suffix = if (end < message.length) "..." else ""

    return prefix + message.substring(start, end).trim() + suffix
}