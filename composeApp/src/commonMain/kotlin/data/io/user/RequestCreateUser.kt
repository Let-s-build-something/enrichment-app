package data.io.user

import augmy.interactive.shared.ui.base.PlatformType
import kotlinx.serialization.Serializable

/** Request body for creation of a user on our BE */
@Serializable
data class RequestCreateUser(
    /** email associated with the registered user */
    val email: String? = null, //optional

    /** client identification provided by Firebase */
    val clientId: String, // required

    /**
     * Firebase Cloud Messaging token, can be revoked and refreshed.
     * It may not be available at the time of creation, but will be in most cases (otherwise it will be sent to our BE later on).
     */
    val fcmToken: String? = null, //optional

    /** Currently used platform */
    val platform: PlatformType
)