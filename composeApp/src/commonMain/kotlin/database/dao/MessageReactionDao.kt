package database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import data.io.social.network.conversation.message.MessageReactionIO
import database.AppRoomDatabase

@Dao
interface MessageReactionDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReplace(item: MessageReactionIO)

    @Query("""
        DELETE FROM ${AppRoomDatabase.TABLE_MESSAGE_REACTION}
        WHERE event_id = :eventId
        """)
    suspend fun remove(eventId: String)

    @Query("""
        SELECT * FROM ${AppRoomDatabase.TABLE_MESSAGE_REACTION}
        WHERE message_id = :messageId
        """)
    suspend fun getAll(messageId: String): List<MessageReactionIO>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<MessageReactionIO>)

    @Query("DELETE FROM ${AppRoomDatabase.TABLE_MESSAGE_REACTION}")
    suspend fun removeAll()
}