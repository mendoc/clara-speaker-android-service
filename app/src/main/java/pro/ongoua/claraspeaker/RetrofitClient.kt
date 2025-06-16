package pro.ongoua.claraspeaker

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

/**
 * Objet singleton pour fournir une instance unique de notre service Retrofit.
 */
object RetrofitClient {

    private const val BASE_URL = "https://texttospeech.googleapis.com/"

    // Création paresseuse (lazy) de l'instance Retrofit
    private val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    // Création paresseuse du service d'API
    val apiService: TtsApiService by lazy {
        retrofit.create(TtsApiService::class.java)
    }
}