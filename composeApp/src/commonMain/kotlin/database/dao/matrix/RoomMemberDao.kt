package database.dao.matrix

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import data.io.matrix.room.event.ConversationRoomMember
import database.AppRoomDatabase

/** Interface for communication with local Room database */
@Dao
interface RoomMemberDao {
    @Query("SELECT * FROM ${AppRoomDatabase.TABLE_ROOM_MEMBER}")
    suspend fun getAll(): List<ConversationRoomMember>

    @Query("SELECT * FROM ${AppRoomDatabase.TABLE_ROOM_MEMBER} " +
            "WHERE user_id IN (:userIds) ")
    suspend fun get(userIds: List<String>): List<ConversationRoomMember>

    @Query("SELECT * FROM ${AppRoomDatabase.TABLE_ROOM_MEMBER} " +
            "WHERE user_id = :userId ")
    suspend fun get(userId: String): ConversationRoomMember?

    @Query("SELECT * FROM ${AppRoomDatabase.TABLE_ROOM_MEMBER} " +
            "WHERE room_id = :roomId ")
    suspend fun getOfRoom(roomId: String): List<ConversationRoomMember>

    @Query("""
        SELECT * FROM ${AppRoomDatabase.TABLE_ROOM_MEMBER}
            WHERE room_id = :roomId
            AND user_id != :ignoreUserId
            ORDER BY timestamp DESC 
            LIMIT :limit
            OFFSET :offset
            """)
    suspend fun getPaginated(
        roomId: String?,
        limit: Int,
        offset: Int,
        ignoreUserId: String?,
    ): List<ConversationRoomMember>

    @Query("""
        SELECT * FROM ${AppRoomDatabase.TABLE_ROOM_MEMBER}
            WHERE display_name like '%' || :prompt || '%'
            OR user_id  like '%' || :prompt || '%'
            """)
    suspend fun searchByPrompt(prompt: String): List<ConversationRoomMember>

    @Query("SELECT COUNT(*) FROM ${AppRoomDatabase.TABLE_ROOM_MEMBER} " +
            "WHERE room_id = :roomId ")
    suspend fun getCount(roomId: String?): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<ConversationRoomMember>)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIgnore(item: ConversationRoomMember): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReplace(item: ConversationRoomMember)

    @Query("DELETE FROM ${AppRoomDatabase.TABLE_ROOM_MEMBER}")
    suspend fun removeAll()

    @Query("""
        DELETE FROM ${AppRoomDatabase.TABLE_ROOM_MEMBER}
        WHERE user_id = :userId
    """)
    suspend fun remove(userId: String)

    @Query("DELETE FROM ${AppRoomDatabase.TABLE_ROOM_MEMBER} WHERE room_id = :roomId")
    suspend fun removeAll(roomId: String)
}
