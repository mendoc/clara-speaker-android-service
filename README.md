# Clara Speaker

Clara Speaker est une application Android conçue pour écouter des résumés textuels de manière fluide et intelligente. Elle reçoit des textes courts via des notifications push, les convertit en audio, et les lit automatiquement lorsqu'un casque Bluetooth est connecté.

## 🚀 Fonctionnalités Principales

- **Réception de Résumés** : Intégration avec Firebase Cloud Messaging (FCM) pour recevoir des textes à la volée.
- **Synthèse Vocale** : Utilise l'API Text-to-Speech d'ElevenLabs pour générer un audio de haute qualité en français.
- **Lecture Intelligente via Bluetooth** :
    - Si un casque est connecté, le résumé est lu immédiatement.
    - Sinon, le résumé est mis en file d'attente.
- **Lecture Différée** : Dès qu'un casque Bluetooth est connecté, l'application lit tous les résumés en attente de manière séquentielle.
- **Interface de Consultation** : Un écran principal affiche les 3 derniers résumés joués, permettant de les réécouter ou de les supprimer.
- **Persistance Locale** : Utilise la base de données Room pour stocker l'historique des résumés, leur état (lu/non lu) et le chemin vers le fichier audio généré.

## 🛠️ Architecture Technique

Le projet suit une architecture Android moderne (MVVM) et s'articule autour des composants suivants :

- **`MyFirebaseMessagingService`** : Service qui écoute les notifications push de FCM, sauvegarde immédiatement chaque résumé en base de données et déclenche la lecture si nécessaire.
- **`Room` (`AppDatabase`, `SummaryDao`, `Summary`)** : Couche de persistance pour stocker les résumés. Le schéma inclut le texte, l'état de lecture, la date, le modèle de voix utilisé et le chemin du fichier audio.
- **`Retrofit` (`TtsApiService`)** : Client HTTP pour communiquer avec l'API Text-to-Speech d'ElevenLabs.
- **`AudioPlayerManager`** : Un objet singleton qui centralise la logique de synthèse vocale, la sauvegarde du fichier audio `.mp3` dans le stockage interne et la gestion de la lecture via `MediaPlayer`.
- **`BluetoothConnectionReceiver`** : Un `BroadcastReceiver` qui détecte les connexions d'appareils Bluetooth (`ACTION_ACL_CONNECTED`) et lance la lecture des résumés en attente.
- **`MVVM` (`MainActivity`, `SummaryViewModel`, `SummaryAdapter`)** : L'interface utilisateur est gérée par une `Activity` qui observe un `ViewModel`. Le `ViewModel` expose les données via `LiveData`, et un `RecyclerView` les affiche.

## ⚙️ Configuration du Projet

Pour compiler et exécuter le projet, suivez ces étapes :

1.  **Cloner le dépôt**
    ```bash
    git clone https://github.com/mendoc/clara-speaker-android-service.git
    ```

2.  **Ouvrir dans Android Studio**
    Importez le projet dans Android Studio.

3.  **Configuration de Firebase**
    - Vous devez avoir un projet Firebase configuré pour une application Android.
    - Téléchargez votre propre fichier `google-services.json` depuis la console Firebase.
    - Placez ce fichier dans le dossier `app/` du projet.

4.  **Clé d'API ElevenLabs**
    - L'application nécessite une clé d'API pour l'API Text-to-Speech d'ElevenLabs.
    - Créez un fichier nommé `local.properties` à la racine du projet (au même niveau que `build.gradle`).
    - Ajoutez votre clé d'API dans ce fichier comme suit :
      ```properties
      elevenlabs.apikey=VOTRE_CLE_API_PERSONNELLE_ICI
      ```

5.  **Compiler et Exécuter**
    Synchronisez le projet avec les fichiers Gradle, puis compilez et exécutez l'application sur un émulateur ou un appareil physique.

## ⚠️ Permissions Requises

L'application demandera les permissions suivantes lors de l'exécution :

- `BLUETOOTH_CONNECT` (pour Android 12 et supérieur) : Pour détecter la connexion des appareils Bluetooth.
- `POST_NOTIFICATIONS` (pour Android 13 et supérieur) : Pour pouvoir recevoir les notifications push de Firebase.
