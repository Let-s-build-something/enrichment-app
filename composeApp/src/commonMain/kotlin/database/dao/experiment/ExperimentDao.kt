package database.dao.experiment

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import data.io.experiment.ExperimentIO
import database.AppRoomDatabase

/** Interface for communication with local Room database */
@Dao
interface ExperimentDao {

    @Query(
        """
            SELECT * FROM ${AppRoomDatabase.TABLE_EXPERIMENT}
            WHERE owner = :owner
            OR owner IS NULL
            ORDER BY activate_until DESC, created_at DESC
        """
    )
    suspend fun getAll(owner: String?): List<ExperimentIO>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: ExperimentIO)

    @Query("DELETE FROM ${AppRoomDatabase.TABLE_EXPERIMENT}")
    suspend fun removeAll()

    @Query("""
        DELETE FROM ${AppRoomDatabase.TABLE_EXPERIMENT}
        WHERE uid = :uid
        """)
    suspend fun remove(uid: String)
}