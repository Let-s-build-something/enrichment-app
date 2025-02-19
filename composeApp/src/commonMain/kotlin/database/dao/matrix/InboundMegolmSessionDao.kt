package database.dao.matrix

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import data.io.matrix.crypto.StoredInboundMegolmSessionEntity
import database.AppRoomDatabase
import net.folivo.trixnity.core.model.RoomId

/** Interface for communication with local Room database */
@Dao
interface InboundMegolmSessionDao {

    /** retrieves filtered items by state */
    @Query("SELECT * FROM ${AppRoomDatabase.TABLE_INBOUND_MEGOLM_SESSION} " +
            "WHERE id = :id " +
            "LIMIT 1")
    suspend fun get(id: String): StoredInboundMegolmSessionEntity?

    suspend fun get(
        roomId: RoomId,
        sessionId: String
    ): StoredInboundMegolmSessionEntity? = get(id = "${roomId.full}-$sessionId")

    /** Inserts or updates a set of item objects */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: StoredInboundMegolmSessionEntity)
}
