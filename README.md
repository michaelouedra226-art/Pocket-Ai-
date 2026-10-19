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
  - **Dart Package** : `pocketstrike` (imports préservés)
  - **Prérequis** : Flutter >= 3.29, Dart >= 3.12, Java 17, `minSdk 26`, `compileSdk 37`

---

## 🛠️ Compilation locale

### 1. Prérequis
- Flutter SDK (>= 3.29)
- JDK 17 (Temurin recommandé)
- Android SDK avec build-tools et plateformes installés

### 2. Commandes de build

```bash
# Résolution des dépendances
flutter pub get

# Analyse statique du code
flutter analyze

# Compilation APK Release (Universel)
flutter build apk --release
# → Artefact généré : build/app/outputs/flutter-apk/app-release.apk

# Compilation APK Release optimisé (Split par ABI)
flutter build apk --release --split-per-abi
# → Artefact ARM64 : build/app/outputs/flutter-apk/app-arm64-v8a-release.apk

# Compilation Android App Bundle (AAB pour Google Play)
flutter build appbundle --release
# → Artefact généré : build/app/outputs/bundle/release/app-release.aab
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
- **Automatique** : À la création d'un tag de version commençant par `v` (ex. `v1.0.0`).
- **Manuel** : Via le bouton *Run workflow* (`workflow_dispatch`) dans l'onglet **Actions** de GitHub.

### Secrets GitHub à configurer

Dans les paramètres de votre dépôt GitHub (**Settings > Secrets and variables > Actions > Repository secrets**) :

| Secret | Description |
|---|---|
| `KEYSTORE_BASE64` | Contenu de `upload-keystore.jks` encodé en base64 (`base64 -w 0 upload-keystore.jks`) |
| `KEYSTORE_PASSWORD` | Mot de passe du keystore |
| `KEY_PASSWORD` | Mot de passe de la clé privée |
| `KEY_ALIAS` | Alias de la clé (ex: `upload`) |

### Artefacts produits et renommés

À chaque build, les livrables suivants sont générés dans le dossier `dist/` :
- `DeepSeekHermes-<tag>-universal.apk` : APK universel compatible avec tous les appareils.
- `DeepSeekHermes-<tag>-arm64.apk` : APK 64-bit optimisé pour les téléphones modernes.
- `DeepSeekHermes-<tag>.aab` : Android App Bundle prêt pour le déploiement sur le Google Play Store.

Les fichiers sont téléchargeables pendant 30 jours via **Actions > Artifacts** et attachés automatiquement à la **Release GitHub** lors du push d'un tag.

```bash
# Exemple pour déclencher une release GitHub
git tag v1.0.0
git push origin v1.0.0
```
