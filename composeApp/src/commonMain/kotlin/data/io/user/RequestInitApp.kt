package data.io.user

import augmy.interactive.shared.ui.base.PlatformType
import augmy.interactive.shared.ui.base.currentPlatform
import kotlinx.serialization.Serializable

/** Request body for retrieval of a user */
@Serializable
data class RequestInitApp(
    /** Currently used platform */
    val platform: PlatformType = currentPlatform,

    /** FCM token for push notifications to be refreshed */
    val fcmToken: String?,

    /** Name of the currently running device */
    val deviceName: String?,

    /** Matrix session refresh token */
    val refreshToken: String?,

    /** In how long does the token expire in milliseconds */
    val expiresInMs: Long?
)