package pro.ongoua.claraspeaker

import com.google.gson.annotations.SerializedName
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path

/**
 * Interface Retrofit pour l'API Text-to-Speech d'ElevenLabs.
 *
 * La voix est passée dans l'URL (voiceId), la clé dans le header `xi-api-key`,
 * et la réponse est l'audio **brut** (mp3), pas du JSON encodé en Base64.
 */
interface TtsApiService {
    @POST("v1/text-to-speech/{voiceId}")
    suspend fun synthesizeText(
        @Path("voiceId") voiceId: String,
        @Header("xi-api-key") apiKey: String,
        @Body body: TtsRequest
    ): Response<ResponseBody>
}

data class TtsRequest(
    val text: String,
    @SerializedName("model_id") val modelId: String
)
