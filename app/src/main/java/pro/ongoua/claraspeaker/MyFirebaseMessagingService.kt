package pro.ongoua.claraspeaker

import android.media.AudioManager
import android.util.Log
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MyFirebaseMessagingService : FirebaseMessagingService() {

    private val TAG = "FirebaseService"

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        Log.d(TAG, "Message reçu de : ${remoteMessage.from}")

        remoteMessage.data["summaryText"]?.let { summaryText ->
            Log.d(TAG, "Résumé reçu : '$summaryText'")

            // On lance une coroutine pour toutes les opérations (DB, lecture, etc.)
            CoroutineScope(Dispatchers.IO).launch {
                val database = AppDatabase.getInstance(applicationContext)

                // 1. On crée et on insère SYSTÉMATIQUEMENT le résumé dans la DB.
                // L'état initial est "non joué".
                val newSummary = Summary(
                    text = summaryText,
                    isPlayed = false, // On spécifie la valeur explicitement
                    createdAt = System.currentTimeMillis(), // On spécifie la valeur explicitement
                    voiceModel = null, // Valeur initiale
                    audioFilePath = null // Valeur initiale
                )
                val newId = database.summaryDao().insert(newSummary)

                // On crée un objet complet avec l'ID retourné par la base de données.
                val summaryWithId = newSummary.copy(id = newId.toInt())
                Log.d(TAG, "Résumé sauvegardé dans la DB avec l'ID: ${summaryWithId.id}")

                // 2. On vérifie si les écouteurs sont connectés.
                if (isBluetoothHeadsetConnected()) {
                    Log.d(TAG, "Casque Bluetooth détecté. Lancement de la lecture immédiate.")
                    // 3. On délègue la lecture ET la mise à jour au manager
                    // en lui passant l'objet complet.
                    AudioPlayerManager.synthesizeAndPlay(applicationContext, summaryWithId)
                } else {
                    Log.d(TAG, "Aucun casque connecté. Le résumé reste en attente dans la DB.")
                    // On n'a rien d'autre à faire. Le résumé est déjà sauvegardé.
                }
            }
        }
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d(TAG, "Nouveau token FCM généré : $token")
        sendTokenToServer(token)
    }

    private fun isBluetoothHeadsetConnected(): Boolean {
        val audioManager = applicationContext.getSystemService(AUDIO_SERVICE) as AudioManager
        return audioManager.isBluetoothA2dpOn
    }

    private fun sendTokenToServer(token: String) {
        Log.d(TAG, "TODO: Envoyer ce token au serveur: $token")
    }
}