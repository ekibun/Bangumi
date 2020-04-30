package soko.ekibun.bangumi.model.history

import androidx.room.*
import io.reactivex.Completable
import io.reactivex.Single

@Dao
interface HistoryDao {

    @Query("SELECT * FROM history ORDER BY timestamp DESC LIMIT :limit OFFSET :offset")
    fun getListOffset(limit: Int, offset: Int): Single<List<History>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(history: History): Completable

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(history: List<History>): Completable

    @Delete
    fun delete(history: History): Completable

    @Query("DELETE FROM history")
    fun deleteAll(): Completable

}