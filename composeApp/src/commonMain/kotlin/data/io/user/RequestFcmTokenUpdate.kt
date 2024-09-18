package data.io.user

import chat.enrichment.shared.ui.base.PlatformType

/** update for change of a FCM token on our BE */
data class RequestFcmTokenUpdate(
    /** currently valid FCM token associated with this device and use */
    val fcmToken: String, //required

    /** Currently used platform */
    val platform: PlatformType
)