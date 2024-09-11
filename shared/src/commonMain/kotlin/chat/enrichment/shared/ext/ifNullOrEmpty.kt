package chat.enrichment.shared.ext

inline fun <R : Any> R?.ifNull(defaultValue: () -> R): R =
    this ?: defaultValue()
