package data.io.social.network

import kotlinx.serialization.Serializable

/** A request for a search of users */
@Serializable
data class SearchUsersRequest(
    /** required verbatim query for display name of the target users */
    val displayNameQuery: String? = null,

    /** optional verbatim query for a tag of the target users */
    val tagQuery: String? = null,
)