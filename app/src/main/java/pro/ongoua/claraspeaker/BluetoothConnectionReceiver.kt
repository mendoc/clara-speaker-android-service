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
                        // On attend la fin complète (synthèse + lecture) de l'intro avant d'enchaîner.
                        AudioPlayerManager.synthesizeAndPlayAwait(context, introSummary)

                        // On lit chaque résumé en attente, en attendant la fin de chacun
                        // avant de passer au suivant (sinon l'un interromprait l'autre).
                        for (summary in unplayedSummaries) {
                            Log.d(TAG, "Lecture du résumé ID: ${summary.id}")
                            AudioPlayerManager.synthesizeAndPlayAwait(context, summary)
                            // Courte pause entre deux résumés.
                            delay(800L)
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