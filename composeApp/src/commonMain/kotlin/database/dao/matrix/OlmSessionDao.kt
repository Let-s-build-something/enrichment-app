package database.dao.matrix

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import data.io.matrix.crypto.StoredOlmSessionEntity
import database.AppRoomDatabase

/** Interface for communication with local Room database */
@Dao
interface OlmSessionDao {

    /** retrieves filtered items by state */
    @Query("SELECT * FROM ${AppRoomDatabase.TABLE_OLM_SESSION} " +
            "WHERE id = :senderKey ")
    suspend fun getSentItems(senderKey: String?): List<StoredOlmSessionEntity>

    /** Inserts or updates a set of item objects */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<StoredOlmSessionEntity>)

    @Query("DELETE FROM ${AppRoomDatabase.TABLE_OLM_SESSION}")
    suspend fun removeAll()
}
