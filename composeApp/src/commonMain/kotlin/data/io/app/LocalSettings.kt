package data.io.app

/** Configuration specific to this application */
data class LocalSettings(

    /** user-selected theme for this application */
    val isDarkTheme: Boolean = false,

    /** Fcm token for this device. Used for push notifications. */
    val fcmToken: String? = null
)