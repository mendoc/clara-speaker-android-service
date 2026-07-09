package pro.ongoua.claraspeaker

import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/**
 * Objet singleton pour fournir une instance unique de notre service Retrofit.
 */
object RetrofitClient {

    private const val BASE_URL = "https://api.elevenlabs.io/"

    // Timeouts généreux : le modèle eleven_v3 (traitement asynchrone) peut mettre
    // bien plus que les 10 s par défaut d'OkHttp à générer l'audio d'un résumé long.
    private val okHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .readTimeout(120, TimeUnit.SECONDS)
            .callTimeout(180, TimeUnit.SECONDS)
            .build()
    }

    // Création paresseuse (lazy) de l'instance Retrofit
    private val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    // Création paresseuse du service d'API
    val apiService: TtsApiService by lazy {
        retrofit.create(TtsApiService::class.java)
    }
}
