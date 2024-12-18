package database

import androidx.room.Dao
import androidx.room.Query
import data.io.user.NetworkItemIO

/** Interface for communication with local Room database */
@Dao
interface NetworkItemDao {

    /** Returns all network items */
    @Query("SELECT * FROM ${AppRoomDatabase.ROOM_NETWORK_ITEM_TABLE} WHERE ownerPublicId == :ownerPublicId ORDER BY proximity DESC")
    suspend fun getNetworkItems(ownerPublicId: String): List<NetworkItemIO>?
}