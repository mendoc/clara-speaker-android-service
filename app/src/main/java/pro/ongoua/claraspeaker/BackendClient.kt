package pro.ongoua.claraspeaker

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

/**
 * Client Retrofit unique pour le backend Clara Speaker.
 */
object BackendClient {

    // Backend Clara Speaker (fonctions Netlify). L'endpoint appelé est "oauth2/exchange"
    // → https://clara-speaker.netlify.app/oauth2/exchange
    private const val BASE_URL = "https://clara-speaker.netlify.app/"

    private val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    val apiService: BackendApiService by lazy {
        retrofit.create(BackendApiService::class.java)
    }
}
