package database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import data.io.social.network.conversation.message.ConversationMessageIO
import database.AppRoomDatabase

/** Interface for communication with local Room database */
@Dao
interface ConversationMessageDao {

    @Query("""
        SELECT * FROM ${AppRoomDatabase.TABLE_CONVERSATION_MESSAGE}
            WHERE conversation_id = :conversationId
            ORDER BY sent_at DESC 
            LIMIT :limit
            OFFSET :offset
            """)
    suspend fun getPaginated(
        conversationId: String?,
        limit: Int,
        offset: Int
    ): List<ConversationMessageIO>

    /** Returns anchored items related to a single message */
    @Query("""
        SELECT * FROM ${AppRoomDatabase.TABLE_CONVERSATION_MESSAGE}
            WHERE conversation_id = :conversationId
            AND (anchor_message_id = :anchorMessageId OR parent_anchor_message_id = :anchorMessageId)
            ORDER BY sent_at DESC 
            LIMIT :limit
            OFFSET :offset
            """)
    suspend fun getAnchoredPaginated(
        conversationId: String?,
        anchorMessageId: String?,
        limit: Int,
        offset: Int
    ): List<ConversationMessageIO>

    /** Counts the number of items */
    @Query("SELECT COUNT(*) FROM ${AppRoomDatabase.TABLE_CONVERSATION_MESSAGE} " +
            "WHERE conversation_id = :conversationId ")
    suspend fun getCount(conversationId: String?): Int

    /** Inserts or updates a set of item objects */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<ConversationMessageIO>)

    /** Retrieves a single item */
    @Query("SELECT * FROM ${AppRoomDatabase.TABLE_CONVERSATION_MESSAGE} " +
            "WHERE id = :messageId " +
            "LIMIT 1")
    suspend fun get(messageId: String?): ConversationMessageIO?

    /** marks a message as transcribed */
    @Query("UPDATE ${AppRoomDatabase.TABLE_CONVERSATION_MESSAGE} " +
            "SET transcribed = :transcribed " +
            "WHERE id = :messageId ")
    suspend fun transcribe(messageId: String, transcribed: Boolean)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIgnore(item: ConversationMessageIO): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReplace(item: ConversationMessageIO)

    /** Removes all items from the database */
    @Query("DELETE FROM ${AppRoomDatabase.TABLE_CONVERSATION_MESSAGE}")
    suspend fun removeAll()
}