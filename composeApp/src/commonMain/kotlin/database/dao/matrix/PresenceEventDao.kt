package database.dao.matrix

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import data.io.user.PresenceData
import database.AppRoomDatabase

/** Interface for communication with local Room database */
@Dao
interface PresenceEventDao {

    /** retrieves filtered items by state */
    @Query("SELECT * FROM ${AppRoomDatabase.TABLE_PRESENCE_EVENT} " +
            "WHERE user_id_full = :userId " +
            "LIMIT 1")
    suspend fun get(
        userId: String
    ): PresenceData?

    /** Inserts or updates a set of item objects */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<PresenceData>)

    @Query("DELETE FROM ${AppRoomDatabase.TABLE_PRESENCE_EVENT}")
    suspend fun removeAll()
}
