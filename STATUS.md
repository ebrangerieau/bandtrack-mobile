# 📊 État du Projet BandTrack Mobile

**Dernière mise à jour** : 2026-02-01

## ✅ Phases Complétées

### Phase 1 : Infrastructure ✅
- [x] Configuration Kotlin Multiplatform (KMP)
- [x] Configuration Firebase (Auth + Firestore)
- [x] Architecture MVVM mise en place
- [x] Modèles de base (User, Group, InvitationCode)

### Phase 2 : Authentification & Groupes ✅
- [x] FirebaseAuthService
- [x] FirestoreService
- [x] AuthRepository & GroupRepository
- [x] AuthViewModel & GroupSelectorViewModel
- [x] LoginScreen & RegisterScreen
- [x] GroupSelectorScreen
- [x] Système d'invitation par code

### Phase 3 : Fonctionnalités Métier Core ✅
- [x] **Modèles de données**
  - Suggestion (avec système de votes)
  - Song (répertoire avec niveaux de maîtrise 0-10)
  - AudioNote (notes audio locales)
  
- [x] **Repositories**
  - SuggestionRepository (CRUD + votes + temps réel)
  - SongRepository (CRUD + maîtrise + temps réel)
  
- [x] **ViewModels**
  - SuggestionsViewModel
  - RepertoireViewModel
  
- [x] **Interface utilisateur**
  - SuggestionsScreen (liste, votes, ajout, conversion)
  - RepertoireScreen (liste, slider 0-10, détails)
  - Bottom Navigation (Suggestions / Répertoire)
  
- [x] **Fonctionnalités clés**
  - Système de vote pour suggestions
  - Conversion suggestion → morceau
  - Slider de maîtrise personnel (0-10)
  - Calcul de maîtrise moyenne du groupe
  - Badge visuel pour morceaux bien maîtrisés (≥7/10)
  - Synchronisation temps réel Firebase

## 🔧 Corrections Techniques Récentes

- ✅ Remplacement de `System.currentTimeMillis()` par `0L` dans commonMain
- ✅ Ajout des imports manquants (Icons)
- ✅ Correction du `.gitignore` (gradle-temp/, gradle-wrapper.zip)
- ✅ Nettoyage des fichiers volumineux du versioning Git

## 🚧 En Cours

### ✅ Compilation - RÉSOLU
- ✅ Projet déplacé vers `D:\developpement\bandtrack-mobile` (sans accents)
- ✅ Correction de l'erreur de syntaxe dans `SongRepository.kt` (ligne 109)
- ✅ Build réussi - APK généré avec succès

### ✅ Runtime - RÉSOLU
- ✅ Mode Hors Ligne (Lecture) : Intégration Room Database
- ✅ Correction conflit getters `getRole` sur `GroupMember` (renommé en `toRoleEnum`)
- ✅ Optimisation des requêtes Firestore (ajout `memberIds` au modèle Group)
- ✅ Création du fichier `firestore.rules` optimisé
- ✅ Ajout de la configuration d'instrument personnalisée (Tonalité, Capo...)
- ✅ Affichage en **gras** de la config perso sur la fiche morceau
- ✅ Ajout des notes personnelles (Mémos privés) par morceau

## 📋 Prochaines Étapes Suggérées

### Phase 3.1 : Notes Audio 🎤 ✅ (Terminé 2026-02-01)
- [x] Service d'enregistrement audio Android (`AudioRecorderService`)
- [x] Service de lecture audio Android (`AudioPlayerService`)
- [x] Repository (`AudioNoteRepository`) avec fichiers locaux
- [x] ViewModel (`AudioNoteViewModel`)
- [x] UI complète (`AudioNotesScreen`) avec enregistrement, lecture, suppression
- [x] Intégration depuis le menu du Répertoire

**Technologies** : MediaRecorder, MediaPlayer, File Storage, Kotlin Coroutines

