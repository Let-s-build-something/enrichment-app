package database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import data.io.matrix.room.ConversationRoomIO
import database.AppRoomDatabase

/** Interface for communication with local Room database */
@Dao
interface ConversationRoomDao {

    /** Returns paginated conversation based on the owner as defined by [ownerPublicId] */
    @Query("SELECT * FROM ${AppRoomDatabase.ROOM_CONVERSATION_ROOM_TABLE} " +
            "WHERE owner_public_id = :ownerPublicId " +
            "AND batch = :batch ")
    suspend fun getPaginated(
        ownerPublicId: String?,
        batch: String?
    ): List<ConversationRoomIO>

    /** Returns all conversations related to an owner as defined by [ownerPublicId] */
    @Query("SELECT * FROM ${AppRoomDatabase.ROOM_CONVERSATION_ROOM_TABLE} " +
            "WHERE owner_public_id = :ownerPublicId ")
    suspend fun getNonFiltered(
        ownerPublicId: String?
    ): List<ConversationRoomIO>

    /** Returns all conversations specific to proximity bounds as defined by [proximityMin] and [proximityMax] */
    @Query("SELECT * FROM ${AppRoomDatabase.ROOM_CONVERSATION_ROOM_TABLE} " +
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

    /** Counts the number of items */
    @Query("UPDATE ${AppRoomDatabase.ROOM_CONVERSATION_ROOM_TABLE} " +
            "SET proximity = :proximity " +
            "WHERE owner_public_id = :ownerPublicId " +
            "AND id = :id ")
    suspend fun updateProximity(
        id: String?,
        ownerPublicId: String?,
        proximity: Float
    )

    /** Counts the number of items */
    @Query("SELECT COUNT(*) FROM ${AppRoomDatabase.ROOM_CONVERSATION_ROOM_TABLE} " +
            "WHERE owner_public_id = :ownerPublicId")
    suspend fun getCount(ownerPublicId: String?): Int

    /** Inserts or updates a set of item objects */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<ConversationRoomIO>)

    /** Removes all items from the database */
    @Query("DELETE FROM ${AppRoomDatabase.ROOM_CONVERSATION_ROOM_TABLE}" +
            " WHERE owner_public_id = :ownerPublicId")
    suspend fun removeAll(ownerPublicId: String?)
}