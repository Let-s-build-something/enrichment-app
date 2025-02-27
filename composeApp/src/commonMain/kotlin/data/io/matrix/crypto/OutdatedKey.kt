package data.io.matrix.crypto

import androidx.room.ColumnInfo
import androidx.room.Entity
import database.AppRoomDatabase.Companion.TABLE_OUTDATED_KEYS
import kotlinx.serialization.Serializable
import net.folivo.trixnity.core.model.UserId

@Entity(tableName = TABLE_OUTDATED_KEYS)
@Serializable
data class OutdatedKey(
    @ColumnInfo(name = "user_id")
    val userId: UserId
)
