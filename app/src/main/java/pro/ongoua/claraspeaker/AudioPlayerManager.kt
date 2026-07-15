package pro.ongoua.claraspeaker

import android.content.Context
import android.media.MediaPlayer
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import kotlin.coroutines.resume

/**
 * Objet singleton pour gérer toute la logique de synthèse vocale et de lecture audio.
 * Il peut être appelé de n'importe où dans l'application.
 */
object AudioPlayerManager {

    private const val TAG = "ClaraSpeaker@AudioPlayerManager"
    private var currentMediaPlayer: MediaPlayer? = null

    // Continuation à reprendre quand la lecture séquentielle en cours se termine (ou est arrêtée).
    private var playbackContinuation: CancellableContinuation<Unit>? = null

    // Scope dédié à la lecture séquentielle : indépendant du composant déclencheur
    // (receiver Bluetooth ou service FCM), pour survivre à la fin de leur callback.
    private val sequenceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // Job de la lecture séquentielle en cours (null si aucune).
    private var sequenceJob: Job? = null

    // Garde-fou synchrone contre le démarrage de deux séquences concurrentes
    // (ex. connexion Bluetooth ET réception FCM quasi simultanées).
    @Volatile
    private var sequenceActive = false

    // Indique si une lecture séquentielle est en cours ; observé par l'UI pour
    // afficher le bouton « Stopper la lecture ».
    private val _sequencePlaying = MutableLiveData(false)
    val sequencePlaying: LiveData<Boolean> = _sequencePlaying

    // Voix ElevenLabs : « David - Gruff Cowboy », modèle Eleven v3.
    private const val VOICE_ID = "OYWwCdDHouzDwiZJWOOu"
    private const val VOICE_NAME = "David - Gruff Cowboy"
    private const val MODEL_ID = "eleven_v3"

    // ID du résumé en cours de lecture (null si aucun), observé par l'UI pour la surbrillance.
    private val _playingId = MutableLiveData<Int?>(null)
    val playingId: LiveData<Int?> = _playingId

    // ID du résumé en cours de synthèse (null si aucun), pour afficher un indicateur de chargement.
    private val _loadingId = MutableLiveData<Int?>(null)
    val loadingId: LiveData<Int?> = _loadingId

    /**
     * Joue ou arrête un résumé.
     * Si le résumé est déjà en lecture, il l'arrête.
     * Sinon, il lance la lecture depuis le fichier local.
     */
    fun playOrStop(summary: Summary) {
        // Toggle : si ce résumé est déjà en cours de lecture, on ne fait que l'arrêter.
        if (_playingId.value == summary.id) {
            stopPlaying()
            return
        }

        // On arrête toute lecture en cours avant de lancer la nouvelle.
        stopPlaying()

        val path = summary.audioFilePath ?: return
        Log.d(TAG, "Lancement de la lecture depuis le fichier : $path")
        currentMediaPlayer = MediaPlayer().apply {
            try {
                setDataSource(path)
                prepare()
                start()
                _playingId.postValue(summary.id)
                setOnCompletionListener { stopPlaying() }
            } catch (e: IOException) {
                Log.e(TAG, "Erreur lors de la préparation du MediaPlayer", e)
                stopPlaying()
            }
        }
    }

    /**
     * Arrête la lecture uniquement si c'est [id] qui joue (ex. avant suppression).
     */
    fun stopIfPlaying(id: Int) {
        if (_playingId.value == id) stopPlaying()
    }

    /**
     * Démarre la lecture séquentielle de tous les résumés non lus (intro puis chacun),
     * si aucune séquence n'est déjà en cours. Point d'entrée UNIQUE, appelé aussi bien
     * à la connexion Bluetooth qu'à la réception d'un résumé quand le casque est déjà
     * connecté. La boucle re-interroge la base entre deux passes pour jouer aussi les
     * résumés arrivés PENDANT la séquence.
     */
    @Synchronized
    fun startSequence(context: Context) {
        if (sequenceActive) {
            Log.d(TAG, "Une séquence est déjà en cours ; demande ignorée.")
            return
        }
        sequenceActive = true
        _sequencePlaying.postValue(true)
        val appContext = context.applicationContext
        sequenceJob = sequenceScope.launch {
            try {
                var batch = AppDatabase.getInstance(appContext).summaryDao().getUnplayed()
                if (batch.isEmpty()) {
                    Log.d(TAG, "Aucun résumé non lu à jouer.")
                    return@launch
                }

                // Intro annonçant le nombre de résumés — uniquement s'il y en a plusieurs.
                // Pour un seul résumé, on le joue directement, sans préambule.
                if (batch.size > 1) {
                    val intro = Summary(
                        text = "Vous avez ${batch.size} résumés en attente. C'est parti.",
                        isPlayed = true, // objet temporaire, non sauvegardé
                        createdAt = System.currentTimeMillis(),
                        voiceModel = null,
                        audioFilePath = null
                    )
                    synthesizeAndPlayAwait(appContext, intro)
                }

                // On lit chaque résumé en attendant la fin de chacun. Entre deux passes,
                // on re-interroge la base : un résumé reçu pendant la lecture est ainsi
                // enchaîné sans attendre une nouvelle connexion Bluetooth.
                while (batch.isNotEmpty()) {
                    for (summary in batch) {
                        Log.d(TAG, "Lecture du résumé ID: ${summary.id}")
                        synthesizeAndPlayAwait(appContext, summary)
                        delay(800L)
                    }
                    batch = AppDatabase.getInstance(appContext).summaryDao().getUnplayed()
                }
            } finally {
                onSequenceEnded()
            }
        }
    }

