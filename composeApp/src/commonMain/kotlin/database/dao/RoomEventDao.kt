package database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import data.io.matrix.room.MatrixEvent
import database.AppRoomDatabase

/** Interface for communication with local Room database */
@Dao
interface RoomEventDao {

    /** retrieves filtered items by state */
    @Query("SELECT * FROM ${AppRoomDatabase.TABLE_ROOM_EVENT} " +
            "WHERE room_id = :roomId " +
            "AND state_key = :stateKey " +
            "AND type = :type ")
    suspend fun filterStateItems(
        roomId: String?,
        stateKey: String,
        type: String
    ): List<MatrixEvent>?

    /** Inserts or updates a set of item objects */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<MatrixEvent>)
}