package database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import data.io.base.paging.MatrixPagingMetaIO
import database.AppRoomDatabase.Companion.TABLE_MATRIX_PAGING_META

/** Interface for communication with local Room database */
@Dao
interface MatrixPagingMetaDao {

    /** Inserts a paging meta data */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(pagingMeta: MatrixPagingMetaIO)

    /** returns when was the last time we used RestApi data */
    @Query("SELECT created_at FROM $TABLE_MATRIX_PAGING_META " +
            "WHERE entity_type = :entityType " +
            "ORDER BY created_at DESC LIMIT 1")
    suspend fun getCreationTime(entityType: String): Long?
}