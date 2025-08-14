package database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import data.io.matrix.room.ConversationRoomIO
import data.io.matrix.room.FullConversationRoom
import data.io.matrix.room.RoomType
import database.AppRoomDatabase

/** Interface for communication with local Room database */
@Dao
interface ConversationRoomDao {

    /** Returns paginated conversation based on the owner as defined by [ownerPublicId] */
    @Query("""
        SELECT r.*
        FROM ${AppRoomDatabase.TABLE_CONVERSATION_ROOM} r
        LEFT JOIN (
            SELECT conversation_id, MAX(sent_at) AS lastMessageTimestamp
            FROM ${AppRoomDatabase.TABLE_CONVERSATION_MESSAGE}
            GROUP BY conversation_id
        ) lm
        ON lm.conversation_id = r.id
        WHERE r.owner_public_id = :ownerPublicId
          AND (r.type = 'Joined' OR r.type = 'Invited')
        ORDER BY r.proximity DESC, lm.lastMessageTimestamp DESC
        LIMIT :limit OFFSET :offset
    """)
    @Transaction
    suspend fun getPaginated(
        ownerPublicId: String?,
        limit: Int,
        offset: Int
    ): List<FullConversationRoom>

    /** Returns all conversations related to an owner as defined by [ownerPublicId] */
    @Query("""
        SELECT * FROM ${AppRoomDatabase.TABLE_CONVERSATION_ROOM}
        WHERE owner_public_id = :ownerPublicId 
        AND type == "Joined"
        AND is_direct == false
        """
    )
    @Transaction
    suspend fun getOpenRooms(
        ownerPublicId: String?
    ): List<FullConversationRoom>

    /** Returns all conversations specific to proximity bounds as defined by [proximityMin] and [proximityMax] */
    @Query("""
        SELECT * FROM ${AppRoomDatabase.TABLE_CONVERSATION_ROOM}
            WHERE owner_public_id = :ownerPublicId
            AND id != :excludeId
            AND (type == "Joined" OR type == "Invited")
            AND proximity BETWEEN :proximityMin AND :proximityMax
            LIMIT :count
            """)
    @Transaction
    suspend fun getByProximity(
        count: Int,
        ownerPublicId: String?,
        proximityMin: Float,
        proximityMax: Float,
        excludeId: String?
    ): List<FullConversationRoom>

    @Query("UPDATE ${AppRoomDatabase.TABLE_CONVERSATION_ROOM} " +
            "SET proximity = :proximity " +
            "WHERE owner_public_id = :ownerPublicId " +
            "AND id = :id ")
    suspend fun updateProximity(
        id: String?,
        ownerPublicId: String?,
        proximity: Float
    )

    /** Counts the number of items */
    @Query("""
        SELECT COUNT(*) FROM ${AppRoomDatabase.TABLE_CONVERSATION_ROOM}
        WHERE owner_public_id = :ownerPublicId
    """)
    suspend fun getCount(ownerPublicId: String?): Int

    @Query("SELECT * FROM ${AppRoomDatabase.TABLE_CONVERSATION_ROOM} " +
            "WHERE owner_public_id = :ownerPublicId " +
            "AND id = :id " +
            "LIMIT 1")
    @Transaction
    suspend fun get(id: String?, ownerPublicId: String?): FullConversationRoom?

    @Query("SELECT * FROM ${AppRoomDatabase.TABLE_CONVERSATION_ROOM} " +
            "WHERE id = :id " +
            "LIMIT 1")
    @Transaction
    suspend fun get(id: String?): FullConversationRoom?

    @Query("SELECT * FROM ${AppRoomDatabase.TABLE_CONVERSATION_ROOM} ")
    @Transaction
    suspend fun getAll(): List<FullConversationRoom>

    @Query("""
           UPDATE ${AppRoomDatabase.TABLE_CONVERSATION_ROOM}
            SET prev_batch = :prevBatch
            WHERE id = :id
        """)
    suspend fun setPrevBatch(id: String?, prevBatch: String?)

    @Query("""
           UPDATE ${AppRoomDatabase.TABLE_CONVERSATION_ROOM}
            SET type = :newType
            WHERE id = :id
            AND owner_public_id = :ownerPublicId
        """)
    suspend fun setType(
        id: String?,
        ownerPublicId: String?,
        newType: RoomType?
    )

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<ConversationRoomIO>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: ConversationRoomIO)

    @Query("DELETE FROM ${AppRoomDatabase.TABLE_CONVERSATION_ROOM}")
    suspend fun removeAll()

    @Query("""
        DELETE FROM ${AppRoomDatabase.TABLE_CONVERSATION_ROOM}
        WHERE owner_public_id = :ownerPublicId
        AND id = :id
        """)
    suspend fun remove(id: String, ownerPublicId: String?)
}