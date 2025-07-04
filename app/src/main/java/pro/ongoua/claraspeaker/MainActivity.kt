package pro.ongoua.claraspeaker

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class MainActivity : AppCompatActivity() {

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
    private lateinit var recyclerView: RecyclerView
    private lateinit var summaryAdapter: SummaryAdapter
    private lateinit var fcmTokenView: TextView
    private lateinit var copyTokenButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        fcmTokenView = findViewById(R.id.fcmTokenView)
        copyTokenButton = findViewById(R.id.copyTokenButton)

        setupRecyclerView()

        // On observe les changements dans la base de données
        summaryViewModel.latestSummaries.observe(this) { summaries ->
            summaries?.let {
                // L'adapter met à jour la liste affichée automatiquement
                summaryAdapter.submitList(it)
            }
        }

        // On demande la permission au démarrage
        askBluetoothConnectPermission()

        // On lance la récupération du token au démarrage de l'activité
        retrieveAndSendFcmToken()

        copyTokenButton.setOnClickListener {
            copyTokenToClipboard()
        }
    }

    private fun setupRecyclerView() {
        recyclerView = findViewById(R.id.recyclerView)
        summaryAdapter = SummaryAdapter(
            onItemClicked = { summary ->
                // Le clic est géré ici et transmis au ViewModel
                summaryViewModel.onSummaryClicked(summary)
            },
            onDeleteClicked = { summary ->
                // La suppression est gérée ici et transmise au ViewModel
                summaryViewModel.deleteSummary(summary)
            }
        )
        recyclerView.adapter = summaryAdapter
        recyclerView.layoutManager = LinearLayoutManager(this)
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
     * Récupère le token FCM actuel et l'envoie au serveur.
     * C'est la méthode à appeler au démarrage de l'application.
     */
    private fun retrieveAndSendFcmToken() {
        // On utilise le lifecycleScope pour que la coroutine soit automatiquement
        // annulée si l'activité est détruite, évitant les fuites de mémoire.
        lifecycleScope.launch {
            try {
                // La méthode .token renvoie une Task, .await() la transforme en appel suspendu
                val token = FirebaseMessaging.getInstance().token.await()

                Log.d("ClaraSpeaker@MainActivity", "Token récupéré au démarrage : $token")
                fcmTokenView.text = token

                // C'est ici que vous envoyez le token à votre serveur (via une API)
                sendTokenToServer(token)

            } catch (e: Exception) {
                // Gérer l'erreur si la récupération du token échoue
                Log.e("ClaraSpeaker@MainActivity", "La récupération du token a échoué", e)
                fcmTokenView.text = "Erreur lors de la récupération du token."
            }
        }
    }

    private fun copyTokenToClipboard() {
        val token = fcmTokenView.text.toString()
        if (token.isNotEmpty() && !token.startsWith("Erreur")) {
            val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText("FCM Token", token)
            clipboard.setPrimaryClip(clip)
            Toast.makeText(this, "Token copié dans le presse-papiers", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Fonction factice pour envoyer le token au serveur.
     * Vous devrez implémenter la logique d'appel à votre API ici (ex: avec Retrofit).
     */
    private fun sendTokenToServer(token: String) {
        // TODO: Implémentez votre appel réseau ici pour envoyer le token
        // à votre base de données ou votre backend, en l'associant à l'utilisateur si nécessaire.
        Log.d("ClaraSpeaker@MainActivity", "Simulation d'envoi du token au serveur : $token")
    }
}