package pro.ongoua.claraspeaker

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface SummaryDao {
    @Insert
    suspend fun insert(summary: Summary)

    @Query("SELECT * FROM summaries WHERE isPlayed = 0")
    suspend fun getUnplayed(): List<Summary>

    @Query("UPDATE summaries SET isPlayed = 1 WHERE id IN (:ids)")
    suspend fun markAsPlayed(ids: List<Int>)
}