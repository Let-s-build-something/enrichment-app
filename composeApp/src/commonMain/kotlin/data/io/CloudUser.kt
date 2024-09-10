package data.io

import data.io.identity_platform.IdentityUserResponse
import dev.gitlive.firebase.auth.FirebaseUser
import dev.gitlive.firebase.auth.MultiFactor
import dev.gitlive.firebase.auth.UserInfo
import dev.gitlive.firebase.auth.UserMetaData

/** User signed in through Google Cloud. Either via Firebase or via HTTP */
data class CloudUser(
    /** unique identifier */
    val uid: String = "",

    /** display name */
    val displayName: String? = null,

    /** email address */
    val email: String? = null,

    /** phone number */
    val phoneNumber: String? = null,

    /** URL to the user's photo */
    val photoURL: String? = null,

    /** whether the user is anonymous */
    val isAnonymous: Boolean = false,

    /** whether the user's email address is verified */
    val isEmailVerified: Boolean = false,

    /** metadata about the user */
    val metaData: UserMetaData? = null,

    /** multi-factor authentication */
    val multiFactor: MultiFactor? = null,

    /** information about the user's identity provider */
    val providerData: List<UserInfo> = emptyList(),

    /** the provider ID */
    val providerId: String = "",

    /** refresh token for the user */
    val refreshToken: String = "",

    /** The number of seconds in which the ID token expires. */
    val expiresIn: Long = 0,

    /** Time of when token expires in epochSeconds [Clock.System.now().epochSeconds] */
    val expiresAt: Long = 0,

    /** currently assigned token by Google Cloud, typical expiry is 1 hour */
    val idToken: String? = null
)

object CloudUserHelper {

    /** creates the [CloudUser] from Firebase user object */
    fun fromFirebaseUser(firebaseUser: FirebaseUser?): CloudUser? {
        return if(firebaseUser == null) null else CloudUser(
            uid = firebaseUser.uid,
            displayName = firebaseUser.displayName,
            email = firebaseUser.email,
            phoneNumber = firebaseUser.phoneNumber,
            photoURL = firebaseUser.photoURL,
            isAnonymous = firebaseUser.isAnonymous,
            isEmailVerified = firebaseUser.isEmailVerified,
            metaData = firebaseUser.metaData,
            multiFactor = firebaseUser.multiFactor,
            providerData = firebaseUser.providerData,
            providerId = firebaseUser.providerId,
            // no need for now
            //idToken = firebaseUser.getIdToken(false)
        )
    }

    /** creates the [CloudUser] from Firebase user object */
    fun fromUserResponse(userResponse: IdentityUserResponse?): CloudUser {
        return CloudUser(
            uid = userResponse?.localId ?: "",
            displayName = userResponse?.displayName,
            email = userResponse?.email,
            photoURL = userResponse?.profilePicture,
            refreshToken = userResponse?.refreshToken ?: "",
            expiresIn = userResponse?.expiresIn ?: 0,
            expiresAt = userResponse?.expiresAt ?: 0,
            idToken = userResponse?.idToken
        )
    }
}