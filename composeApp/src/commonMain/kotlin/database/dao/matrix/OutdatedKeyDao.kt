package database.dao.matrix

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import data.io.matrix.crypto.OutdatedKey
import database.AppRoomDatabase

/** Interface for communication with local Room database */
@Dao
interface OutdatedKeyDao {
    @Query("SELECT * FROM ${AppRoomDatabase.TABLE_OUTDATED_KEYS}")
    suspend fun getAll(): List<OutdatedKey>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<OutdatedKey>)

    @Query("DELETE FROM ${AppRoomDatabase.TABLE_OUTDATED_KEYS}")
    suspend fun removeAll()
}
