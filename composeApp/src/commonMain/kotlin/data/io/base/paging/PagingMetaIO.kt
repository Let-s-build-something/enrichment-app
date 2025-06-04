package data.io.base.paging

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import database.AppRoomDatabase
import kotlinx.datetime.Clock
import kotlinx.serialization.Serializable

/** Meta information of paging response */
@Serializable
@Entity(tableName = AppRoomDatabase.TABLE_PAGING_META)
data class PagingMetaIO(

    /** Local room database identifier */
    @PrimaryKey(autoGenerate = false)
    @ColumnInfo("entity_id")
    val entityId: String,

    /** Type of paginated entity */
    @ColumnInfo("entity_type")
    val entityType: String? = null,

    /** Index of this page */
    @ColumnInfo("current_page")
    val currentPage: Int? = null,

    /** A flag whether there is another page after this one or not */
    @ColumnInfo("next_page")
    val nextPage: Int? = null,

    /** At what time was this object created in milliseconds */
    @ColumnInfo("created_at")
    val createdAt: Long = Clock.System.now().toEpochMilliseconds(),

    /** Index of previous page, mainly for local Room database */
    @ColumnInfo("previous_page")
    val previousPage: Int? = null
)