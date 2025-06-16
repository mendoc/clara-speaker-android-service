package pro.ongoua.claraspeaker

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Query

/**
 * Interface Retrofit pour l'API Google Cloud Text-to-Speech.
 */
interface TtsApiService {
    @POST("v1/text:synthesize")
    suspend fun synthesizeText(
        @Body body: TtsRequest,
        @Query("key") apiKey: String
    ): Response<TtsResponse>
}

// --- Classes de données pour la requête et la réponse ---

data class TtsRequest(val input: Input, val voice: Voice, val audioConfig: AudioConfig)
data class Input(val text: String)
data class Voice(val languageCode: String, val name: String)
data class AudioConfig(val audioEncoding: String)

data class TtsResponse(val audioContent: String)