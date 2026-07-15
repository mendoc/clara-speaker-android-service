package pro.ongoua.claraspeaker

import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class BluetoothConnectionReceiver : BroadcastReceiver() {

    private val TAG = "BluetoothReceiver"

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == BluetoothDevice.ACTION_ACL_CONNECTED) {
            Log.d(TAG, "Un appareil Bluetooth a été connecté. Lancement de la séquence.")
            // La récupération des résumés non lus et la boucle de lecture vivent dans
            // AudioPlayerManager (point d'entrée partagé avec la réception FCM).
            AudioPlayerManager.startSequence(context)
        }
    }
}
