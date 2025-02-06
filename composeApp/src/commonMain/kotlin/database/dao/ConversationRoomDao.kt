package database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import data.io.matrix.room.ConversationRoomIO
import database.AppRoomDatabase
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.auth.auth

/** Interface for communication with local Room database */
@Dao
interface ConversationRoomDao {

    /** Returns paginated conversation based on the owner as defined by [ownerPublicId] */
    @Query("SELECT * FROM ${AppRoomDatabase.ROOM_CONVERSATION_ROOM_TABLE} " +
            "WHERE owner_public_id = :ownerPublicId " +
            "AND batch = :batch ")
    suspend fun getPaginated(
        ownerPublicId: String? = Firebase.auth.currentUser?.uid,
        batch: String?
    ): List<ConversationRoomIO>

    /** Returns all conversations related to an owner as defined by [ownerPublicId] */
    @Query("SELECT * FROM ${AppRoomDatabase.ROOM_CONVERSATION_ROOM_TABLE} " +
            "WHERE owner_public_id = :ownerPublicId ")
    suspend fun getNonFiltered(
        ownerPublicId: String? = Firebase.auth.currentUser?.uid
    ): List<ConversationRoomIO>

    /** Returns all conversations specific to proximity bounds as defined by [proximityMin] and [proximityMax] */
    @Query("SELECT * FROM ${AppRoomDatabase.ROOM_CONVERSATION_ROOM_TABLE} " +
            "WHERE owner_public_id = :ownerPublicId " +
            "AND id != :excludeId " +
            "AND proximity BETWEEN :proximityMin AND :proximityMax " +
            "LIMIT :count")
    suspend fun getByProximity(
        count: Int,
        ownerPublicId: String? = Firebase.auth.currentUser?.uid,
        proximityMin: Float,
        proximityMax: Float,
        excludeId: String?
    ): List<ConversationRoomIO>

    /** Counts the number of items */
    @Query("UPDATE ${AppRoomDatabase.ROOM_CONVERSATION_ROOM_TABLE} " +
            "SET proximity = :proximity " +
            "WHERE id = :id ")
    suspend fun updateProximity(
        id: String?,
        proximity: Float
    )

    /** Counts the number of items */
    @Query("SELECT COUNT(*) FROM ${AppRoomDatabase.ROOM_CONVERSATION_ROOM_TABLE} " +
            "WHERE owner_public_id = :ownerPublicId")
    suspend fun getCount(ownerPublicId: String? = Firebase.auth.currentUser?.uid): Int

    /** Inserts or updates a set of item objects */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<ConversationRoomIO>)

    /** Removes all items from the database */
    @Query("DELETE FROM ${AppRoomDatabase.ROOM_CONVERSATION_ROOM_TABLE}")
    suspend fun removeAll()
}