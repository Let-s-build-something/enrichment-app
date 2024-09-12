package chat.enrichment.eu.fcm

import android.graphics.Bitmap
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.RemoteMessage
import io.notifications.NotificationTag
import io.notifications.RemoteMessageData
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

/**
 * Processed notification containing bitmaps from original URIs
 */
@Serializable
data class ProcessedMessageData(
        /** large image displayed below the message */
        @SerialName("_image")
        @Transient
        var image: Bitmap? = null,

        /** icon displayed on the left side, for example an avatar of a user */
        @SerialName("_largeIcon")
        @Transient
        var largeIcon: Bitmap? = null,

        /**
         * Type of the message this is, example of this is enum ACCOUNT_DASHBOARD,
         * which indicates [clickAction] will navigate to account dashboard.
         * This may also mean different visual elements etc.
         */
        @SerialName("_messageType")
        @Transient
        var messageType: NotificationTag? = null,

        /** if not null, an action should be shown on the notification */
        @SerialName("_clickAction")
        @Transient
        var action: NotificationCompat.Action? = null
): RemoteMessageData() {

        companion object {
                /** converts regular notification object to [ProcessedMessageData] */
                fun fromNotification(notification: RemoteMessage.Notification?): ProcessedMessageData? {
                        return if(notification != null) {
                                ProcessedMessageData().apply {
                                        title = notification.title
                                        body = notification.body
                                        channelId = notification.channelId
                                        tag = notification.tag
                                        icon = notification.icon
                                        imageUrl = notification.imageUrl?.toString()
                                        clickAction = notification.clickAction
                                        link = notification.link?.toString()
                                        defaultSound = notification.defaultSound
                                        sticky = notification.sticky
                                }
                        }else null
                }
        }
}