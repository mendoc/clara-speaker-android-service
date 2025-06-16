package pro.ongoua.claraspeaker

import android.media.AudioManager
import android.util.Log
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MyFirebaseMessagingService : FirebaseMessagingService() {

    private val TAG = "ClaraSpeaker@FirebaseService"

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        Log.d(TAG, "Message reçu de : ${remoteMessage.from}")

        remoteMessage.data["summaryText"]?.let { summary ->
            Log.d(TAG, "Résumé reçu : '$summary'")

            CoroutineScope(Dispatchers.IO).launch {
                if (isBluetoothHeadsetConnected()) {
                    Log.d(TAG, "Casque Bluetooth détecté. Délégation à AudioPlayerManager.")
                    // On délègue tout le travail au manager
                    AudioPlayerManager.synthesizeAndPlay(applicationContext, summary)
                } else {
                    Log.d(TAG, "Aucun casque connecté. Sauvegarde du résumé en base de données.")
                    val database = AppDatabase.getInstance(applicationContext)
                    database.summaryDao().insert(Summary(text = summary, isPlayed = false))
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