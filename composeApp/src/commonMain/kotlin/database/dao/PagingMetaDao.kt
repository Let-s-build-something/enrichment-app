package database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import data.io.base.paging.PagingMetaIO
import database.AppRoomDatabase.Companion.TABLE_PAGING_META

/** Interface for communication with local Room database */
@Dao
interface PagingMetaDao {

    /** Inserts all paging meta data */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(pagingMeta: List<PagingMetaIO>)

    /** returns specific paging meta data for a given entity identification [entityId] */
    @Query("SELECT * FROM $TABLE_PAGING_META WHERE entity_id = :entityId")
    suspend fun getByEntityId(entityId: String): PagingMetaIO?

    /** deletes all paging meta data */
    @Query("DELETE FROM $TABLE_PAGING_META")
    suspend fun removeAll()

    /** returns when was the last time we used RestApi data */
    @Query("SELECT created_at FROM $TABLE_PAGING_META " +
            "WHERE entity_type = :entityType " +
            "ORDER BY created_at DESC LIMIT 1")
    suspend fun getCreationTime(entityType: String): Long?
}