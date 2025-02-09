package database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import data.io.social.network.conversation.EmojiSelection
import database.AppRoomDatabase
import ui.conversation.components.emoji.EmojiUseCase.Companion.EMOJIS_HISTORY_LENGTH

/** Interface for communication with local Room database */
@Dao
interface EmojiSelectionDao {

    /** Returns emoji selections specific to a conversation, or all if no conversation is specified */
    @Query(
        "SELECT * FROM ${AppRoomDatabase.TABLE_EMOJI_SELECTION} " +
                "WHERE conversation_id == :conversationId " +
                "ORDER BY count DESC " +
                "LIMIT $EMOJIS_HISTORY_LENGTH"
    )
    suspend fun getSelections(conversationId: String): List<EmojiSelection>

    /** Returns emoji selections specific to a conversation, or all if no conversation is specified */
    @Query(
        "SELECT * FROM ${AppRoomDatabase.TABLE_EMOJI_SELECTION} " +
                "WHERE conversation_id IS NULL " +
                "ORDER BY count DESC "
    )
    suspend fun getGeneralSelections(): List<EmojiSelection>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSelection(selection: EmojiSelection)
}
