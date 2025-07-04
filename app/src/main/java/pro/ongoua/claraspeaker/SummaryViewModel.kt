package pro.ongoua.claraspeaker

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch

class SummaryViewModel(application: Application) : AndroidViewModel(application) {

    private val summaryDao = AppDatabase.getInstance(application).summaryDao()

    // LiveData qui expose la liste des 3 derniers résumés
    val latestSummaries: LiveData<List<Summary>> = summaryDao.getLatestPlayedSummaries()

    fun onSummaryClicked(summary: Summary) {
        AudioPlayerManager.playOrStop(summary)
    }

    fun deleteSummary(summary: Summary) {
        viewModelScope.launch {
            summary.audioFilePath?.let { path ->
                AudioPlayerManager.deleteAudioFile(path)
            }
            summaryDao.delete(summary)
        }
    }
}