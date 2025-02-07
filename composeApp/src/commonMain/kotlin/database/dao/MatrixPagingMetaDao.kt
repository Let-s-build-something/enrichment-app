package database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import data.io.base.paging.MatrixPagingMetaIO
import database.AppRoomDatabase.Companion.ROOM_MATRIX_PAGING_META_TABLE

/** Interface for communication with local Room database */
@Dao
interface MatrixPagingMetaDao {

    /** Inserts all paging meta data */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(pagingMeta: List<MatrixPagingMetaIO>)

    /** Inserts a paging meta data */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(pagingMeta: MatrixPagingMetaIO)

    /** returns specific paging meta data for a given entity identification [entityId] */
    @Query("SELECT * FROM $ROOM_MATRIX_PAGING_META_TABLE WHERE entity_id = :entityId")
    suspend fun getByEntityId(entityId: String): MatrixPagingMetaIO?

    /** deletes all paging meta data */
    @Query("DELETE FROM $ROOM_MATRIX_PAGING_META_TABLE WHERE entity_type = :entityType")
    suspend fun removeAll(entityType: String)

    /** returns when was the last time we used RestApi data */
    @Query("SELECT created_at FROM $ROOM_MATRIX_PAGING_META_TABLE " +
            "WHERE entity_type = :entityType " +
            "ORDER BY created_at DESC LIMIT 1")
    suspend fun getCreationTime(entityType: String): Long?
}