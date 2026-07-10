package pro.ongoua.claraspeaker

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SummaryViewModel(application: Application) : AndroidViewModel(application) {

    private val summaryDao = AppDatabase.getInstance(application).summaryDao()

    // Tous les résumés (joués et en attente), les plus récents d'abord.
    val summaries: LiveData<List<Summary>> = summaryDao.getAllSummaries()

    // État de lecture / synthèse, exposé à l'UI pour la surbrillance et le spinner.
    val playingId: LiveData<Int?> = AudioPlayerManager.playingId
    val loadingId: LiveData<Int?> = AudioPlayerManager.loadingId

    /**
     * Bouton lecture d'un résumé :
     * - déjà synthétisé (audio local) → lecture/arrêt immédiat ;
     * - en attente (pas encore d'audio) → synthèse ElevenLabs à la demande puis lecture.
     */
    fun onPlayClicked(summary: Summary) {
        if (summary.audioFilePath != null) {
            AudioPlayerManager.playOrStop(summary)
        } else {
            viewModelScope.launch(Dispatchers.IO) {
                AudioPlayerManager.synthesizeAndPlay(getApplication(), summary)
            }
        }
    }

    fun deleteSummary(summary: Summary) {
        viewModelScope.launch(Dispatchers.IO) {
            AudioPlayerManager.stopIfPlaying(summary.id)
            summary.audioFilePath?.let { path ->
                AudioPlayerManager.deleteAudioFile(path)
            }
            summaryDao.delete(summary)
        }
    }
}
