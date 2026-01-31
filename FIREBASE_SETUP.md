# Configuration Firebase pour BandTrack

Ce guide vous accompagne étape par étape dans la configuration complète de Firebase pour l'application BandTrack.

---

## Prérequis

- Un compte Google
- Android Studio installé
- Le projet BandTrack initialisé localement

---

## Étape 1 : Créer le projet Firebase

### 1.1 Accéder à la console Firebase

1. Ouvrez votre navigateur et accédez à [https://console.firebase.google.com/](https://console.firebase.google.com/)
2. Connectez-vous avec votre compte Google

### 1.2 Créer un nouveau projet

1. Cliquez sur **"Ajouter un projet"** (ou "Add project")
2. Donnez un nom au projet : `BandTrack` ou `bandtrack-prod`
3. **Google Analytics** : Vous pouvez désactiver Google Analytics pour commencer (optionnel)
4. Cliquez sur **"Créer le projet"**
5. Attendez que Firebase prépare votre projet (environ 30 secondes)
6. Cliquez sur **"Continuer"**

---

## Étape 2 : Ajouter l'application Android

### 2.1 Enregistrer l'application

1. Dans la console Firebase, sur la page d'accueil du projet, cliquez sur l'icône **Android** (symbole Android)
2. Remplissez les informations :
   - **Package Android** : `com.bandtrack` (ou votre propre package, IMPORTANT : notez-le bien)
   - **Nom de l'application** : `BandTrack`
   - **Certificat de signature SHA-1** : Laissez vide pour l'instant (nécessaire plus tard pour Google Sign-In si utilisé)
3. Cliquez sur **"Enregistrer l'application"**

### 2.2 Télécharger le fichier de configuration

1. Téléchargez le fichier `google-services.json`
2. **IMPORTANT** : Placez ce fichier dans le répertoire suivant de votre projet :
   ```
   bandtrack-mobile/androidApp/google-services.json
   ```
3. Cliquez sur **"Suivant"**

### 2.3 Ajouter le SDK Firebase (À FAIRE PLUS TARD)

Firebase vous montre des instructions pour ajouter les dépendances Gradle. Nous les ajouterons lors de la configuration du projet. Pour l'instant :

1. Cliquez sur **"Suivant"**
2. Cliquez sur **"Continuer vers la console"**

---

## Étape 3 : Activer Firebase Authentication

### 3.1 Accéder au module Authentication

1. Dans le menu de gauche, cliquez sur **"Authentication"** (🔐)
2. Cliquez sur **"Commencer"** (Get started)

### 3.2 Configurer les méthodes de connexion

#### Méthode E-mail/Mot de passe (OBLIGATOIRE)

1. Dans l'onglet **"Sign-in method"**, cliquez sur **"E-mail/Mot de passe"**
2. Activez le premier commutateur **"E-mail/Mot de passe"** (PAS "Lien de connexion par e-mail")
3. Cliquez sur **"Enregistrer"**

#### Méthode Google Sign-In (OPTIONNEL, recommandé)

1. Cliquez sur **"Google"**
2. Activez le commutateur
3. Renseignez **l'e-mail d'assistance du projet** (votre e-mail)
4. Cliquez sur **"Enregistrer"**

> **Note** : Pour le Google Sign-In en production, il faudra ajouter l'empreinte SHA-1 de votre certificat de signature.

---

## Étape 4 : Configurer Firestore Database

### 4.1 Créer la base de données

1. Dans le menu de gauche, cliquez sur **"Firestore Database"**
2. Cliquez sur **"Créer une base de données"**

### 4.2 Choisir le mode de démarrage

1. Sélectionnez **"Commencer en mode production"** (nous configurerons les règles juste après)
2. Cliquez sur **"Suivant"**

### 4.3 Choisir la localisation

1. Sélectionnez une localisation proche de vos utilisateurs :
   - **Europe** : `eur3 (europe-west)` - recommandé pour la France
   - **USA** : `nam5 (us-central)`
2. ⚠️ **ATTENTION** : Cette localisation ne peut PAS être changée après création
3. Cliquez sur **"Activer"**
4. Attendez la création de la base (environ 1 minute)

### 4.4 Configurer les règles de sécurité Firestore

1. Cliquez sur l'onglet **"Règles"** (Rules)
2. Remplacez le contenu par les règles suivantes :

```javascript
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    
    // Helper functions
    function isAuthenticated() {
      return request.auth != null;
    }
    
    function getUserId() {
      return request.auth.uid;
    }
    
    function isGroupMember(groupId) {
      return exists(/databases/$(database)/documents/groups/$(groupId)/members/$(getUserId()));
    }
    
    function isGroupAdmin(groupId) {
      let member = get(/databases/$(database)/documents/groups/$(groupId)/members/$(getUserId()));
      return member.data.role == 'admin';
    }
    
    // Users collection
    match /users/{userId} {
      allow read, write: if isAuthenticated() && getUserId() == userId;
    }
    
    // Groups collection
    match /groups/{groupId} {
      allow read: if isAuthenticated() && isGroupMember(groupId);
      allow create: if isAuthenticated();
      allow update, delete: if isAuthenticated() && isGroupAdmin(groupId);
      
      // Group members subcollection
      match /members/{memberId} {
        allow read: if isAuthenticated() && isGroupMember(groupId);
        allow create: if isAuthenticated() && (isGroupAdmin(groupId) || memberId == getUserId());
        allow update, delete: if isAuthenticated() && isGroupAdmin(groupId);
      }
      
      // Suggestions subcollection
      match /suggestions/{suggestionId} {
        allow read: if isAuthenticated() && isGroupMember(groupId);
        allow create: if isAuthenticated() && isGroupMember(groupId);
        allow update: if isAuthenticated() && isGroupMember(groupId);
        allow delete: if isAuthenticated() && (isGroupAdmin(groupId) || resource.data.createdBy == getUserId());
      }
      
      // Songs (Repertoire) subcollection
      match /songs/{songId} {
        allow read: if isAuthenticated() && isGroupMember(groupId);
        allow create, update: if isAuthenticated() && isGroupMember(groupId);
        allow delete: if isAuthenticated() && isGroupAdmin(groupId);
      }
      
      // Performances subcollection
      match /performances/{performanceId} {
        allow read: if isAuthenticated() && isGroupMember(groupId);
        allow create, update, delete: if isAuthenticated() && (isGroupAdmin(groupId) || isGroupMember(groupId));
      }
      
      // Invitation codes
      match /invitations/{invitationId} {
        allow read: if isAuthenticated();
        allow create: if isAuthenticated() && isGroupAdmin(groupId);
        allow delete: if isAuthenticated() && isGroupAdmin(groupId);
      }
    }
  }
}
```

3. Cliquez sur **"Publier"**

> **Explication des règles** :
> - Un utilisateur ne peut accéder qu'aux données des groupes dont il est membre
> - Seuls les admins peuvent modifier/supprimer les groupes
> - Tous les membres peuvent créer des suggestions et morceaux
> - Les notes audio restent locales (pas dans Firestore)

---

## Étape 5 : Activer Cloud Messaging (Notifications Push)

### 5.1 Configuration FCM

1. Dans le menu de gauche, cliquez sur **"Cloud Messaging"**
2. Les notifications push sont automatiquement activées avec le SDK
3. Aucune configuration supplémentaire n'est nécessaire pour l'instant

> **Note** : Le token FCM sera géré dans le code de l'application.

---

## Étape 6 : Structure de données Firestore (Optionnel - pour référence)

Voici la structure des collections que l'application créera automatiquement :

```
firestore/
├── users/
│   └── {userId}
│       ├── email: string
│       ├── displayName: string
│       ├── createdAt: timestamp
│       └── groupIds: array<string>
│
└── groups/
    └── {groupId}
        ├── name: string
        ├── createdAt: timestamp
        ├── createdBy: string
        │
        ├── members/
        │   └── {userId}
        │       ├── role: 'admin' | 'member'
        │       ├── joinedAt: timestamp
        │       └── displayName: string
        │
        ├── suggestions/
        │   └── {suggestionId}
        │       ├── title: string
        │       ├── artist: string
        │       ├── link: string
        │       ├── votes: map<userId, boolean>
        │       ├── voteCount: number
        │       ├── createdBy: string
        │       └── createdAt: timestamp
        │
        ├── songs/
        │   └── {songId}
        │       ├── title: string
        │       ├── artist: string
        │       ├── duration: number
        │       ├── structure: string
        │       ├── masteryLevels: map<userId, number> // 0-10
        │       ├── convertedFromSuggestion: string?
        │       └── addedAt: timestamp
        │
        ├── performances/
        │   └── {performanceId}
        │       ├── name: string
        │       ├── venue: string
        │       ├── date: timestamp
        │       ├── setlist: array<songId>
        │       └── createdAt: timestamp
        │
        └── invitations/
            └── {invitationId}
                ├── code: string
                ├── qrCode: string
                ├── createdBy: string
                ├── expiresAt: timestamp
                └── used: boolean
```

---

## Étape 7 : Obtenir les certificats SHA-1 (Pour Google Sign-In et P2P)

### 7.1 Debug SHA-1 (Développement)

Ouvrez un terminal dans le répertoire de votre projet et exécutez :

**Windows (PowerShell)** :
```powershell
cd android
./gradlew signingReport
```

**macOS/Linux** :
```bash
cd android
./gradlew signingReport
```

Cherchez dans la sortie la section **`Variant: debug`** et copiez la valeur **SHA1**.

### 7.2 Ajouter le SHA-1 à Firebase

1. Dans la console Firebase, cliquez sur l'icône **⚙️ Paramètres** > **Paramètres du projet**
2. Descendez jusqu'à la section **"Vos applications"**
3. Cliquez sur votre application Android
4. Dans la section **"Certificats de signature"**, collez le SHA-1
5. Cliquez sur **"Enregistrer"**

> **Important** : Vous devrez également ajouter le SHA-1 de votre certificat de release pour la production.

---

## Étape 8 : Récapitulatif des fichiers et informations

Après cette configuration, vous devez avoir :

### ✅ Fichiers téléchargés
- [x] `google-services.json` dans `androidApp/`

### ✅ Informations notées
- [x] **Package Android** : `com.bandtrack` (ou le vôtre)
- [x] **SHA-1 Debug** : (obtenu à l'étape 7)

### ✅ Services activés dans Firebase
- [x] Authentication (Email/Password)
- [x] Firestore Database
- [x] Cloud Messaging
- [x] Règles de sécurité Firestore configurées

---

## Étape 9 : Intégration dans le code (À FAIRE APRÈS)

Lors de la configuration du projet Android, vous devrez ajouter dans `build.gradle.kts` :

### `build.gradle.kts` (project-level)
```kotlin
plugins {
    id("com.google.gms.google-services") version "4.4.2" apply false
}
```

### `androidApp/build.gradle.kts`
```kotlin
plugins {
    id("com.android.application")
    id("com.google.gms.google-services")
}

dependencies {
    // Firebase BoM (Bill of Materials)
    implementation(platform("com.google.firebase:firebase-bom:33.5.1"))
    
    // Firebase services
    implementation("com.google.firebase:firebase-auth-ktx")
    implementation("com.google.firebase:firebase-firestore-ktx")
    implementation("com.google.firebase:firebase-messaging-ktx")
}
```

---

## Étape 10 : Tester la configuration

Une fois le projet Android configuré, vous pourrez tester :

1. **Test Authentication** : Créer un utilisateur test via le code
2. **Test Firestore** : Vérifier que les données apparaissent dans la console Firebase
3. **Test Notifications** : Envoyer une notification test depuis la console

Dans la console Firebase > Authentication > Users, vous verrez les utilisateurs créés.
Dans Firestore > Data, vous verrez les documents créés en temps réel.

---

## ⚠️ Points importants de sécurité

### Mode Production

Avant de publier l'application :

1. ✅ Vérifiez que les règles Firestore sont restrictives
2. ✅ Ajoutez le SHA-1 du certificat de release
3. ✅ Activez App Check pour protéger votre backend
4. ✅ Configurez des quotas dans Firebase pour éviter les abus

### Données sensibles

- ❌ Ne JAMAIS commiter `google-services.json` dans un dépôt public
- ❌ Ne JAMAIS stocker de clés API côté client
- ✅ Ajouter `google-services.json` au `.gitignore`

---

## Ressources utiles

- [Documentation Firebase Android](https://firebase.google.com/docs/android/setup)
- [Règles de sécurité Firestore](https://firebase.google.com/docs/firestore/security/get-started)
- [Firebase Authentication](https://firebase.google.com/docs/auth)
- [Firebase Console](https://console.firebase.google.com/)

---

## Support et problèmes courants

### Problème : "google-services.json not found"
**Solution** : Vérifiez que le fichier est bien dans `androidApp/` et exécutez `./gradlew clean`

### Problème : Règles Firestore refusent l'accès
**Solution** : Vérifiez dans la console Firebase > Firestore > Règles que les règles sont bien publiées

### Problème : SHA-1 invalide pour Google Sign-In
**Solution** : Régénérez le SHA-1 avec `./gradlew signingReport` et remplacez-le dans Firebase

---

**Configuration terminée !** 🎉

Vous êtes maintenant prêt à intégrer Firebase dans le code de l'application BandTrack.
