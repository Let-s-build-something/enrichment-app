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

    val deviceId: String? = null,

    /** Identifier of the owner of OLM crypto keys */
    val pickleKey: String? = null,

    /** instance pointer */
    val uuid: String? = null
) {
    override fun toString(): String {
        return "LocalSettings(theme=$theme, fcmToken=$fcmToken, clientStatus=$clientStatus, networkColors=$networkColors, uuid=$uuid)"
    }

    fun update(other: LocalSettings): LocalSettings {
        return this.copy(
            theme = other.theme,
            fcmToken = other.fcmToken ?: this.fcmToken,
            clientStatus = other.clientStatus,
            networkColors = other.networkColors,
            uuid = other.uuid ?: this.uuid,
            deviceId = other.deviceId ?: this.deviceId,
            pickleKey = other.pickleKey ?: this.pickleKey
        )
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