package database

import androidx.room.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL

object DatabaseMigrations {
    val MIGRATION_82_83 = object : Migration(82, 83) {
        override fun migrate(connection: SQLiteConnection) {
            connection.execSQL(
                """
                    ALTER TABLE ${AppRoomDatabase.TABLE_MESSAGE_REACTION}
                    ADD COLUMN sent_at TEXT
                """.trimIndent()
            )
        }
    }
}