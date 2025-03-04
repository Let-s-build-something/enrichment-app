package data.shared.crypto.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import database.AppRoomDatabase.Companion.TABLE_DEVICE_KEY
import kotlinx.serialization.Serializable

@Entity(TABLE_DEVICE_KEY)
@Serializable
data class DeviceKey(
    @ColumnInfo("user_id")
    val userId: String,
    val key: String,
    val value: StoredDeviceKeys,

    @PrimaryKey
    val id: String = "${userId}_${key}"
)