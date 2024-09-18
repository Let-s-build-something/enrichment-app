package data.io.user

/** update for change of a FCM token on our BE */
data class RequestFcmTokenUpdate(
    /** currently valid FCM token associated with this device and use */
    val fcmToken: String, //required

    /** Currently used platform */
    val platform: PlatformType
)