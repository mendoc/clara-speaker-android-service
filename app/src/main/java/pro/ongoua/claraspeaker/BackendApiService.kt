package pro.ongoua.claraspeaker

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

/**
 * Interface Retrofit pour le backend Clara Speaker.
 */
interface BackendApiService {

    /**
     * Envoie le server auth code Google au backend, qui l'échange contre un
     * refresh token (accès hors-ligne Gmail) et l'enregistre sous l'ID de l'utilisateur.
     */
    @POST("oauth2/exchange")
    suspend fun exchangeAuthCode(@Body body: ExchangeCodeRequest): Response<Unit>
}

data class ExchangeCodeRequest(val code: String)
