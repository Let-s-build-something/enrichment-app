package ui.conversation.components.experimental.gravity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import database.AppRoomDatabase.Companion.TABLE_GRAVITY
import kotlinx.serialization.Serializable

@Entity(tableName = TABLE_GRAVITY)
@Serializable
data class GravityValue(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,

    @ColumnInfo("conversation_id")
    val conversationId: String?,
    
    val fraction: Float,
    val offset: Float
)