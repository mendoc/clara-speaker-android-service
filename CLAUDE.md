# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Vue d'ensemble

Clara Speaker est un service Android (Kotlin, MVVM) qui reçoit des « résumés » textuels via Firebase Cloud Messaging (FCM), les convertit en audio avec l'API Google Cloud Text-to-Speech, et les lit automatiquement lorsqu'un casque Bluetooth est connecté. Un seul module : `app`.

## Commandes

```bash
# Compilation
./gradlew assembleDebug            # APK debug
./gradlew assembleRelease          # APK release signé (nécessite les propriétés de signature, voir ci-dessous)
./gradlew build                    # Compile + tests + lint

# Tests
./gradlew test                     # Tests unitaires JVM (app/src/test)
./gradlew testDebugUnitTest        # Tests unitaires sur la variante debug
./gradlew connectedAndroidTest     # Tests instrumentés (app/src/androidTest, nécessite un appareil/émulateur)

# Un seul test unitaire
./gradlew test --tests "pro.ongoua.claraspeaker.ExampleUnitTest.addition_isCorrect"

# Lint
./gradlew lint                     # Rapport dans app/build/reports/lint-results-*.html

# Installer sur un appareil connecté
./gradlew installDebug
```

## Configuration requise pour compiler

Deux éléments **non versionnés** sont nécessaires (voir `.gitignore`) :

1. **`app/google-services.json`** — configuration Firebase. À télécharger depuis la console Firebase.
2. **`local.properties`** à la racine, contenant la clé TTS **ElevenLabs**. La propriété lue par `app/build.gradle` est `elevenlabs.apikey` :
   ```properties
   elevenlabs.apikey=VOTRE_CLE_API
   ```
   Elle est exposée au code via `BuildConfig.ELEVENLABS_API_KEY`.

Pour un build release signé, passer les propriétés de signature en ligne de commande (elles injectent le keystore encodé en Base64, cf. `signingConfigs.release`) :
```bash
./gradlew assembleRelease -PsigningKeyBase64=... -PkeystorePassword=... -PkeyAlias=... -PkeyPassword=...
```

Toolchain : JDK 17, `compileSdk` 35, `targetSdk` 34, `minSdk` 24. Android Gradle Plugin 8.6.0, Kotlin 2.1.20, Gradle 8.7.

## Architecture

Le flux central est **événementiel**, pas piloté par l'UI. L'`Activity` ne sert qu'à consulter/réécouter l'historique ; la logique métier vit dans un service, un receiver et un singleton.

**Réception (`MyFirebaseMessagingService`)** — Point d'entrée. Reçoit un message FCM contenant `summaryText`, sauvegarde immédiatement un `Summary` en base avec `isPlayed = false`, puis déclenche la lecture **uniquement si** un casque Bluetooth est déjà connecté.

**Persistance Room (`AppDatabase`, `Summary`, `SummaryDao`)** — Source de vérité unique. Un `Summary` porte le texte, `isPlayed`, la date, le modèle de voix, et le chemin du fichier audio `.mp3` (rempli après synthèse). Le DAO distingue les résumés non lus (file d'attente) des résumés déjà joués (historique). Schémas Room exportés dans `app/schemas` (`room.schemaLocation`).

**Synthèse & lecture (`AudioPlayerManager`, `TtsApiService`, `RetrofitClient`)** — `AudioPlayerManager` est un `object` singleton qui centralise TOUTE la logique audio. Il appelle l'API **ElevenLabs** (modèle `eleven_v3`, voix fixe « David - Gruff Cowboy », clé dans le header `xi-api-key`) via Retrofit, écrit les octets audio bruts (mp3) dans un `.mp3` en stockage interne, met à jour le `Summary` (chemin + `isPlayed = true`), et lit via `MediaPlayer`.

**Lecture différée (`BluetoothConnectionReceiver`)** — Fonctionnalité clé. `BroadcastReceiver` sur `ACTION_ACL_CONNECTED` : à la connexion d'un appareil Bluetooth, lit une intro (« Vous avez X résumés en attente ») puis joue séquentiellement tous les `Summary` non lus, avec un délai entre chacun.

**UI MVVM (`MainActivity`, `SummaryViewModel`, `SummaryAdapter`)** — `MainActivity` observe le `SummaryViewModel` (LiveData) et affiche les 3 derniers résumés **déjà joués** dans un `RecyclerView`. Clic = réécoute. Au démarrage, demande la permission `BLUETOOTH_CONNECT` (Android 12+) et récupère le token FCM. ViewBinding activé (pas de Compose).

Enchaînement type : serveur → push FCM → sauvegarde → (casque connecté ? lecture immédiate : mise en file) → à la prochaine connexion Bluetooth, lecture de toute la file.

## Points d'attention

- Room utilise **kapt** (pas KSP) — la génération de code passe par `kotlin-kapt`.
- Le composant central à connaître pour toute modification audio est le singleton `AudioPlayerManager` ; presque tout y converge.
- Le CI (`.github/workflows/android-release.yml`) construit un APK release signé et crée une GitHub Release taguée `vYYYY.MM.DD-HHMM` à chaque push sur `main`. Les secrets (`google-services.json`, keystore, mots de passe) viennent des secrets GitHub Actions.
- `GEMINI.md` documente la même architecture (source complémentaire).
```
