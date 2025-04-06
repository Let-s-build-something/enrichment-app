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

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<ConversationRoomMember>)

    @Query("DELETE FROM ${AppRoomDatabase.TABLE_ROOM_MEMBER}")
    suspend fun removeAll()
}
