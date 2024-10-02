package data.io.user

import augmy.interactive.shared.ui.base.PlatformType
import kotlinx.serialization.Serializable

/** Request body for retrieval of a user */
@Serializable
data class RequestGetUser(
    /** Currently used platform */
    val platform: PlatformType,

    /** FCM token for push notifications to be refreshed */
    val fcmToken: String? = null
)