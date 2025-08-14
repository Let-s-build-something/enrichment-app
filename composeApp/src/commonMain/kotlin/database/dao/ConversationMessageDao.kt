package database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.RawQuery
import androidx.room.RoomRawQuery
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
        query: String,
        limit: Int,
        offset: Int
    ): List<FullConversationMessage>

    @Transaction
    @Query("""
        SELECT * FROM ${AppRoomDatabase.TABLE_CONVERSATION_MESSAGE}
        WHERE conversation_id = :conversationId
        ORDER BY sent_at DESC
        LIMIT 1
    """)
    suspend fun getLastMessage(conversationId: String): FullConversationMessage?

    @Transaction
    @RawQuery
    suspend fun queryPaginatedMimeType(query: RoomRawQuery): List<FullConversationMessage>

    @Transaction
    @RawQuery
    suspend fun countQueryPaginatedMimeType(query: RoomRawQuery): Int

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

    @Query("""
    SELECT COUNT(*) 
    FROM ${AppRoomDatabase.TABLE_CONVERSATION_MESSAGE}
    WHERE conversation_id = :conversationId
    AND sent_at > (
        SELECT COALESCE(
            (SELECT sent_at 
             FROM ${AppRoomDatabase.TABLE_CONVERSATION_MESSAGE}
             WHERE id = :messageId
             AND conversation_id = :conversationId),
            0
        )
    )
""")
    suspend fun getMessagesAfterCount(
        messageId: String,
        conversationId: String
    ): Int

    /** Counts the number of items */
    @Query("SELECT COUNT(*) FROM ${AppRoomDatabase.TABLE_CONVERSATION_MESSAGE} " +
            "WHERE conversation_id = :conversationId ")
    suspend fun getCount(conversationId: String?): Int

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

    fun buildQueryPaginatedWithMimeTypes(
        conversationId: String,
        query: String,
        mimeTypes: List<String>,
        limit: Int = 100,
        offset: Int = 0,
        countOnly: Boolean = false
    ): RoomRawQuery {
        val sql = StringBuilder()
        val args = mutableListOf<Any?>()

        if (mimeTypes.isEmpty()) {
            sql.append(
                """
            ${if (countOnly) "SELECT COUNT(id)" else "SELECT *"} FROM ${AppRoomDatabase.TABLE_CONVERSATION_MESSAGE}
            WHERE conversation_id = ?
              AND content LIKE '%' || ? || '%'
              AND author_public_id != 'SYSTEM'
            ORDER BY sent_at DESC
            LIMIT ? OFFSET ?
            """.trimIndent()
            )
            args.add(conversationId)
            args.add(query)
            args.add(limit)
            args.add(offset)
        } else {
            sql.append(
                """
                ${if (countOnly) "SELECT COUNT(DISTINCT m.id)" else "SELECT DISTINCT m.*"} FROM ${AppRoomDatabase.TABLE_CONVERSATION_MESSAGE} m
                INNER JOIN ${AppRoomDatabase.TABLE_MEDIA} media ON media.message_id = m.id
                WHERE m.conversation_id = ?
                AND m.author_public_id != 'SYSTEM'
                AND (
            """.trimIndent()
            )

            args.add(conversationId)

            mimeTypes.forEachIndexed { index, _ ->
                sql.append("media.mimetype LIKE '%' || ? || '%'")
                if (index < mimeTypes.lastIndex) sql.append(" OR ")
                args.add(mimeTypes[index])
            }

            if (query.isNotBlank()) {
                sql.append("AND m.content LIKE '%' || ? || '%'")
                args.add(query)
            }

            sql.append(
                """
            )
            ORDER BY m.sent_at DESC
            LIMIT ? OFFSET ?
            """.trimIndent()
            )
            args.add(limit)
            args.add(offset)
        }

        return RoomRawQuery(
            sql = sql.toString(),
            onBindStatement = { stmt ->
                args.forEachIndexed { i, arg ->
                    when (arg) {
                        is String -> stmt.bindText(i + 1, arg)
                        is Int -> stmt.bindLong(i + 1, arg.toLong())
                        is Long -> stmt.bindLong(i + 1, arg)
                        is Float -> stmt.bindDouble(i + 1, arg.toDouble())
                        is Double -> stmt.bindDouble(i + 1, arg)
                        null -> stmt.bindNull(i + 1)
                        else -> error("Unsupported bind type: ${arg::class}")
                    }
                }
            }
        )
    }
}