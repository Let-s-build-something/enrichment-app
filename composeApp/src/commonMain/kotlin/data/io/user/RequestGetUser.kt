package data.io.user

import augmy.interactive.shared.ui.base.PlatformType
import augmy.interactive.shared.ui.base.currentPlatform
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Request body for retrieval of a user */
@Serializable
data class RequestGetUser(
    /** Currently used platform */
    val platform: PlatformType = currentPlatform,

    /** FCM token for push notifications to be refreshed */
    val fcmToken: String?,

    /** Name of the currently running device */
    @SerialName("device_name")
    val deviceName: String?,

    /** Matrix session refresh token */
    @SerialName("refresh_token")
    val refreshToken: String?,

    /** In how long does the token expire in milliseconds */
    @SerialName("expires_in_ms")
    val expiresInMs: Long?
)