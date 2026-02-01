# Guide de Récupération du SHA-1 pour Firebase

## 📍 Où exécuter la commande

**Répertoire exact** : `d:\Développement\bandtrack-mobile\`

C'est le **répertoire racine du projet**, là où se trouvent les fichiers :
- `gradlew.bat`
- `build.gradle.kts`
- `settings.gradle.kts`

## 💻 Commande Windows (PowerShell)

```powershell
# 1. Aller dans le répertoire du projet
cd d:\Développement\bandtrack-mobile

# 2. Exécuter la commande signingReport
.\gradlew.bat signingReport
```

## 🔍 Ce que fait cette commande

La commande `signingReport` génère un rapport de toutes les signatures de votre application, notamment :
- **Debug keystore** (keystore de développement automatique)
- **Release keystore** (si configuré)

## 📋 Interpréter la sortie

Vous verrez plusieurs sections. Cherchez celle nommée **`Variant: debug`** :

```
> Task :androidApp:signingReport
Variant: debug
Config: debug
Store: C:\Users\VotreNom\.android\debug.keystore
Alias: AndroidDebugKey
MD5: 12:34:56:78:90:AB:CD:EF:12:34:56:78:90:AB:CD:EF
SHA1: AA:BB:CC:DD:EE:FF:00:11:22:33:44:55:66:77:88:99:AA:BB:CC:DD
SHA-256: 11:22:33:44:55:66:77:88:99:AA:BB:CC:DD:EE:FF:00:11:22:33:44:55:66:77:88:99:AA:BB:CC:DD:EE:FF:00:11

Variant: release
Config: none
...
```

## ✅ Que copier

Copiez la ligne **SHA1** de la section `Variant: debug` :

```
SHA1: AA:BB:CC:DD:EE:FF:00:11:22:33:44:55:66:77:88:99:AA:BB:CC:DD
```

## 🔐 Pourquoi c'est nécessaire

Le SHA-1 est requis pour :
1. **Google Sign-In** : Authentification Google
2. **Android Nearby Connections** : Partage P2P sécurisé
3. **Firebase Dynamic Links** : Liens d'invitation profonds

Sans ce certificat dans Firebase, ces fonctionnalités ne fonctionneront pas.

## 📝 Étape suivante dans Firebase

1. Allez dans **Console Firebase** > **⚙️ Paramètres** > **Paramètres du projet**
2. Section **"Vos applications"** > Cliquez sur votre app Android **BandTrack**
3. Section **"Certificats de signature"**
4. Collez le SHA-1 dans le champ
5. Cliquez sur **"Enregistrer"**

## ⚠️ Problèmes courants

### "gradlew.bat n'est pas reconnu"
**Cause** : Vous n'êtes pas dans le bon répertoire

**Solution** :
```powershell
cd d:\Développement\bandtrack-mobile
# Vérifiez que vous êtes au bon endroit
ls gradlew.bat
```

### "Could not find or load main class"
**Cause** : Wrapper Gradle manquant ou incomplet

**Solution** : Le wrapper a maintenant été configuré automatiquement

### "ANDROID_HOME not set"
**Cause** : Android SDK n'est pas configuré

**Solution** :
1. Installer Android Studio
2. Ouvrir le projet dans Android Studio (File > Open)
3. Laisser Android Studio configurer le SDK
4. Relancer la commande

## 🎯 Résumé

**Commande complète** :
```powershell
cd d:\Développement\bandtrack-mobile
.\gradlew.bat signingReport
```

**Cherchez** : Section `Variant: debug`  
**Copiez** : La ligne `SHA1: ...`  
**Collez dans** : Console Firebase > Paramètres du projet > Certificats de signature
