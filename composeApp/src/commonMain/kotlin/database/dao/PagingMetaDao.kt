package database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import data.io.base.PagingMetaIO
import database.AppRoomDatabase.Companion.ROOM_PAGING_META_TABLE

/** Interface for communication with local Room database */
@Dao
interface PagingMetaDao {

    /** Inserts all paging meta data */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(pagingMeta: List<PagingMetaIO>)

    /** returns specific paging meta data for a given entity identification [entityId] */
    @Query("SELECT * FROM $ROOM_PAGING_META_TABLE WHERE entity_id = :entityId")
    suspend fun getPagingMetaByEntityId(entityId: String): PagingMetaIO?

    /** deletes all paging meta data */
    @Query("DELETE FROM $ROOM_PAGING_META_TABLE")
    suspend fun removeAll()

    /** returns when was the last time we used RestApi data */
    @Query("SELECT created_at FROM $ROOM_PAGING_META_TABLE ORDER BY created_at DESC LIMIT 1")
    suspend fun getCreationTime(): Long?
}