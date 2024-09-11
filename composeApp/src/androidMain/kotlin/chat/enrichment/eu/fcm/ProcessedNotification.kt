package chat.enrichment.eu.fcm

import android.graphics.Bitmap
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.RemoteMessage
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

/**
 * Processed notification containing bitmaps from original URIs
 */
@Serializable
data class ProcessedNotification(
        val title: String? = null,
        val body: String? = null,

        /** whether default sound should be used */
        val defaultSound: Boolean = false,

        /** whether the notification is sticky - is irremovable by user */
        val sticky: Boolean = false,

        /** channelId either from remote source or default one */
        val channelId: String? = null,

        /**
         * Type of the message this is, example of this is enum ACCOUNT_DASHBOARD,
         * which indicates [clickAction] will navigate to account dashboard.
         * This may also mean different visual elements etc.
         */
        val tag: String? = null,

        /** url path to an icon displayed on the left side of the notification */
        val icon: String? = null,

        /** url of a large image displayed either below the message (Android) or above the message (iOS) */
        val imageUrl: String? = null,

        /** text of the action displayed within the notification */
        val clickAction: String? = null,

        /** url to which app should navigate */
        val link: String? = null
) {
        /** large image displayed below the message */
        @SerialName("_image")
        @Transient
        var image: Bitmap? = null

        /** icon displayed on the left side, for example an avatar of a user */
        @SerialName("_largeIcon")
        @Transient
        var largeIcon: Bitmap? = null

        /**
         * Type of the message this is, example of this is enum ACCOUNT_DASHBOARD,
         * which indicates [clickAction] will navigate to account dashboard.
         * This may also mean different visual elements etc.
         */
        @SerialName("_messageType")
        @Transient
        var messageType: NotificationTag? = null

        /** if not null, an action should be shown on the notification */
        @SerialName("_clickAction")
        @Transient
        var action: NotificationCompat.Action? = null

        companion object {
                /** converts regular notification object to [ProcessedNotification] */
                fun fromNotification(notification: RemoteMessage.Notification?): ProcessedNotification? {
                        return if(notification != null) {
                                ProcessedNotification(
                                        title = notification.title,
                                        body = notification.body,
                                        channelId = notification.channelId,
                                        tag = notification.tag,
                                        icon = notification.icon,
                                        imageUrl = notification.imageUrl?.toString(),
                                        clickAction = notification.clickAction,
                                        link = notification.link?.toString(),
                                        defaultSound = notification.defaultSound,
                                        sticky = notification.sticky
                                )
                        }else null
                }
        }
}