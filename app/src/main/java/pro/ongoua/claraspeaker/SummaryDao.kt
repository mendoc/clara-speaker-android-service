package pro.ongoua.claraspeaker

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update

@Dao
interface SummaryDao {
    @Insert
    suspend fun insert(summary: Summary): Long

    @Update
    suspend fun update(summary: Summary)

    @Delete
    suspend fun delete(summary: Summary)

    @Query("SELECT * FROM summaries WHERE is_played = 0")
    suspend fun getUnplayed(): List<Summary>

    // Tous les résumés (joués et en attente), les plus récents d'abord.
    // LiveData permet à l'UI de se mettre à jour automatiquement.
    @Query("SELECT * FROM summaries ORDER BY created_at DESC")
    fun getAllSummaries(): LiveData<List<Summary>>
}