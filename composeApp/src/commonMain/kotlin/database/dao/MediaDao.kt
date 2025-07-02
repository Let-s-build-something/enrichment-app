package database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import data.io.social.network.conversation.message.MediaIO
import database.AppRoomDatabase

@Dao
interface MediaDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReplace(item: MediaIO)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<MediaIO>)

    @Query("""
        SELECT * FROM ${AppRoomDatabase.TABLE_MEDIA}
        WHERE url = :url
        LIMIT 1
        """)
    suspend fun getByUrl(url: String): MediaIO?

    @Query("""
        SELECT * FROM ${AppRoomDatabase.TABLE_MEDIA}
        WHERE id = :id
        LIMIT 1
        """)
    suspend fun get(id: Long): MediaIO?

    @Query("""
        SELECT * FROM ${AppRoomDatabase.TABLE_MEDIA}
        WHERE message_id = :messageId
        """)
    suspend fun getAllByMessageId(messageId: String): List<MediaIO>

    @Query("""
        DELETE FROM ${AppRoomDatabase.TABLE_MEDIA}
        WHERE message_id = :messageId
        """)
    suspend fun removeAllOf(messageId: String)

    @Query("""
        SELECT * FROM ${AppRoomDatabase.TABLE_MEDIA}
        WHERE id IN (:idList)
        """)
    suspend fun getAllById(idList: List<String>): List<MediaIO>
}
