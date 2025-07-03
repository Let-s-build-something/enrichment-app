package data.io.matrix.room

import kotlinx.serialization.Serializable

@Serializable
data class SearchRequest(val searchCategories: Categories) {
    @Serializable
    data class Categories(val roomEvents: RoomEventsCriteria? = null) {
        @Serializable
        data class RoomEventsCriteria(
            val eventContext: IncludeEventContext? = null,
            val filter: RoomEventsFilter? = null,
            val groupings: Groupings? = null,
            val includeState: Boolean? = null,
            val keys: Set<String>? = null,
            val orderBy: Ordering? = null,
            val searchTerm: String
        ) {
            @Serializable
            data class IncludeEventContext(
                val afterLimit: Long? = null,
                val beforeLimit: Long? = null,
                val includeProfile: Boolean? = null,
            )

            @Serializable
            data class Groupings(val groupBy: Set<Groups>? = null) {
                @Serializable
                data class Groups(val key: String? = null)
            }

            @Serializable
            enum class Ordering {
                Recent,
                Rank;

                override fun toString() = this.name.lowercase()
            }
        }

        @Serializable
        data class RoomEventsFilter(
            val containsUrl: Boolean? = null,
            val includeRedundantMembers: Boolean? = null,
            val lazyLoadMembers: Boolean? = null,
            val limit: Int? = null,
            val notRooms: Set<String>? = null,
            val notSenders: Set<String>? = null,
            val notTypes: Set<String>? = null,
            val rooms: Set<String>? = null,
            val senders: Set<String>? = null,
            val types: Set<String>? = null,
            val unreadThreadNotifications: Boolean? = null
        )
    }
}