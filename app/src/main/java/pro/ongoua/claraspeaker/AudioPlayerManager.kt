package pro.ongoua.claraspeaker

import android.content.Context
import android.media.MediaPlayer
import android.util.Base64
import android.util.Log
import java.io.File
import java.io.FileOutputStream

/**
 * Objet singleton pour gérer toute la logique de synthèse vocale et de lecture audio.
 * Il peut être appelé de n'importe où dans l'application.
 */
object AudioPlayerManager {

    private const val TAG = "ClaraSpeaker@AudioPlayerManager"

    private val frenchVoices = listOf(
        "fr-FR-Chirp-HD-D", "fr-FR-Chirp-HD-F", "fr-FR-Chirp-HD-O",
        "fr-FR-Chirp3-HD-Aoede", "fr-FR-Chirp3-HD-Charon", "fr-FR-Chirp3-HD-Fenrir",
        "fr-FR-Chirp3-HD-Kore", "fr-FR-Chirp3-HD-Leda", "fr-FR-Chirp3-HD-Orus",
        "fr-FR-Chirp3-HD-Puck", "fr-FR-Chirp3-HD-Zephyr", "fr-FR-Neural2-F",
        "fr-FR-Neural2-G", "fr-FR-Polyglot-1", "fr-FR-Standard-A",
        "fr-FR-Standard-B", "fr-FR-Standard-C", "fr-FR-Standard-D",
        "fr-FR-Standard-E", "fr-FR-Standard-F", "fr-FR-Standard-G",
        "fr-FR-Studio-A", "fr-FR-Studio-D", "fr-FR-Wavenet-A",
        "fr-FR-Wavenet-B", "fr-FR-Wavenet-C", "fr-FR-Wavenet-D",
        "fr-FR-Wavenet-E", "fr-FR-Wavenet-F", "fr-FR-Wavenet-G"
    )

    /**
     * La fonction publique principale. Elle prend un texte et s'occupe de tout.
     */
    suspend fun synthesizeAndPlay(context: Context, text: String) {
        val apiKey = BuildConfig.GOOGLE_TTS_API_KEY
        if (apiKey.isNullOrEmpty() || apiKey == "null") {
            Log.e(TAG, "ERREUR : La clé API pour Google TTS n'est pas configurée.")
            return
        }

        val apiService = RetrofitClient.apiService
        val chosenVoice = frenchVoices.random()
        Log.d(TAG, "Voix choisie au hasard : $chosenVoice")

        val request = TtsRequest(
            input = Input(text = text),
            voice = Voice(languageCode = "fr-FR", name = chosenVoice),
            audioConfig = AudioConfig(audioEncoding = "MP3")
        )

        try {
            val response = apiService.synthesizeText(request, apiKey)
            if (response.isSuccessful && response.body() != null) {
                Log.d(TAG, "Réponse de l'API TTS reçue avec succès.")
                playAudioFromBase64(context.applicationContext, response.body()!!.audioContent)
            } else {
                Log.e(TAG, "Erreur de l'API TTS: Code ${response.code()} - ${response.errorBody()?.string()}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception lors de l'appel à l'API TTS", e)
        }
    }

    /**
     * Fonction privée pour jouer le son à partir des données Base64.
     */
    private fun playAudioFromBase64(context: Context, base64Audio: String) {
        try {
            val audioData = Base64.decode(base64Audio, Base64.DEFAULT)
            val tempMp3 = File.createTempFile("summary_audio", "mp3", context.cacheDir)
            tempMp3.deleteOnExit()

            FileOutputStream(tempMp3).use { fos -> fos.write(audioData) }

            val mediaPlayer = MediaPlayer().apply {
                setDataSource(tempMp3.absolutePath)
                setOnCompletionListener {
                    Log.d(TAG, "Lecture audio terminée.")
                    it.release()
                }
                prepare()
                start()
            }
            Log.d(TAG, "Lecture audio démarrée.")

        } catch (e: Exception) {
            Log.e(TAG, "Erreur lors de la lecture audio", e)
        }
    }
}