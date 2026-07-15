package pro.ongoua.claraspeaker

import android.Manifest
import android.accounts.Account
import android.content.pm.PackageManager
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Base64
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetCredentialResponse
import androidx.credentials.exceptions.GetCredentialException
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.auth.api.identity.AuthorizationRequest
import com.google.android.gms.auth.api.identity.AuthorizationResult
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.Scope
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import org.json.JSONObject

class MainActivity : AppCompatActivity() {

    private companion object {
        const val TAG = "ClaraSpeaker@MainActivity"
    }

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            if (isGranted) {
                Log.d("Permissions", "Permission BLUETOOTH_CONNECT accordée.")
            } else {
                Log.w("Permissions", "Permission BLUETOOTH_CONNECT refusée.")
                // Vous pouvez afficher un message à l'utilisateur ici pour expliquer pourquoi la permission est nécessaire.
            }
        }

    private val summaryViewModel: SummaryViewModel by viewModels()

    private lateinit var authView: View
    private lateinit var contentView: View
    private lateinit var signInButton: Button
    private lateinit var authErrorView: TextView
    private lateinit var accountView: TextView
    private lateinit var signOutButton: Button
    private lateinit var emptyView: TextView
    private lateinit var recyclerView: RecyclerView
    private lateinit var summaryAdapter: SummaryAdapter
    private lateinit var gmailStatusView: TextView
    private lateinit var gmailConnectButton: Button

    private val credentialManager by lazy { CredentialManager.create(this) }
    private val firebaseAuth by lazy { FirebaseAuth.getInstance() }

    // Résultat de l'écran de consentement Gmail (server auth code via l'Authorization API).
    private val authorizationLauncher =
        registerForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { activityResult ->
            try {
                val result = Identity.getAuthorizationClient(this)
                    .getAuthorizationResultFromIntent(activityResult.data)
                handleAuthorizationResult(result)
            } catch (e: ApiException) {
                Log.e(TAG, "Consentement Gmail échoué ou annulé", e)
                Toast.makeText(this, R.string.gmail_connect_failed, Toast.LENGTH_SHORT).show()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        authView = findViewById(R.id.authView)
        contentView = findViewById(R.id.contentView)
        signInButton = findViewById(R.id.signInButton)
        authErrorView = findViewById(R.id.authErrorView)
        accountView = findViewById(R.id.accountView)
        signOutButton = findViewById(R.id.signOutButton)
        emptyView = findViewById(R.id.emptyView)
        gmailStatusView = findViewById(R.id.gmailStatusView)
        gmailConnectButton = findViewById(R.id.gmailConnectButton)

        setupRecyclerView()

        // On observe la liste des résumés et l'état de lecture / synthèse.
        summaryViewModel.summaries.observe(this) { summaries ->
            summaryAdapter.submitList(summaries.orEmpty())
            emptyView.visibility = if (summaries.isNullOrEmpty()) View.VISIBLE else View.GONE
        }
        summaryViewModel.playingId.observe(this) { summaryAdapter.setPlayingId(it) }
        summaryViewModel.loadingId.observe(this) { summaryAdapter.setLoadingId(it) }

        signInButton.setOnClickListener { signInWithGoogle() }
        signOutButton.setOnClickListener { signOut() }
        gmailConnectButton.setOnClickListener { connectGmail() }

        // On demande la permission au démarrage
        askBluetoothConnectPermission()

        // On restaure l'état à partir de la session persistée (ou l'écran de connexion).
        val accountId = SessionStore.accountId(this)
        if (accountId != null) {
            showSignedIn(accountId, SessionStore.email(this))
        } else {
            showSignedOut(null)
        }
    }

    private fun setupRecyclerView() {
        recyclerView = findViewById(R.id.recyclerView)
        summaryAdapter = SummaryAdapter(
            onPlayClicked = { summary -> summaryViewModel.onPlayClicked(summary) },
            onDeleteClicked = { summary -> confirmDelete(summary) }
        )
        recyclerView.adapter = summaryAdapter
        recyclerView.layoutManager = LinearLayoutManager(this)
    }

    private fun confirmDelete(summary: Summary) {
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.summary_delete_title)
            .setMessage(R.string.summary_delete_message)
            .setNegativeButton(R.string.summary_delete_cancel, null)
            .setPositiveButton(R.string.summary_delete_confirm) { _, _ ->
                summaryViewModel.deleteSummary(summary)
            }
            .show()
    }

    private fun askBluetoothConnectPermission() {
        // La permission n'est requise que pour Android 12 (API 31) et plus
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            when {
                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.BLUETOOTH_CONNECT
                ) == PackageManager.PERMISSION_GRANTED -> {
                    // La permission est déjà accordée.
                    Log.d("Permissions", "Permission BLUETOOTH_CONNECT déjà accordée.")
                }

                shouldShowRequestPermissionRationale(Manifest.permission.BLUETOOTH_CONNECT) -> {
                    // Expliquer à l'utilisateur pourquoi vous avez besoin de cette permission.
                    // Pour l'instant, on demande directement.
                    requestPermissionLauncher.launch(Manifest.permission.BLUETOOTH_CONNECT)
                }

                else -> {
                    // Demander directement la permission.
                    requestPermissionLauncher.launch(Manifest.permission.BLUETOOTH_CONNECT)
                }
            }
        }
    }

    /**
     * Lance le flux « Se connecter avec Google » via Credential Manager.
     * Le serverClientId est le client OAuth Web (généré à partir de google-services.json).
     */
    private fun signInWithGoogle() {
        authErrorView.visibility = View.GONE

        val signInOption = GetSignInWithGoogleOption
            .Builder(getString(R.string.default_web_client_id))
            .build()
        val request = GetCredentialRequest.Builder()
            .addCredentialOption(signInOption)
            .build()

        lifecycleScope.launch {
            try {
                val response = credentialManager.getCredential(this@MainActivity, request)
                handleSignInResponse(response)
            } catch (e: GetCredentialException) {
                Log.e(TAG, "Connexion via Credential Manager échouée", e)
                showSignedOut(getString(R.string.auth_sign_in_failed))
            }
        }
    }

    private fun handleSignInResponse(response: GetCredentialResponse) {
        val credential = response.credential
        if (credential is CustomCredential &&
            credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
        ) {
            val googleCredential = GoogleIdTokenCredential.createFrom(credential.data)
            // Le claim `sub` de l'ID token est l'ID de compte Google (= ID du document Firestore).
            val accountId = subjectFromIdToken(googleCredential.idToken)
            if (accountId == null) {
                Log.e(TAG, "ID token sans claim `sub`, synchronisation impossible.")
                showSignedOut(getString(R.string.auth_sign_in_failed))
                return
            }
            val email = googleCredential.id

            // On s'authentifie auprès de Firebase avec l'ID token Google : indispensable
            // pour que l'écriture Firestore passe les règles de sécurité (request.auth != null).
            val firebaseCredential = GoogleAuthProvider.getCredential(googleCredential.idToken, null)
            firebaseAuth.signInWithCredential(firebaseCredential)
                .addOnSuccessListener {
                    SessionStore.save(this, accountId, email)
                    showSignedIn(accountId, email)
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "Authentification Firebase échouée", e)
                    showSignedOut(getString(R.string.auth_sign_in_failed))
                }
        } else {
            Log.e(TAG, "Type d'identifiant inattendu : ${credential.type}")
            showSignedOut(getString(R.string.auth_sign_in_failed))
        }
    }

    /**
     * Décode la charge utile (payload) d'un ID token JWT et en extrait le claim `sub`.
     */
    private fun subjectFromIdToken(idToken: String): String? {
        return try {
            val parts = idToken.split(".")
            if (parts.size < 2) return null
            val payload = String(
                Base64.decode(parts[1], Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP)
            )
            JSONObject(payload).optString("sub").ifEmpty { null }
        } catch (e: Exception) {
            Log.e(TAG, "Impossible de décoder l'ID token", e)
            null
        }
    }

    /**
     * Affiche l'écran d'authentification. [errorMessage] est non nul quand on y
     * revient à la suite d'un échec de connexion.
     */
    private fun showSignedOut(errorMessage: String?) {
        contentView.visibility = View.GONE
        authView.visibility = View.VISIBLE
        if (errorMessage != null) {
            authErrorView.text = errorMessage
            authErrorView.visibility = View.VISIBLE
        } else {
            authErrorView.visibility = View.GONE
        }
    }

    /**
     * Affiche le contenu principal pour le compte connecté et synchronise son token FCM.
     */
    private fun showSignedIn(accountId: String, email: String?) {
        authView.visibility = View.GONE
        contentView.visibility = View.VISIBLE
        accountView.text = getString(R.string.content_signed_in_as, email ?: accountId)
        updateGmailUi()
        syncFcmToken(accountId)
    }

    /**
     * Étape « Connecter Gmail » : demande le scope gmail.readonly + un server auth code
     * (accès hors-ligne) contre le client Web, puis transmet le code au backend.
     */
    private fun connectGmail() {
        val builder = AuthorizationRequest.builder()
            .setRequestedScopes(listOf(Scope("https://www.googleapis.com/auth/gmail.readonly")))
            // Le serverClientId DOIT être le client Web (celui dont le backend a le secret),
            // pas le client Android. forceCodeForRefreshToken garantit un refresh token.
            .requestOfflineAccess(getString(R.string.default_web_client_id), true)
        // On cible le compte déjà connecté.
        SessionStore.email(this)?.let { builder.setAccount(Account(it, "com.google")) }

        Identity.getAuthorizationClient(this)
            .authorize(builder.build())
            .addOnSuccessListener { result ->
                val pendingIntent = result.pendingIntent
                if (result.hasResolution() && pendingIntent != null) {
                    // Consentement requis : on lance l'écran système.
                    try {
                        authorizationLauncher.launch(
                            IntentSenderRequest.Builder(pendingIntent.intentSender).build()
                        )
                    } catch (e: Exception) {
                        Log.e(TAG, "Échec du lancement du consentement Gmail", e)
                        Toast.makeText(this, R.string.gmail_connect_failed, Toast.LENGTH_SHORT).show()
                    }
                } else {
                    // Déjà autorisé : le code est disponible directement.
                    handleAuthorizationResult(result)
                }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Autorisation Gmail échouée", e)
                Toast.makeText(this, R.string.gmail_connect_failed, Toast.LENGTH_SHORT).show()
            }
    }

    private fun handleAuthorizationResult(result: AuthorizationResult) {
        val code = result.serverAuthCode
        if (code == null) {
            Log.e(TAG, "Aucun server auth code renvoyé par l'autorisation Gmail.")
            Toast.makeText(this, R.string.gmail_connect_failed, Toast.LENGTH_SHORT).show()
            return
        }
        postAuthCodeToBackend(code)
    }

    private fun postAuthCodeToBackend(code: String) {
        lifecycleScope.launch {
            try {
                val response = BackendClient.apiService.exchangeAuthCode(ExchangeCodeRequest(code))
                if (response.isSuccessful) {
                    SessionStore.setGmailConnected(this@MainActivity, true)
                    updateGmailUi()
                    Toast.makeText(this@MainActivity, R.string.gmail_connect_success, Toast.LENGTH_SHORT).show()
                } else {
                    Log.e(TAG, "Le backend a refusé le code (HTTP ${response.code()})")
                    Toast.makeText(this@MainActivity, R.string.gmail_connect_failed, Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Échec de l'envoi du code au backend", e)
                Toast.makeText(this@MainActivity, R.string.gmail_connect_failed, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun updateGmailUi() {
        val connected = SessionStore.isGmailConnected(this)
        gmailStatusView.text =
            getString(if (connected) R.string.gmail_connected else R.string.gmail_prompt)
        gmailConnectButton.visibility = if (connected) View.GONE else View.VISIBLE
    }

    private fun signOut() {
        firebaseAuth.signOut()
        lifecycleScope.launch {
            try {
                credentialManager.clearCredentialState(
                    androidx.credentials.ClearCredentialStateRequest()
                )
            } catch (e: Exception) {
                Log.w(TAG, "Échec du nettoyage de l'état d'identifiant", e)
            }
            SessionStore.clear(this@MainActivity)
            showSignedOut(null)
        }
    }

    /**
     * Récupère le token FCM actuel et le synchronise dans Firestore pour le compte donné.
     */
    private fun syncFcmToken(accountId: String) {
        // On utilise le lifecycleScope pour que la coroutine soit automatiquement
        // annulée si l'activité est détruite, évitant les fuites de mémoire.
        lifecycleScope.launch {
            try {
                val token = FirebaseMessaging.getInstance().token.await()
                Log.d(TAG, "Token FCM récupéré : $token")
                FirestoreSync.updateFcmToken(accountId, token)
            } catch (e: Exception) {
                Log.e(TAG, "La récupération du token FCM a échoué", e)
            }
        }
    }
}
