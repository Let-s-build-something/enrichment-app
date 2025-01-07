package data.io.user

import augmy.interactive.shared.ui.base.PlatformType
import augmy.interactive.shared.ui.base.currentPlatform
import kotlinx.serialization.Serializable

/** Request for an FCM token */
@Serializable
data class RequestUpdateFcmToken(
    val fcmToken: String,

    /** Current platform */
    val platform: PlatformType = currentPlatform,

    /** Previous fcm token which is to be replaced */
    val oldFcmToken: String? = null
)