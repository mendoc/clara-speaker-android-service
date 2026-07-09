package pro.ongoua.claraspeaker

import android.content.Context
import android.media.MediaPlayer
import android.util.Log
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

/**
 * Objet singleton pour gérer toute la logique de synthèse vocale et de lecture audio.
 * Il peut être appelé de n'importe où dans l'application.
 */
object AudioPlayerManager {

    private const val TAG = "ClaraSpeaker@AudioPlayerManager"
    private var currentMediaPlayer: MediaPlayer? = null

    // Voix ElevenLabs : « David - Gruff Cowboy », modèle Eleven v3.
    private const val VOICE_ID = "OYWwCdDHouzDwiZJWOOu"
    private const val VOICE_NAME = "David - Gruff Cowboy"
    private const val MODEL_ID = "eleven_v3"

    /**
     * Joue ou arrête un résumé.
     * Si le résumé est déjà en lecture, il l'arrête.
     * Sinon, il lance la lecture depuis le fichier local.
     */
    fun playOrStop(summary: Summary) {
        // On arrête la lecture en cours
        stopPlaying()

        // Si le résumé cliqué n'était pas celui en cours, on lance la nouvelle lecture
        summary.audioFilePath?.let { path ->
            Log.d(TAG, "Lancement de la lecture depuis le fichier : $path")
            currentMediaPlayer = MediaPlayer().apply {
                try {
                    setDataSource(path)
                    prepare()
                    start()
                    setOnCompletionListener { stopPlaying() }
                } catch (e: IOException) {
                    Log.e(TAG, "Erreur lors de la préparation du MediaPlayer", e)
                    stopPlaying()
                }
            }
        }
    }

    /**
     * Stoppe toute lecture en cours et libère les ressources.
     */
    private fun stopPlaying() {
        currentMediaPlayer?.stop()
        currentMediaPlayer?.release()
        currentMediaPlayer = null
        Log.d(TAG, "MediaPlayer stoppé et libéré.")
    }

    /**
     * La fonction publique principale. Elle prend un texte et s'occupe de tout.
     */
    suspend fun synthesizeAndPlay(context: Context, summary: Summary) {
        val apiKey = BuildConfig.ELEVENLABS_API_KEY
        if (apiKey.isNullOrEmpty() || apiKey == "null") {
            Log.e(TAG, "ERREUR : La clé API ElevenLabs n'est pas configurée.")
            return
        }

        val apiService = RetrofitClient.apiService
        val request = TtsRequest(text = summary.text, modelId = MODEL_ID)

        try {
            val response = apiService.synthesizeText(VOICE_ID, apiKey, request)
            if (response.isSuccessful && response.body() != null) {
                val audioData = response.body()!!.bytes()
                val audioFile = saveAudioToFile(context, audioData, summary.id)
                if (audioFile != null) {
                    summary.isPlayed = true
                    summary.voiceModel = VOICE_NAME
                    summary.audioFilePath = audioFile.absolutePath

                    val dao = AppDatabase.getInstance(context).summaryDao()
                    dao.update(summary)
                    Log.d(TAG, "Résumé ${summary.id} mis à jour dans la DB avec le chemin du fichier.")

                    // On lance la lecture du fichier qui vient d'être sauvegardé
                    playOrStop(summary)
                }
            } else {
                Log.e(TAG, "Erreur de l'API TTS: Code ${response.code()} - ${response.errorBody()?.string()}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception lors de l'appel à l'API TTS", e)
        }
    }

    /**
     * Sauvegarde les octets audio bruts (mp3) dans un fichier dans le stockage interne.
     * @return Le fichier sauvegardé, ou null en cas d'erreur.
     */
    private fun saveAudioToFile(context: Context, audioData: ByteArray, summaryId: Int): File? {
        return try {
            val file = File(context.filesDir, "summary_$summaryId.mp3")
            FileOutputStream(file).use { fos -> fos.write(audioData) }
            Log.d(TAG, "Fichier audio sauvegardé dans : ${file.absolutePath}")
            file
        } catch (e: IOException) {
            Log.e(TAG, "Erreur lors de la sauvegarde du fichier audio", e)
            null
        }
    }

    /**
     * Supprime un fichier audio du stockage interne.
     * @param filePath Le chemin absolu du fichier à supprimer.
     * @return true si le fichier a été supprimé avec succès, false sinon.
     */
    fun deleteAudioFile(filePath: String): Boolean {
        return try {
            val file = File(filePath)
            if (file.exists()) {
                val deleted = file.delete()
                if (deleted) {
                    Log.d(TAG, "Fichier audio supprimé : $filePath")
                } else {
                    Log.w(TAG, "Impossible de supprimer le fichier audio : $filePath")
                }
                deleted
            } else {
                Log.d(TAG, "Le fichier audio n'existe pas : $filePath")
                true // Considérer comme supprimé si n'existe pas
            }
        } catch (e: Exception) {
            Log.e(TAG, "Erreur lors de la suppression du fichier audio : $filePath", e)
            false
        }
    }
}
