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
    private const val KEY_GMAIL_CONNECTED = "gmail_connected"

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun save(context: Context, accountId: String, email: String?) {
        val editor = prefs(context).edit()
            .putString(KEY_ACCOUNT_ID, accountId)
            .putString(KEY_EMAIL, email)
        // Si on change de compte, l'état « Gmail connecté » ne vaut plus.
        if (accountId != this.accountId(context)) {
            editor.remove(KEY_GMAIL_CONNECTED)
        }
        editor.apply()
    }

    fun accountId(context: Context): String? = prefs(context).getString(KEY_ACCOUNT_ID, null)

    fun email(context: Context): String? = prefs(context).getString(KEY_EMAIL, null)

    fun isGmailConnected(context: Context): Boolean =
        prefs(context).getBoolean(KEY_GMAIL_CONNECTED, false)

    fun setGmailConnected(context: Context, connected: Boolean) {
        prefs(context).edit().putBoolean(KEY_GMAIL_CONNECTED, connected).apply()
    }

    fun clear(context: Context) {
        prefs(context).edit().clear().apply()
    }
}
