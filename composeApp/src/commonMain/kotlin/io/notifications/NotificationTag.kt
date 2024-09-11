package io.notifications

import org.jetbrains.compose.resources.StringResource

/** Tag is an identification of a type of notification being sent */
enum class NotificationTag {
    /** action to open dashboard */
    OPEN_ACCOUNT;

    /** human readable channel name for notification */
    val humanReadableChannel: StringResource?
        get() = when(this) {
            else -> null
        }
}