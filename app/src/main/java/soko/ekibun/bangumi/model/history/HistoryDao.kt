package soko.ekibun.bangumi.model.history

import androidx.room.*

@Dao
interface HistoryDao {

    @Query("SELECT * FROM history ORDER BY timestamp DESC LIMIT :limit OFFSET :offset")
    suspend fun getListOffset(limit: Int, offset: Int): List<History>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(history: History)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(history: List<History>)

    @Delete
    suspend fun delete(history: History)

    @Query("DELETE FROM history")
    suspend fun deleteAll()

}