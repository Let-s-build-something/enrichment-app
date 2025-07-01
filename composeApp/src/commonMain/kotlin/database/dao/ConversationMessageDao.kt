package database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import data.io.social.network.conversation.message.ConversationMessageIO
import data.io.social.network.conversation.message.FullConversationMessage
import data.io.social.network.conversation.message.MessageState
import database.AppRoomDatabase

/** Interface for communication with local Room database */
@Dao
interface ConversationMessageDao {

    @Transaction
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
    ): List<FullConversationMessage>

    /** Returns anchored items related to a single message */
    @Transaction
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
    ): List<FullConversationMessage>

    /** Returns anchored items related to a single message */
    @Transaction
    @Query("""
        SELECT * FROM ${AppRoomDatabase.TABLE_CONVERSATION_MESSAGE}
            WHERE conversation_id = :conversationId
            AND content like '%' || :query || '%'
            AND author_public_id != "SYSTEM"
            ORDER BY sent_at DESC 
            LIMIT :limit
            OFFSET :offset
            """)
    suspend fun queryPaginated(
        conversationId: String,
        mimeTypes: List<String>,
        query: String,
        limit: Int,
        offset: Int
    ): List<FullConversationMessage>

    /** Retrieves a single item */
    @Transaction
    @Query("SELECT * FROM ${AppRoomDatabase.TABLE_CONVERSATION_MESSAGE} " +
            "WHERE id = :messageId " +
            "LIMIT 1")
    suspend fun get(messageId: String?): FullConversationMessage?

    @Query("""
        SELECT * FROM ${AppRoomDatabase.TABLE_CONVERSATION_MESSAGE} 
        WHERE author_public_id = :senderUserId 
        AND verification IS NOT NULL
        ORDER BY sent_at DESC
    """)
    suspend fun getPendingVerifications(senderUserId: String?): List<ConversationMessageIO>

    /** Counts the number of items */
    @Query("SELECT COUNT(*) FROM ${AppRoomDatabase.TABLE_CONVERSATION_MESSAGE} " +
            "WHERE conversation_id = :conversationId ")
    suspend fun getCount(conversationId: String?): Int

    /** Counts the number of items */
    @Query("""
        SELECT COUNT(*) FROM ${AppRoomDatabase.TABLE_CONVERSATION_MESSAGE}
            WHERE conversation_id = :conversationId 
            AND content like '%' || :query || '%'
            AND author_public_id != "SYSTEM"
            """)
    suspend fun getQueryCount(
        query: String,
        conversationId: String?
    ): Int

    /** Inserts or updates a set of item objects */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<ConversationMessageIO>)

    /** marks a message as transcribed */
    @Query("UPDATE ${AppRoomDatabase.TABLE_CONVERSATION_MESSAGE} " +
            "SET transcribed = :transcribed " +
            "WHERE id = :messageId ")
    suspend fun transcribe(messageId: String, transcribed: Boolean)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIgnore(item: ConversationMessageIO): Long

    @Query("""
           UPDATE ${AppRoomDatabase.TABLE_CONVERSATION_MESSAGE}
            SET content = :message, 
                edited = :edited
            WHERE id = :id
        """)
    suspend fun updateMessage(id: String, message: String, edited: Boolean = true)

    @Query("""
           UPDATE ${AppRoomDatabase.TABLE_CONVERSATION_MESSAGE}
            SET state = :state
            WHERE id = :id
        """)
    suspend fun updateState(id: String, state: MessageState)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReplace(item: ConversationMessageIO)

    /** Removes all items from the database */
    @Query("DELETE FROM ${AppRoomDatabase.TABLE_CONVERSATION_MESSAGE} WHERE id = :id")
    suspend fun remove(id: String): Int

    /** Removes all items from the database */
    @Query("DELETE FROM ${AppRoomDatabase.TABLE_CONVERSATION_MESSAGE}")
    suspend fun removeAll()
}