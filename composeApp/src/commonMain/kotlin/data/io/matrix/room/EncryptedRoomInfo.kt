package data.io.matrix.room

import androidx.room.ColumnInfo
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import net.folivo.trixnity.core.model.events.m.room.HistoryVisibilityEventContent
import net.folivo.trixnity.core.model.keys.EncryptionAlgorithm

@Serializable
data class EncryptedRoomInfo(
    val id: String,

    @SerialName("history_visibility")
    @ColumnInfo(name = "history_visibility")
    val historyVisibility: HistoryVisibilityEventContent.HistoryVisibility,
    val algorithm: EncryptionAlgorithm,
    val type: RoomType
)