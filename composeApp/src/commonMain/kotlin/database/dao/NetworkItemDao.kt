package database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import data.io.user.NetworkItemIO
import database.AppRoomDatabase
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.auth.auth

/** Interface for communication with local Room database */
@Dao
interface NetworkItemDao {

    /** Returns all network items */
    @Query("SELECT * FROM ${AppRoomDatabase.ROOM_NETWORK_ITEM_TABLE} " +
            "WHERE owner_public_id = :ownerPublicId " +
            "ORDER BY proximity DESC " +
            "LIMIT :limit " +
            "OFFSET :offset")
    suspend fun getPaginated(
        ownerPublicId: String?,
        limit: Int,
        offset: Int
    ): List<NetworkItemIO>

    /** Returns all network items related to an owner as defined by [ownerPublicId] */
    @Query("SELECT * FROM ${AppRoomDatabase.ROOM_NETWORK_ITEM_TABLE} " +
            "WHERE owner_public_id = :ownerPublicId ")
    suspend fun getNonFiltered(
        ownerPublicId: String? = Firebase.auth.currentUser?.uid
    ): List<NetworkItemIO>

    /** Returns all network items within the list [userPublicIds] */
    @Query("SELECT * FROM ${AppRoomDatabase.ROOM_NETWORK_ITEM_TABLE} " +
            "WHERE owner_public_id = :ownerPublicId " +
            "AND user_public_id IN (:userPublicIds)")
    suspend fun getItems(
        userPublicIds: List<String>?,
        ownerPublicId: String? = Firebase.auth.currentUser?.uid
    ): List<NetworkItemIO>

    /** Returns all network items specific to proximity bounds as defined by [proximityMin] and [proximityMax] */
    @Query("SELECT * FROM ${AppRoomDatabase.ROOM_NETWORK_ITEM_TABLE} " +
            "WHERE owner_public_id = :ownerPublicId " +
            "AND user_public_id != :excludeId " +
            "AND proximity BETWEEN :proximityMin AND :proximityMax " +
            "LIMIT :count")
    suspend fun getByProximity(
        count: Int,
        ownerPublicId: String?,
        proximityMin: Float,
        proximityMax: Float,
        excludeId: String?
    ): List<NetworkItemIO>

    /** Counts the number of items */
    @Query("SELECT COUNT(*) FROM ${AppRoomDatabase.ROOM_NETWORK_ITEM_TABLE} " +
            "WHERE owner_public_id = :ownerPublicId")
    suspend fun getCount(ownerPublicId: String?): Int

    /** Counts the number of items */
    @Query("UPDATE ${AppRoomDatabase.ROOM_NETWORK_ITEM_TABLE} " +
            "SET proximity = :proximity " +
            "WHERE owner_public_id = :ownerPublicId " +
            "AND public_id = :publicId ")
    suspend fun updateProximity(
        ownerPublicId: String? = Firebase.auth.currentUser?.uid,
        proximity: Float,
        publicId: String?
    )

    /** Inserts or updates a set of item objects */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<NetworkItemIO>)

    /** Removes all items from the database */
    @Query("DELETE FROM ${AppRoomDatabase.ROOM_NETWORK_ITEM_TABLE}")
    suspend fun removeAll()
}