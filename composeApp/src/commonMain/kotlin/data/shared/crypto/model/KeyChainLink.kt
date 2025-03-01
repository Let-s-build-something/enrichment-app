package data.shared.crypto.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import database.AppRoomDatabase
import kotlinx.serialization.Serializable
import net.folivo.trixnity.core.model.UserId
import net.folivo.trixnity.core.model.keys.Key
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
@Entity(AppRoomDatabase.TABLE_KEY_CHAIN_LINK)
@Serializable
data class KeyChainLink(
    @ColumnInfo("signing_user_id")
    val signingUserId: UserId,
    @ColumnInfo("signing_key")
    val signingKey: Key.Ed25519Key,
    @ColumnInfo("signed_user_id")
    val signedUserId: UserId,
    @ColumnInfo("signed_key")
    val signedKey: Key.Ed25519Key,

    @PrimaryKey
    val id: String = Uuid.random().toString(),
)