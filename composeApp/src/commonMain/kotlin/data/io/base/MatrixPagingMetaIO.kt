package data.io.base

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import database.AppRoomDatabase
import kotlinx.datetime.Clock
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Meta information of paging response from Matrix protocol */
@Serializable
@Entity(tableName = AppRoomDatabase.ROOM_PAGING_META_TABLE)
data class MatrixPagingMetaIO(

    /** Local room database identifier */
    @PrimaryKey(autoGenerate = false)
    @ColumnInfo("entity_id")
    val entityId: String,

    /** Type of paginated entity */
    @ColumnInfo("entity_type")
    val entityType: PagingEntityType? = null,

    /** A flag whether there is another page after this one or not */
    @SerialName("next_batch")
    val nextBatch: String? = null,

    /** Identification of current batch */
    val batch: String? = null,

    /** At what time was this object created in milliseconds */
    @ColumnInfo(name = "created_at")
    val createdAt: Long = Clock.System.now().toEpochMilliseconds()
)
