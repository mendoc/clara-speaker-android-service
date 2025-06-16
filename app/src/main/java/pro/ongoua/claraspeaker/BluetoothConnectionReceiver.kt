package pro.ongoua.claraspeaker

import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BluetoothConnectionReceiver : BroadcastReceiver() {

    private val TAG = "ClaraSpeaker@BluetoothReceiver"

    override fun onReceive(context: Context, intent: Intent) {
        Log.d("Bluetooth_DEBUG", "!!! onReceive a été DÉCLENCHÉ !!! Action reçue: ${intent.action}")

        // On vérifie si l'action est bien une connexion d'un appareil Bluetooth
        if (intent.action == BluetoothDevice.ACTION_ACL_CONNECTED) {
            Log.d(TAG, "Un appareil Bluetooth a été connecté.")

            val pendingResult = goAsync() // Permet à la coroutine de terminer
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val database = AppDatabase.getInstance(context.applicationContext)
                    val unplayedSummaries = database.summaryDao().getUnplayed()

                    if (unplayedSummaries.isNotEmpty()) {
                        Log.d(TAG, "${unplayedSummaries.size} résumé(s) non lu(s) trouvé(s).")

                        // On crée un rapport global
                        val reportHeader =
                            "Vous avez manqué ${unplayedSummaries.size} résumé(s). Les voici. "
                        val fullReport =
                            unplayedSummaries.joinToString(separator = ". Prochain résumé. ") { it.text }
                        val consolidatedSummary = reportHeader + fullReport

                        // On lance la synthèse vocale
                        AudioPlayerManager.synthesizeAndPlay(context, consolidatedSummary)

                        // On marque les résumés comme lus
                        val idsToUpdate = unplayedSummaries.map { it.id }
                        database.summaryDao().markAsPlayed(idsToUpdate)
                        Log.d(TAG, "Les résumés ont été marqués comme lus.")
                    } else {
                        Log.d(TAG, "Aucun résumé non lu à jouer.")
                    }
                } finally {
                    pendingResult.finish() // Indique que le travail en arrière-plan est terminé
                }
            }
        }
    }
}
