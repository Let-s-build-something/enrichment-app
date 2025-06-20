package database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import database.AppRoomDatabase
import ui.conversation.components.experimental.gravity.GravityValue

/** Interface for communication with local Room database */
@Dao
interface GravityDao {
    @Query("SELECT * FROM ${AppRoomDatabase.TABLE_GRAVITY} " +
            "WHERE conversation_id = :conversationId ")
    suspend fun getAll(conversationId: String?): List<GravityValue>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: GravityValue)

    @Query("""
        DELETE FROM ${AppRoomDatabase.TABLE_GRAVITY}
        WHERE conversation_id = :conversationId
        """)
    suspend fun removeAll(conversationId: String)

    @Query("""
        DELETE FROM ${AppRoomDatabase.TABLE_GRAVITY}
        """)
    suspend fun removeAll()
}