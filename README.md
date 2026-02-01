# BandTrack Mobile

Application mobile collaborative pour groupes de musiciens, développée en **Kotlin Multiplatform** avec **Jetpack Compose**.

## 🎯 Fonctionnalités

- **Multi-groupes** : Gérez plusieurs projets musicaux depuis un seul compte
- **Mode hors-ligne** : Travaillez sans connexion (synchronisation automatique au retour réseau)
- **Suggestions et votes** : Proposez et votez pour de nouveaux morceaux
- **Répertoire** : Suivez votre progression sur chaque morceau (slider 0-10)
- **Notes audio** : Enregistrez des mémos vocaux pour chaque titre
- **Planification** : Gérez calendrier et prestations avec notifications
- **Partage P2P** : Partagez vos fichiers audio en peer-to-peer
- **Invitations** : Codes, QR codes et liens d'invitation pour recruter des membres

## 🏗️ Architecture

- **Frontend** : Jetpack Compose (Android)
- **Architecture** : MVVM (Model-View-ViewModel)
- **Multiplateforme** : Kotlin Multiplatform (préparé pour iOS)
- **Base locale** : Room Database
- **Cloud** : Firebase (Auth, Firestore, FCM)
- **Sécurité** : Authentification biométrique, EncryptedSharedPreferences

## 📋 Prérequis

- **Android Studio** : Hedgehog (2023.1.1) ou supérieur
- **JDK** : 17 ou supérieur
- **Android SDK** : API 24+ (Android 7.0+)
- **Compte Firebase** : Projet configuré (voir [FIREBASE_SETUP.md](FIREBASE_SETUP.md))

## 🚀 Installation

### 1. Cloner le dépôt

```bash
git clone https://github.com/votre-username/bandtrack-mobile.git
cd bandtrack-mobile
```

### 2. Configurer Firebase

Suivez les instructions détaillées dans [FIREBASE_SETUP.md](FIREBASE_SETUP.md), puis :

1. Téléchargez `google-services.json` depuis la console Firebase
2. Placez-le dans `androidApp/google-services.json`

### 3. Ouvrir le projet

1. Lancez **Android Studio**
2. **File** > **Open** > Sélectionnez le dossier `bandtrack-mobile`
3. Attendez la synchronisation Gradle (première fois ~5-10 minutes)

### 4. Lancer l'application

1. Connectez un appareil Android ou lancez un émulateur
2. Cliquez sur **Run** (▶️) ou `Shift+F10`

## 📁 Structure du projet

```
bandtrack-mobile/
├── androidApp/              # Application Android
│   ├── src/main/
│   │   ├── java/com/bandtrack/
│   │   │   ├── ui/          # Interfaces Compose
│   │   │   ├── services/    # Services Android
│   │   │   └── MainActivity.kt
│   │   └── res/             # Ressources Android
│   └── build.gradle.kts
│
├── shared/                  # Code partagé KMP
│   └── src/
│       ├── commonMain/      # Code commun
│       │   ├── data/        # Models, Repositories
│       │   ├── domain/      # Use cases
│       │   └── ui/          # ViewModels
│       └── androidMain/     # Code spécifique Android
│
├── .agent/                  # Directives et workflows
│   ├── directives/          # SOPs métier
│   └── workflows/           # Workflows de développement
│
├── execution/               # Scripts Python
├── FIREBASE_SETUP.md        # Guide configuration Firebase
├── CCF_bandtrack.md         # Cahier des charges
└── README.md                # Ce fichier
```

## 🧪 Tests

```bash
# Tests unitaires (module shared)
./gradlew shared:testDebugUnitTest

# Tests unitaires (module androidApp)
./gradlew androidApp:testDebugUnitTest

# Tests d'instrumentation Android (nécessite appareil/émulateur)
./gradlew androidApp:connectedAndroidTest
```

## 📱 Build APK

```bash
# Debug APK
./gradlew androidApp:assembleDebug

# Release APK (nécessite keystore configuré)
./gradlew androidApp:assembleRelease
```

## 🔒 Sécurité

- ❌ **Ne JAMAIS commiter** `google-services.json` dans un dépôt public
- ✅ Utilisez `.gitignore` pour exclure les fichiers sensibles
- ✅ Les données sensibles utilisent `EncryptedSharedPreferences`
- ✅ Firebase Security Rules activées (voir console Firebase)

## 📚 Documentation

- [Cahier des charges](CCF_bandtrack.md) : Spécifications complètes
- [Configuration Firebase](FIREBASE_SETUP.md) : Guide pas-à-pas
- [Plan d'implémentation](C:\Users\ebrangerieau\.gemini\antigravity\brain\11fa0c1d-5800-4763-aae3-64228d9c996f\implementation_plan.md) : Détails techniques
- [Instructions Agent](AGENT.md) : Architecture 3-layers

## 🤝 Contribution

1. Fork le projet
2. Créez une branche (`git checkout -b feature/nouvelle-fonctionnalite`)
3. Committez vos changements (`git commit -m 'Ajout nouvelle fonctionnalité'`)
4. Push vers la branche (`git push origin feature/nouvelle-fonctionnalite`)
5. Ouvrez une Pull Request

## 📝 Licence

Voir le fichier [LICENSE](LICENSE) pour plus de détails.

## 👨‍💻 Auteur

Développé dans le cadre du projet BandTrack - Application collaborative pour musiciens.

## 🎵 Roadmap

- [x] Phase 1 : Infrastructure et configuration
- [ ] Phase 2 : Authentification et multi-groupes
- [ ] Phase 3 : Fonctionnalités métier (suggestions, répertoire)
- [ ] Phase 4 : Planification et prestations
- [ ] Phase 5 : Synchronisation offline
- [ ] Phase 6 : Partage P2P des médias
- [ ] Phase 7 : Interface utilisateur complète
- [ ] Phase 8 : Sécurité et finalisation
- [ ] Version iOS avec Kotlin Multiplatform

---

**Note** : Ce projet est en cours de développement actif. Consultez le [task.md](C:\Users\ebrangerieau\.gemini\antigravity\brain\11fa0c1d-5800-4763-aae3-64228d9c996f\task.md) pour suivre l'avancement.