### Phase 4 : Planification & Prestations 📅 ✅ (Terminé 2026-02-01)
- [x] Modèle de données `Performance` (Concert, Répétition)
- [x] Repository Firestore (`PerformanceRepository`)
- [x] ViewModel (`PerformanceViewModel`)
- [x] UI Liste des événements (À venir / Passés)
- [x] UI Ajout/Suppression d'événement
- [x] Gestion des Setlists (Sélection de morceaux pour un événement)
- [x] Éditeur de Setlist (Drag & Drop simplifié, Suppression)
- [ ] Notifications push (reporté à plus tard)

**Technologies** : Firestore, DatePicker, TabRow, Custom Setlist Editor

### Phase 5 : Améliorations UX 🎨 ✅ (Terminé 2026-02-01)
- [x] Recherche/filtrage dans le répertoire (Titre, Artiste)
- [x] Tri des morceaux (Titre, Artiste, Maîtrise)
- [x] Statistiques du groupe (Maîtrise globale, Tops/Flops)
- [x] Interface améliorée avec Material 3
- [ ] Profil utilisateur (Reporté)
- [ ] Paramètres de l'application (Reporté)

**Technologies** : SearchBar, Sort Logic, Data Visualization (Basic Cards)

### Phase 6 : Tests & Documentation 📝 (Partiel)
- [x] Mise à jour du README.md (Architecture, Roadmap à jour)
- [x] Refactoring pour testabilité (Repository `open`)
- [ ] Tests Unitaires (ViewModel) - *Bloqué par config Gradle*
- [ ] Tests d'intégration

**Technologies** : Markdown, JUnit



### Phase 7 : Mode Hors Ligne & Synchronisation 📡
- [ ] **Persistance Locale** (Room Database)
- [ ] **Cache** pour les données Firestore (Groupes, Chansons, Events)
- [ ] **Synchronisation** (Worker Manager pour l'upload différé)
- [ ] **Gestion des conflits** simple

**Technologies** : Room, WorkManager, SQLDelight (optionnel pour KMP)

## 🎯 Objectifs à Long Terme

- [ ] **Phase 8** : Partage P2P (Fichiers audio lourds)
- [ ] **Phase 9** : Intégrations externes (Spotify, YouTube)
- [ ] **Phase 10** : Version iOS (grâce à KMP)
- [ ] **Phase 11** : Publication sur Play Store

## 📊 Métriques du Projet

- **Fichiers Kotlin** : ~28
- **Modèles de données** : 6 (User, Group, InvitationCode, Suggestion, Song, AudioNote)
- **Repositories** : 5 (Auth, Group, Suggestion, Song, AudioNote)
- **ViewModels** : 5 (Auth, Group, Suggestions, Repertoire, AudioNote)
- **Écrans UI** : 6 (Login, Register, Group, Suggestions, Repertoire, AudioNotes)
- **Lignes de code** : ~3500+

## 🔒 Sécurité

- ✅ Règles Firestore configurées
- ✅ Authentification Firebase
- ✅ Validation côté serveur
- ⚠️ À faire : Chiffrement des données sensibles
- ⚠️ À faire : Rate limiting

## 🐛 Problèmes Connus

1. **Timestamps** : Les timestamps sont à 0L, à définir lors de la création côté Repository
2. **Tests** : Aucun test automatisé pour le moment

## 💡 Notes

- Le projet utilise **Kotlin Multiplatform** pour une future compatibilité iOS
- **Material Design 3** pour une UI moderne
- **Firebase** pour le backend (Auth + Firestore)
- **Synchronisation temps réel** avec Flow et Firestore listeners

---

## 🎵 Roadmap & Statut Global

- [x] **Phase 1-2** : Infra & Auth
- [x] **Phase 3** : Répertoire & Suggestions
- [x] **Phase 4** : Planification (Events)
- [x] **Phase 5** : UX (Tri, Recherche)
- [x] **Phase 6** : Tests & Docs (Partiel)
- [ ] **Phase 7** : Mode Hors Ligne (Room)
- [ ] **Phase 8** : Partage P2P
- [ ] **Phase 9** : Version iOS

