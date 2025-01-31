package data.io.user

import augmy.interactive.shared.ui.base.PlatformType
import augmy.interactive.shared.ui.base.currentPlatform
import kotlinx.serialization.Serializable

/** Request body for retrieval of a user */
@Serializable
data class RequestGetUser(
    /** Currently used platform */
    val platform: PlatformType = currentPlatform,

    /** FCM token for push notifications to be refreshed */
    val fcmToken: String? = null,

    val deviceName: String? = null,
    val refreshToken: String? = null,
    val expiresInMs: Long? = null
)