# Directive: Initialiser un Projet Kotlin Multiplatform

## Objectif

Créer un nouveau projet Kotlin Multiplatform (KMP) avec support Android et préparation iOS, incluant:
- Configuration Gradle multi-modules
- Dépendances Firebase, Room, Jetpack Compose
- Structure MVVM
- Wrapper Gradle

## Inputs

- Nom du projet (ex: "BandTrack")
- Package Android (ex: "com.bandtrack")
- Version minimale Android (ex: API 24)
- Compte Firebase configuré

## Processus

### 1. Configuration Gradle

Créer les fichiers de configuration suivants:

**`settings.gradle.kts`** :
- Déclarer les repositories (Google, MavenCentral)
- Inclure les modules: `androidApp`, `shared`

**`build.gradle.kts` (racine)** :
- Plugins: Android Application/Library, Kotlin Multiplatform, Google Services
- Tâche  de nettoyage

**`gradle.properties`** :
- Configuration JVM (heap size, encoding)
- Options Kotlin et Android

### 2. Module Shared (Code commun KMP)

**`shared/build.gradle.kts`** :
- Plugin Kotlin Multiplatform
- Source sets: `commonMain`, `androidMain`, `iosMain`
- Dépendances communes: Coroutines, DateTime, Serialization
- Dépendances Android: Room, Firebase, Lifecycle

### 3. Module AndroidApp

**`androidApp/build.gradle.kts`** :
- Plugin Android Application + Google Services
- Dependency sur `:shared`
- Configuration Compose
- Dépendances: Firebase, Room, Biometric, Nearby, Camera, QR Code

**`AndroidManifest.xml`** :
Permissions critiques:
- `INTERNET`, `RECORD_AUDIO`
- `USE_BIOMETRIC`
- `BLUETOOTH_*`, `NEARBY_WIFI_DEVICES`
- `POST_NOTIFICATIONS`, `CAMERA`

### 4. Fichiers Kotlin de base

**`BandTrackApplication.kt`** :
```kotlin
class BandTrackApplication : Application() {
    override fun onCreate() {
        FirebaseApp.initializeApp(this)
        // Initialiser Room, WorkManager
    }
}
```

**`MainActivity.kt`** :
- Activité Compose avec `setContent`
- Thème BandTrack

**Thème Material3** :
- `Color.kt` : Palette High Contrast (mode sombre prioritaire)
- `Type.kt` : Typographie mobile
- `Theme.kt` : Configuration Material Theme

### 5. Ressources Android

**`res/values/strings.xml`** :
- Chaînes de caractères localisées (français)
- Navigation, Auth, Features, Erreurs

**`res/values/themes.xml`** :
- Thème de base Material

**`proguard-rules.pro`** :
- Règles pour Firebase, Room, Coroutines

### 6. Gradle Wrapper

**`gradle/wrapper/gradle-wrapper.properties`** :
- Distribution URL (Gradle 8.2.1+)
- Chemins de stockage

**`gradlew.bat`** :
- Script batch Windows pour lancer Gradle

### 7. Configuration Git

**`.gitignore`** :
Exclure:
- `build/`, `.gradle/`, `.idea/`
- `google-services.json`
- Keystores, secrets
- `__pycache__/`, `.venv/`

## Outputs

- Projet KMP complet et compilable
- Structure MVVM prête
- Firebase intégré
- Wrapper Gradle fonctionnel

## Vérification

```bash
# Synchronisation Gradle (auto dans Android Studio)
./gradlew build --dry-run

# Compilation module shared
./gradlew shared:build

# Compilation androidApp
./gradlew androidApp:assembleDebug
```

## Edge Cases

### Problème: Gradle sync échoue
- Vérifier JDK 17+ installé
- Vérifier `google-services.json` présent
- Clear caches: `./gradlew clean`

### Problème: Dépendances Firebase non résolues
- Vérifier plugin `com.google.gms.google-services` appliqué
- Vérifier Firebase BoM version à jour

### Problème: Room compiler ne fonctionne pas
- Préférer KSP à kapt (meilleure compatibilité KMP)
- Ajouter: `id("com.google.devtools.ksp")` si nécessaire

## Temps estimé

- Configuration initiale: 15-20 minutes
- Première synchronisation Gradle: 5-10 minutes
- Tests de compilation: 2-3 minutes

## Remarques

- Le wrapper Gradle évite les conflits de versions
- Firebase BoM gère automatiquement les versions compatibles
- Structure KMP permet d'ajouter iOS sans refonte
- Mode sombre par défaut pour usage en studio/scène