    /** Réinitialise l'état à la fin de la séquence (fin normale OU annulation). */
    @Synchronized
    private fun onSequenceEnded() {
        sequenceActive = false
        sequenceJob = null
        _sequencePlaying.postValue(false)
    }

    /**
     * Stoppe TOUTE la séquence de lecture : annule la boucle (le résumé suivant ne
     * démarrera pas) et arrête la lecture en cours. C'est le contrôle qui permet
     * d'interrompre une longue série de résumés d'un seul geste.
     */
    @Synchronized
    fun stopSequence() {
        sequenceJob?.cancel()
        sequenceJob = null
        sequenceActive = false
        stopPlaying()
        _sequencePlaying.postValue(false)
    }

    /**
     * Stoppe toute lecture en cours et libère les ressources.
     * Reprend aussi la continuation d'une éventuelle lecture séquentielle en attente.
     */
    private fun stopPlaying() {
        currentMediaPlayer?.stop()
        currentMediaPlayer?.release()
        currentMediaPlayer = null
        _playingId.postValue(null)
        playbackContinuation?.let { cont ->
            playbackContinuation = null
            if (cont.isActive) cont.resume(Unit)
        }
        Log.d(TAG, "MediaPlayer stoppé et libéré.")
    }

    /**
     * Synthétise le résumé via ElevenLabs, sauvegarde le .mp3 et met à jour la DB.
     * @return true si l'audio est prêt (summary.audioFilePath renseigné), false sinon.
     */
    private suspend fun synthesize(context: Context, summary: Summary): Boolean {
        val apiKey = BuildConfig.ELEVENLABS_API_KEY
        if (apiKey.isNullOrEmpty() || apiKey == "null") {
            Log.e(TAG, "ERREUR : La clé API ElevenLabs n'est pas configurée.")
            return false
        }

        val request = TtsRequest(text = summary.text, modelId = MODEL_ID)
        _loadingId.postValue(summary.id)
        try {
            val response = RetrofitClient.apiService.synthesizeText(VOICE_ID, apiKey, request)
            if (response.isSuccessful && response.body() != null) {
                val audioData = response.body()!!.bytes()
                val audioFile = saveAudioToFile(context, audioData, summary.id) ?: return false
                summary.isPlayed = true
                summary.voiceModel = VOICE_NAME
                summary.audioFilePath = audioFile.absolutePath
                AppDatabase.getInstance(context).summaryDao().update(summary)
                Log.d(TAG, "Résumé ${summary.id} mis à jour dans la DB avec le chemin du fichier.")
                return true
            } else {
                Log.e(TAG, "Erreur de l'API TTS: Code ${response.code()} - ${response.errorBody()?.string()}")
                return false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception lors de l'appel à l'API TTS", e)
            return false
        } finally {
            _loadingId.postValue(null)
        }
    }

    /**
     * Synthétise puis lance la lecture sans attendre la fin (usage UI / lecture immédiate).
     */
    suspend fun synthesizeAndPlay(context: Context, summary: Summary) {
        if (synthesize(context, summary)) playOrStop(summary)
    }

    /**
     * Synthétise, lit, et **suspend jusqu'à la fin de la lecture**.
     * Utilisé par la lecture séquentielle (Bluetooth) pour ne pas enchaîner
     * le résumé suivant avant que le précédent soit terminé.
     */
    suspend fun synthesizeAndPlayAwait(context: Context, summary: Summary) {
        if (synthesize(context, summary)) playAndAwait(summary)
    }

    /**
     * Lit le fichier audio du résumé et suspend jusqu'à la fin (ou l'arrêt) de la lecture.
     */
    private suspend fun playAndAwait(summary: Summary): Unit =
        suspendCancellableCoroutine { cont ->
            stopPlaying()

            val path = summary.audioFilePath
            if (path == null) {
                cont.resume(Unit)
                return@suspendCancellableCoroutine
            }

            playbackContinuation = cont
            Log.d(TAG, "Lancement de la lecture depuis le fichier : $path")
            currentMediaPlayer = MediaPlayer().apply {
                try {
                    setDataSource(path)
                    prepare()
                    start()
                    _playingId.postValue(summary.id)
                    setOnCompletionListener { stopPlaying() }
                } catch (e: IOException) {
                    Log.e(TAG, "Erreur lors de la préparation du MediaPlayer", e)
                    stopPlaying()
                }
            }

            cont.invokeOnCancellation { stopPlaying() }
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
