package database.dao.matrix

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import data.shared.crypto.model.DeviceKey
import database.AppRoomDatabase

/** Interface for communication with local Room database */
@Dao
interface DeviceKeyDao {
    @Query("SELECT * FROM ${AppRoomDatabase.TABLE_DEVICE_KEY} " +
            "WHERE user_id = :userId ")
    suspend fun getAllByUserId(userId: String): List<DeviceKey>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<DeviceKey>)

    @Query("DELETE FROM ${AppRoomDatabase.TABLE_DEVICE_KEY} " +
            "WHERE user_id = :userId ")
    suspend fun removeWhere(userId: String)

    @Query("DELETE FROM ${AppRoomDatabase.TABLE_DEVICE_KEY}")
    suspend fun removeAll()
}
