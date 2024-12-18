package database.dao

import androidx.room.Dao
import androidx.room.Query
import data.io.user.NetworkItemIO
import database.AppRoomDatabase

/** Interface for communication with local Room database */
@Dao
interface NetworkItemDao {

    /** Returns all network items */
    @Query("SELECT * FROM ${AppRoomDatabase.ROOM_NETWORK_ITEM_TABLE} WHERE ownerPublicId == :ownerPublicId ORDER BY proximity DESC")
    suspend fun getNetworkItems(ownerPublicId: String): List<NetworkItemIO>
}