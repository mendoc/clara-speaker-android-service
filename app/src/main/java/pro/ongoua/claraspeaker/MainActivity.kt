package pro.ongoua.claraspeaker

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // On demande la permission au démarrage
        askBluetoothConnectPermission()

        // On lance la récupération du token au démarrage de l'activité
        retrieveAndSendFcmToken()
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

                // C'est ici que vous envoyez le token à votre serveur (via une API)
                sendTokenToServer(token)

            } catch (e: Exception) {
                // Gérer l'erreur si la récupération du token échoue
                Log.e("ClaraSpeaker@MainActivity", "La récupération du token a échoué", e)
            }
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