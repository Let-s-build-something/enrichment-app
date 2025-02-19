package database.dao.matrix

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import data.io.matrix.crypto.StoredInboundMegolmMessageIndexEntity
import database.AppRoomDatabase
import net.folivo.trixnity.core.model.RoomId

/** Interface for communication with local Room database */
@Dao
interface MegolmMessageIndexDao {

    /** retrieves filtered items by state */
    @Query("SELECT * FROM ${AppRoomDatabase.TABLE_MEGOLM_MESSAGE_INDEX} " +
            "WHERE id = :id " +
            "LIMIT 1")
    suspend fun get(id: String): StoredInboundMegolmMessageIndexEntity?

    suspend fun get(
        roomId: RoomId,
        sessionId: String,
        messageIndex: Long
    ): StoredInboundMegolmMessageIndexEntity? = get(id = "${roomId.full}-$sessionId-$messageIndex")

    /** Inserts or updates a set of item objects */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: StoredInboundMegolmMessageIndexEntity)
}
