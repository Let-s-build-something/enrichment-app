package io.notifications

import kotlinx.serialization.Serializable

/**
 * Processed notification containing bitmaps from original URIs
 */
@Serializable
open class RemoteMessageData(
        var title: String? = null,
        var body: String? = null,

        /** whether default sound should be used */
        var defaultSound: Boolean = false,

        /** whether the notification is sticky - is irremovable by user */
        var sticky: Boolean = false,

        /** channelId either from remote source or default one */
        var channelId: String? = null,

        /**
         * Type of the message this is, example of this is enum ACCOUNT_DASHBOARD,
         * which indicates [clickAction] will navigate to account dashboard.
         * This may also mean different visual elements etc.
         */
        var tag: String? = null,

        /** url path to an icon displayed on the left side of the notification */
        var icon: String? = null,

        /** url of a large image displayed either below the message (Android) or above the message (iOS) */
        var imageUrl: String? = null,

        /** text of the action displayed within the notification */
        var clickAction: String? = null,

        /** url to which app should navigate */
        var link: String? = null
)