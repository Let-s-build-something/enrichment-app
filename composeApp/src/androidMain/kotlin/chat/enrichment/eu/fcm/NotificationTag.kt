package chat.enrichment.eu.fcm

/** Tag is an identification of a type of notification being sent */
enum class NotificationTag {
    /** action to open dashboard */
    OPEN_ACCOUNT;

    /** human readable channel name for notification */
    val humanReadableChannel: Int?
        get() = when(this) {
            else -> null
        }
}