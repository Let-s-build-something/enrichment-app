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

    /** returns specific paging meta data for a given entity identification [entityId] */
    @Query("SELECT * FROM $TABLE_MATRIX_PAGING_META " +
            "WHERE entity_id = :entityId")
    suspend fun getByEntityId(entityId: String): MatrixPagingMetaIO?
}