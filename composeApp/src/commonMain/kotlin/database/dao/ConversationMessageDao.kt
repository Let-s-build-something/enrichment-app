package database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import data.io.social.network.conversation.ConversationMessageIO
import database.AppRoomDatabase

/** Interface for communication with local Room database */
@Dao
interface ConversationMessageDao {

    /** Returns all items */
    @Query("SELECT * FROM ${AppRoomDatabase.ROOM_CONVERSATION_MESSAGE_TABLE} " +
            "WHERE conversation_id = :conversationId " +
            "LIMIT :limit " +
            "OFFSET :offset")
    suspend fun getPaginated(
        conversationId: String?,
        limit: Int,
        offset: Int
    ): List<ConversationMessageIO>

    /** Counts the number of items */
    @Query("SELECT COUNT(*) FROM ${AppRoomDatabase.ROOM_CONVERSATION_MESSAGE_TABLE} " +
            "WHERE conversation_id = :conversationId")
    suspend fun getCount(conversationId: String?): Int

    /** Inserts or updates a set of item objects */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<ConversationMessageIO>)

    /** Removes all items from the database */
    @Query("DELETE FROM ${AppRoomDatabase.ROOM_CONVERSATION_MESSAGE_TABLE}")
    suspend fun removeAll()
}