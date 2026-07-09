package pro.ongoua.claraspeaker

import android.content.Context

/**
 * Persiste l'identité du compte connecté.
 *
 * Contrairement à l'ancienne API GoogleSignIn, Credential Manager ne conserve pas
 * de « dernier compte connecté » ; on stocke donc nous-mêmes l'ID de compte Google
 * (le claim `sub` de l'ID token, qui sert d'ID de document Firestore) et l'e-mail.
 */
object SessionStore {

    private const val PREFS = "clara_session"
    private const val KEY_ACCOUNT_ID = "account_id"
    private const val KEY_EMAIL = "email"

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun save(context: Context, accountId: String, email: String?) {
        prefs(context).edit()
            .putString(KEY_ACCOUNT_ID, accountId)
            .putString(KEY_EMAIL, email)
            .apply()
    }

    fun accountId(context: Context): String? = prefs(context).getString(KEY_ACCOUNT_ID, null)

    fun email(context: Context): String? = prefs(context).getString(KEY_EMAIL, null)

    fun clear(context: Context) {
        prefs(context).edit().clear().apply()
    }
}
