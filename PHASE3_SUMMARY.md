# Phase 3 - Fonctionnalités Métier Core ✅

## Résumé des implémentations

### 📦 Modèles de données créés

1. **`Suggestion.kt`** - Suggestions de morceaux avec système de votes
   - Titre, artiste, lien (YouTube/Spotify)
   - Map de votes (userId -> boolean)
   - Compteur de votes
   - Statut (PENDING, ACCEPTED, REJECTED)
   - Méthode `toggleVote()` pour voter/dé-voter

2. **`Song.kt`** - Morceaux du répertoire
   - Informations musicales (titre, artiste, durée, structure, tonalité, tempo)
   - Map de niveaux de maîtrise (userId -> niveau 0-10)
   - Méthodes pour gérer les niveaux de maîtrise
   - Calcul de la moyenne de maîtrise du groupe
   - Conversion depuis une suggestion

3. **`AudioNote.kt`** - Notes audio locales
   - Métadonnées (titre, description, durée)
   - Chemin du fichier local
   - Formatage de la durée et taille du fichier

### 🗄️ Repositories créés

1. **`SuggestionRepository.kt`**
   - CRUD complet pour les suggestions
   - Observable temps réel avec Flow
   - Système de vote avec transactions Firestore
   - Conversion en morceau du répertoire

2. **`SongRepository.kt`**
   - CRUD complet pour les morceaux
   - Observable temps réel avec Flow
   - Mise à jour des niveaux de maîtrise avec transactions
   - Création depuis une suggestion

### 🎨 ViewModels créés

1. **`SuggestionsViewModel.kt`**
   - États UI (Loading, Success, Error)
   - Observation temps réel des suggestions
   - Création de suggestions
   - Système de vote
   - Conversion suggestion → morceau
   - Suppression de suggestions

2. **`RepertoireViewModel.kt`**
   - États UI (Loading, Success, Error)
   - Observation temps réel des morceaux
   - Création de morceaux
   - Mise à jour du niveau de maîtrise personnel
   - Filtrage des morceaux bien maîtrisés (≥7/10)

### 🖼️ Écrans UI créés

1. **`SuggestionsScreen.kt`**
   - Liste des suggestions avec votes
   - Bouton de vote tactile (pouce levé)
   - Dialogue d'ajout de suggestion
   - Menu contextuel (convertir, supprimer)
   - Indicateur visuel du nombre de votes
   - Vue vide avec incitation à l'action

2. **`RepertoireScreen.kt`**
   - Liste des morceaux du répertoire
   - **Slider de maîtrise 0-10** pour chaque morceau
   - Affichage de la maîtrise moyenne du groupe
   - Badge visuel pour morceaux bien maîtrisés (✓)
   - Dialogue d'ajout de morceau (complet)
   - Dialogue de détails du morceau
   - Chips pour tonalité et tempo
   - Vue vide avec incitation à l'action

### 🎯 Navigation mise à jour

**`MainActivity.kt`** - Bottom Navigation ajoutée
- Onglet "Suggestions" (icône ampoule)
- Onglet "Répertoire" (icône note de musique)
- TopBar avec nom du groupe et actions (changer groupe, déconnexion)

## 🔒 Sécurité Firestore

Les règles de sécurité sont déjà configurées dans `FIREBASE_SETUP.md` :
- Seuls les membres du groupe peuvent lire/créer/modifier suggestions et morceaux
- Les créateurs peuvent supprimer leurs propres suggestions
- Seuls les admins peuvent supprimer des morceaux du répertoire

## ✅ Fonctionnalités implémentées

- [x] Module Suggestions et votes
- [x] Module Répertoire
- [x] Conversion suggestion → morceau
- [x] Slider de maîtrise (0-10)
- [ ] Enregistreur de notes audio (à implémenter en Phase 3.1)

## 🚀 Prochaines étapes

### Phase 3.1 : Notes Audio (optionnel)
- Service d'enregistrement audio Android
- Repository pour AudioNote
- UI d'enregistrement et lecture
- Stockage local des fichiers

### Phase 4 : Planification et Prestations
- Modèle Performance
- Calendrier des concerts
- Notifications push

## 📝 Notes techniques

**Synchronisation temps réel** : Toutes les données utilisent des Flow avec `observeGroupSuggestions()` et `observeGroupSongs()` pour une mise à jour automatique de l'UI.

**Transactions Firestore** : Les votes et niveaux de maîtrise utilisent des transactions pour éviter les conflits en cas de modifications simultanées.

**Material 3** : Tous les écrans utilisent Material Design 3 avec des couleurs thématiques cohérentes.
