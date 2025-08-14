package data.io.user

import androidx.room.Ignore
import base.utils.toSha256
import data.io.social.UserConfiguration
import kotlinx.serialization.Serializable
import net.folivo.trixnity.core.model.UserId

/** user object specific to our database saved in custom DB */
@Serializable
data class UserIO(
    /** username of the current user */
    val displayName: String? = null,

    /** current session token of the Matrix protocol */
    val accessToken: String? = null,

    /** Matrix homeserver associated with this user */
    val matrixHomeserver: String? = null,

    /** Matrix user identifier */
    val userId: String? = null,

    /** Matrix remote path to an avatar */
    val avatarUrl: String? = null,

    /** current configuration specific to this user */
    val configuration: UserConfiguration? = null
) {
    val tag: String?
        @Ignore
        get() = userId?.let { UserId(it).generateUserTag() }

    val isFullyValid: Boolean
        @Ignore
        get() = accessToken != null
                && matrixHomeserver != null
                && userId != null

    fun update(other: UserIO?): UserIO {
        return this.copy(
            displayName = other?.displayName ?: this.displayName,
            accessToken = other?.accessToken ?: this.accessToken,
            userId = other?.userId ?: this.userId,
            matrixHomeserver = other?.matrixHomeserver ?: this.matrixHomeserver,
            configuration = other?.configuration ?: this.configuration,
            avatarUrl = other?.avatarUrl ?: this.avatarUrl
        )
    }

    override fun toString(): String {
        return "{" +
                "displayName: $displayName, " +
                "accessToken: $accessToken, " +
                "matrixHomeserver: $matrixHomeserver, " +
                "matrixUserId: $userId, " +
                "configuration: $configuration" +
                "isFullyValid: $isFullyValid" +
                "}"
    }

    companion object {
        fun UserId.generateUserTag() = localpart.toSha256().take(6)

        fun initialsOf(displayName: String?): String {
            return displayName?.trim()
                ?.split("""[\s_\-.]+""".toRegex()) // split on space, underscore, hyphen, or dot
                ?.let {
                    when {
                        it.size >= 2 -> it[0].take(1) + it[1].take(1)
                        it.isNotEmpty() -> it[0].take(1)
                        else -> ""
                    }
                } ?: ""
        }
    }
}
