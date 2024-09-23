package data.io.user

import chat.enrichment.shared.ui.base.PlatformType
import kotlinx.serialization.Serializable

/** update for change of a FCM token on our BE */
@Serializable
data class RequestFcmTokenUpdate(
    /** currently valid FCM token associated with this device and use */
    val fcmToken: String, //required

    /** previous FCM token, if any, which should be removed from the DB */
    val previousToken: String? = null,

    /** Currently used platform */
    val platform: PlatformType
)