package data.io.app

/** Configuration specific to this application */
data class LocalSettings(

    /** user-selected theme for this application */
    val theme: ThemeChoice = ThemeChoice.SYSTEM,

    /** Fcm token for this device. Used for push notifications. */
    var fcmToken: String? = null
)

enum class ThemeChoice {
    LIGHT,
    DARK,
    SYSTEM
}