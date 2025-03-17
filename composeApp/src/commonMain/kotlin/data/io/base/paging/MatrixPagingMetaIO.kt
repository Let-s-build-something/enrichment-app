package data.io.base.paging

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import database.AppRoomDatabase
import kotlinx.datetime.Clock
import kotlinx.serialization.Serializable

/** Meta information of paging response from Matrix protocol */
@Serializable
@Entity(tableName = AppRoomDatabase.TABLE_MATRIX_PAGING_META)
data class MatrixPagingMetaIO(

    /** Local room database identifier */
    @PrimaryKey(autoGenerate = false)
    @ColumnInfo("entity_id")
    val entityId: String,

    /** Type of paginated entity */
    @ColumnInfo("entity_type")
    val entityType: String?,

    /** A flag whether there is another page after this one or not */
    @ColumnInfo("next_batch")
    val nextBatch: String?,

    /** The starting batch of all the data in the DB, this serves as a limit for loading older messages */
    @ColumnInfo("current_batch")
    val currentBatch: String? = null,

    val prevBatch: String?,

    /** At what time was this object created in milliseconds */
    @ColumnInfo("created_at")
    val createdAt: Long = Clock.System.now().toEpochMilliseconds()
)
