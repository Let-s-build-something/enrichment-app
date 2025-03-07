package database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import data.io.matrix.room.ConversationRoomIO
import data.io.matrix.room.EncryptedRoomInfo
import database.AppRoomDatabase
import net.folivo.trixnity.core.model.events.m.room.HistoryVisibilityEventContent
import net.folivo.trixnity.core.model.keys.EncryptionAlgorithm

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

    @Query("SELECT id, history_visibility, algorithm, type FROM ${AppRoomDatabase.TABLE_CONVERSATION_ROOM} " +
            "WHERE algorithm IS NOT NULL")
    suspend fun getEncrypted(): List<EncryptedRoomInfo>

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

    @Query("SELECT prev_batch FROM ${AppRoomDatabase.TABLE_CONVERSATION_ROOM} " +
            "WHERE id = :id " +
            "LIMIT 1")
    suspend fun getPrevBatch(id: String?): String?

    /** Counts the number of items */
    @Query("SELECT COUNT(*) FROM ${AppRoomDatabase.TABLE_CONVERSATION_ROOM} " +
            "WHERE owner_public_id = :ownerPublicId")
    suspend fun getCount(ownerPublicId: String?): Int

    @Query("SELECT * FROM ${AppRoomDatabase.TABLE_CONVERSATION_ROOM} " +
            "WHERE id = :id " +
            "LIMIT 1")
    suspend fun get(id: String?): ConversationRoomIO?

    @Query("SELECT * FROM ${AppRoomDatabase.TABLE_CONVERSATION_ROOM} " +
            "WHERE owner_public_id = :ownerPublicId " +
            "AND id = :id " +
            "LIMIT 1")
    suspend fun getItem(id: String?, ownerPublicId: String?): ConversationRoomIO?

    @Query("SELECT history_visibility FROM ${AppRoomDatabase.TABLE_CONVERSATION_ROOM} " +
            "WHERE owner_public_id = :ownerPublicId " +
            "AND id = :id " +
            "LIMIT 1")
    suspend fun getHistoryVisibility(id: String?, ownerPublicId: String?): HistoryVisibilityEventContent.HistoryVisibility?

    @Query("SELECT algorithm FROM ${AppRoomDatabase.TABLE_CONVERSATION_ROOM} " +
            "WHERE owner_public_id = :ownerPublicId " +
            "AND id = :id " +
            "LIMIT 1")
    suspend fun getAlgorithm(id: String?, ownerPublicId: String?): EncryptionAlgorithm?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<ConversationRoomIO>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: ConversationRoomIO)

    @Query("DELETE FROM ${AppRoomDatabase.TABLE_CONVERSATION_ROOM}")
    suspend fun removeAll()
}