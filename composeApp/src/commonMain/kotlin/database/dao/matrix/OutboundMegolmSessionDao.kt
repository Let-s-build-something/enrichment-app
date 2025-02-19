package database.dao.matrix

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import data.io.matrix.crypto.StoredOutboundMegolmSessionEntity
import database.AppRoomDatabase

/** Interface for communication with local Room database */
@Dao
interface OutboundMegolmSessionDao {

    /** retrieves filtered items by state */
    @Query("SELECT * FROM ${AppRoomDatabase.TABLE_OUTBOUND_MEGOLM_SESSION} " +
            "WHERE id = :roomId " +
            "LIMIT 1")
    suspend fun get(roomId: String): StoredOutboundMegolmSessionEntity?

    /** Inserts or updates a set of item objects */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(items: StoredOutboundMegolmSessionEntity)
}
