package database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import data.io.matrix.room.ConversationRoomIO
import data.io.matrix.room.RoomType
import database.AppRoomDatabase

/** Interface for communication with local Room database */
@Dao
interface ConversationRoomDao {

    /** Returns paginated conversation based on the owner as defined by [ownerPublicId] */
    @Query("SELECT * FROM ${AppRoomDatabase.TABLE_CONVERSATION_ROOM} " +
            "WHERE owner_public_id = :ownerPublicId " +
            "ORDER BY proximity DESC, last_message_timestamp DESC " +
            "LIMIT :limit " +
            "OFFSET :offset")
    suspend fun getPaginated(
        ownerPublicId: String?,
        limit: Int,
        offset: Int
    ): List<ConversationRoomIO>

    /** Returns all conversations related to an owner as defined by [ownerPublicId] */
    @Query("SELECT * FROM ${AppRoomDatabase.TABLE_CONVERSATION_ROOM} " +
            "WHERE owner_public_id = :ownerPublicId ")
    suspend fun getNonFiltered(
        ownerPublicId: String?
    ): List<ConversationRoomIO>

    /** Returns all conversations specific to proximity bounds as defined by [proximityMin] and [proximityMax] */
    @Query("SELECT * FROM ${AppRoomDatabase.TABLE_CONVERSATION_ROOM} " +
            "WHERE owner_public_id = :ownerPublicId " +
            "AND id != :excludeId " +
            "AND proximity BETWEEN :proximityMin AND :proximityMax " +
            "LIMIT :count")
    suspend fun getByProximity(
        count: Int,
        ownerPublicId: String?,
        proximityMin: Float,
        proximityMax: Float,
        excludeId: String?
    ): List<ConversationRoomIO>

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
    @Query("SELECT COUNT(*) FROM ${AppRoomDatabase.TABLE_CONVERSATION_ROOM} " +
            "WHERE owner_public_id = :ownerPublicId")
    suspend fun getCount(ownerPublicId: String?): Int

    @Query("SELECT * FROM ${AppRoomDatabase.TABLE_CONVERSATION_ROOM} " +
            "WHERE id = :id " +
            "LIMIT 1")
    suspend fun get(id: String?): ConversationRoomIO?

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

    @Query("SELECT * FROM ${AppRoomDatabase.TABLE_CONVERSATION_ROOM} " +
            "WHERE owner_public_id = :ownerPublicId " +
            "AND id = :id " +
            "LIMIT 1")
    suspend fun getItem(id: String?, ownerPublicId: String?): ConversationRoomIO?

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