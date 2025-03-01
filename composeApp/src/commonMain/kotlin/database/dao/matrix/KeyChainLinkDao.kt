package database.dao.matrix

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import data.shared.crypto.model.KeyChainLink
import database.AppRoomDatabase

/** Interface for communication with local Room database */
@Dao
interface KeyChainLinkDao {
    @Query("SELECT * FROM ${AppRoomDatabase.TABLE_KEY_CHAIN_LINK} " +
            "WHERE signing_user_id = :userId ")
    suspend fun getByUserId(userId: String): List<KeyChainLink>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: KeyChainLink)

    @Query("DELETE FROM ${AppRoomDatabase.TABLE_KEY_CHAIN_LINK} " +
            "WHERE id IN (:ids) ")
    suspend fun removeWhere(ids: List<String>)

    @Query("DELETE FROM ${AppRoomDatabase.TABLE_KEY_CHAIN_LINK}")
    suspend fun removeAll()
}
