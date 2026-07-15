package pro.ongoua.claraspeaker

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions

/**
 * Centralise la synchronisation du token FCM vers Firestore.
 *
 * Le document de l'utilisateur est indexé par son ID de compte Google
 * (GoogleSignInAccount.getId(), ex: "117449155396661763788"), dans la
 * collection [USERS_COLLECTION]. Chaque document contient aussi `refreshToken`
 * et `lastHistoryId`, écrits par le backend : les écritures ci-dessous utilisent
 * donc un merge pour ne mettre à jour QUE les champs de l'app (`fcmToken`, `email`).
 */
object FirestoreSync {

    private const val TAG = "FirestoreSync"
    private const val USERS_COLLECTION = "clara_speaker_users"
    private const val FIELD_FCM_TOKEN = "fcmToken"
    private const val FIELD_EMAIL = "email"

    /**
     * Synchronise le `fcmToken` et, si connu, l'`email` du compte après l'authentification.
     * L'e-mail provient de l'ID token vérifié par Firebase (voir [SessionStore.email]).
     */
    fun updateFcmToken(accountId: String, token: String, email: String? = null) {
        val fields = buildMap {
            put(FIELD_FCM_TOKEN, token)
            email?.let { put(FIELD_EMAIL, it) }
        }
        FirebaseFirestore.getInstance()
            .collection(USERS_COLLECTION)
            .document(accountId)
            .set(fields, SetOptions.merge())
            .addOnSuccessListener {
                Log.d(TAG, "fcmToken/email synchronisés dans Firestore pour le compte $accountId")
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Échec de la synchro fcmToken/email pour le compte $accountId", e)
            }
    }

    /**
     * Écrit le profil Google de l'utilisateur (prénom, nom, nom complet, photo, langue…)
     * pour que le backend puisse personnaliser les réponses du LLM. Les valeurs viennent
     * des claims de l'ID token vérifié par Firebase. Merge : n'écrase pas les champs backend.
     */
    fun updateProfile(accountId: String, profile: Map<String, Any>) {
        if (profile.isEmpty()) return
        FirebaseFirestore.getInstance()
            .collection(USERS_COLLECTION)
            .document(accountId)
            .set(profile, SetOptions.merge())
            .addOnSuccessListener {
                Log.d(TAG, "Profil synchronisé dans Firestore pour le compte $accountId : ${profile.keys}")
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Échec de la synchro du profil pour le compte $accountId", e)
            }
    }
}
