package pro.ongoua.claraspeaker

import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class BluetoothConnectionReceiver : BroadcastReceiver() {

    private val TAG = "BluetoothReceiver"

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == BluetoothDevice.ACTION_ACL_CONNECTED) {
            Log.d(TAG, "Un appareil Bluetooth a été connecté.")

            val pendingResult = goAsync()
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val database = AppDatabase.getInstance(context.applicationContext)
                    val unplayedSummaries = database.summaryDao().getUnplayed()

                    if (unplayedSummaries.isNotEmpty()) {
                        Log.d(TAG, "${unplayedSummaries.size} résumé(s) non lu(s) trouvé(s). Lancement de la lecture séquentielle.")

                        // On informe l'utilisateur qu'un récapitulatif commence
                        val intro = "Vous avez ${unplayedSummaries.size} résumés en attente. C'est parti."
                        // On crée un objet Summary temporaire juste pour l'intro, il ne sera pas sauvegardé.
                        val introSummary = Summary(
                            text = intro,
                            isPlayed = true, // On le considère joué car il n'est pas sauvegardé
                            createdAt = System.currentTimeMillis(),
                            voiceModel = null,
                            audioFilePath = null
                        )
                        AudioPlayerManager.synthesizeAndPlay(context, introSummary)

                        // On attend un peu pour que l'intro se termine (à ajuster selon vos tests)
                        delay(5000L)

                        // On boucle sur chaque résumé en attente pour le lire
                        for (summary in unplayedSummaries) {
                            Log.d(TAG, "Lecture du résumé ID: ${summary.id}")
                            AudioPlayerManager.synthesizeAndPlay(context, summary)
                            // On ajoute un délai pour laisser le temps à la lecture de se faire et pour marquer une pause
                            // Ce délai est à ajuster en fonction de la longueur moyenne de vos résumés.
                            delay(10000L)
                        }

                    } else {
                        Log.d(TAG, "Aucun résumé non lu à jouer.")
                    }
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }
}