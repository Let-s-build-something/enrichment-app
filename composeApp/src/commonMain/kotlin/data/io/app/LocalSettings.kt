package data.io.app

/** Configuration specific to this application */
data class LocalSettings(

    /** user-selected theme for this application */
    val theme: ThemeChoice = ThemeChoice.SYSTEM,

    /** Fcm token for this device. Used for push notifications. */
    var fcmToken: String? = null,

    /** current client status */
    val clientStatus: ClientStatus = ClientStatus.NEW,

    /** Hexadecimal representations of colors for each category */
    var networkColors: List<String> = listOf(),

    /** instance pointer */
    val uuid: String? = null
) {
    override fun toString(): String {
        return "LocalSettings(theme=$theme, fcmToken=$fcmToken, clientStatus=$clientStatus, networkColors=$networkColors, uuid=$uuid)"
    }
}

enum class ClientStatus {
    NEW,
    REGISTERED
}

enum class ThemeChoice {
    LIGHT,
    DARK,
    SYSTEM
}