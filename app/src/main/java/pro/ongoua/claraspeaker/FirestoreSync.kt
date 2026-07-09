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
 * et `lastHistoryId`, écrits par le backend : l'écriture ci-dessous utilise donc
 * un merge pour ne mettre à jour QUE le champ `fcmToken`.
 */
object FirestoreSync {

    private const val TAG = "FirestoreSync"
    private const val USERS_COLLECTION = "clara_speaker_users"
    private const val FIELD_FCM_TOKEN = "fcmToken"

    fun updateFcmToken(accountId: String, token: String) {
        FirebaseFirestore.getInstance()
            .collection(USERS_COLLECTION)
            .document(accountId)
            .set(mapOf(FIELD_FCM_TOKEN to token), SetOptions.merge())
            .addOnSuccessListener {
                Log.d(TAG, "fcmToken synchronisé dans Firestore pour le compte $accountId")
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Échec de la synchro du fcmToken pour le compte $accountId", e)
            }
    }
}
