# DeepSeek Hermes

> **DeepSeek Hermes** (fork de PocketStrike 1.0.0) — Station de travail d'agent IA mobile autonome multi-fournisseurs (DeepSeek natif en priorité, OpenAI, Claude, Gemini, Ollama) avec inférence locale GGUF (llama.cpp), mémoire Hermes auto-évolutive, boucle d'agent ReAct, protocole MCP et relais Telegram.

---

## 📱 Caractéristiques principales

- **Multi-Provider AI** : DeepSeek natif en priorité (`api.deepseek.com` : `deepseek-chat`, `deepseek-reasoner`, `deepseek-coder`), ainsi qu'OpenAI, Anthropic Claude, Google Gemini et Ollama.
- **Inférence locale GGUF** : Exécution sur l'appareil via `llama.cpp` / `llama_flutter_android`.
- **Mémoire Hermes** : Magasin de mémoire contextuel auto-évolutif et persistant.
- **Agent ReAct & MCP** : Détection et exécution d'outils, Model Context Protocol et tâches planifiées.
- **Relais Telegram** : Service de messagerie en arrière-plan pour interagir à distance avec l'agent.
- **Spécifications Android** :
  - **applicationId** : `com.deepseekhermes.app`
  - **Label Launcher** : `DeepSeek Hermes`
  - **Technologie** : Kotlin et Jetpack Compose
  - **Prérequis** : JDK 17, Android SDK 36, `minSdk 26`, `targetSdk 36`

---

## 🛠️ Compilation locale

### 1. Prérequis
- JDK 17 (Temurin recommandé)
- Android SDK avec la plateforme Android 36 et les build-tools 36 installés
- Aucun téléchargement manuel de Gradle n'est requis : le wrapper `./gradlew` est fourni

### 2. Commandes de build

Le build local utilise `debug.keystore` à la racine (le workflow CI le génère automatiquement). Si le fichier n'existe pas encore, créez-le une fois :

```bash
keytool -genkeypair -v -keystore debug.keystore -storepass android \
  -alias androiddebugkey -keypass android -keyalg RSA -keysize 2048 \
  -validity 10000 -dname "CN=Android Debug,O=Android,C=US"
```

```bash
# Exécuter les tests unitaires
./gradlew :app:testDebugUnitTest

# Compiler l'APK de débogage
./gradlew :app:assembleDebug
# → Artefact : app/build/outputs/apk/debug/app-debug.apk

# Compiler l'APK Release et l'Android App Bundle
./gradlew :app:assembleRelease :app:bundleRelease
# → Artefacts : app/build/outputs/apk/release/app-release.apk
#               app/build/outputs/bundle/release/app-release.aab

```

---

## 🔐 Configuration de la signature Release

Pour signer les APKs et l'App Bundle en production :

### 1. Génération du Keystore

Exécutez la commande suivante dans un terminal :

```bash
keytool -genkeypair -v \
  -keystore upload-keystore.jks \
  -keyalg RSA \
  -keysize 2048 \
  -validity 10000 \
  -alias upload
```

> ⚠️ **Sécurité** : Ne committez **JAMAIS** `upload-keystore.jks` ni `key.properties` dans votre dépôt Git (ils sont exclus par `.gitignore`).

### 2. Configuration locale (`key.properties`)

Copiez le modèle et renseignez vos identifiants :

```bash
cp android/key.properties.example android/key.properties
```

Contenu de `android/key.properties` :
```properties
storePassword=votre_mot_de_passe_keystore
keyPassword=votre_mot_de_passe_cle
keyAlias=upload
storeFile=upload-keystore.jks
```

> ℹ️ **Mode secours (Fallback)** : Si aucun fichier `key.properties` ni variable secrète n'est configuré, le système de build utilise automatiquement une clé de débogage pour produire un APK installable immédiatement en sideload.

---

## 🚀 Intégration continue (GitHub Actions CI/CD)

Le workflow `.github/workflows/build-android.yml` automatise la compilation et la publication des livrables Android.

### Déclencheurs
- **Automatique** : À chaque push ou pull request vers `main`, et à la création d'un tag commençant par `v` (ex. `v1.0.0`).
- **Manuel** : Via le bouton *Run workflow* (`workflow_dispatch`) dans l'onglet **Actions** de GitHub.

### Secrets GitHub à configurer

Dans les paramètres de votre dépôt GitHub (**Settings > Secrets and variables > Actions > Repository secrets**) :

| Secret | Description |
|---|---|
| `KEYSTORE_BASE64` | Contenu de `upload-keystore.jks` encodé en base64 (`base64 -w 0 upload-keystore.jks`) |
| `KEYSTORE_PASSWORD` | Mot de passe du keystore |
| `KEY_PASSWORD` | Mot de passe de la clé privée |
| `KEY_ALIAS` | Alias de la clé (ex: `upload`) |

### Artefacts produits

À chaque build, les APK de débogage et de release ainsi que l'Android App Bundle sont téléversés dans **Actions > Artifacts** et conservés pendant 30 jours. Lors d'un push de tag `v*`, l'APK de release et l'AAB sont aussi attachés à la GitHub Release.

```bash
# Exemple pour déclencher une release GitHub
git tag v1.0.0
git push origin v1.0.0
```
