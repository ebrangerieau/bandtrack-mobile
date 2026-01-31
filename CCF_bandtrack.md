# BandTrack – Cahier des charges (Version 5 - Native Mobile)

## 1. Présentation du projet

BandTrack évolue d'une solution web vers une application mobile native (Android en priorité) conçue pour la mobilité extrême des musiciens. L'objectif est de fournir un outil capable de fonctionner dans l'isolement des studios de répétition (mode hors-ligne) tout en offrant une collaboration fluide via le cloud pour la planification et le répertoire.

Cette version 5 introduit la gestion multi-groupes, permettant à un musicien de piloter l'ensemble de ses projets artistiques depuis un compte unique.

---

## 2. Architecture technique moderne

### 2.1 Socle technologique

* **Langage :** Kotlin.
* **Framework UI :** Jetpack Compose (Android) avec une structure **Kotlin Multiplatform (KMP)** pour assurer une portabilité rapide vers iOS sans réécriture de la logique métier.
* **Architecture :** MVVM (Model-View-ViewModel) pour une séparation nette entre l'interface et les données.

### 2.2 Backend et Synchronisation (Cloud Hybrid)

* **Collaboration (Données textuelles) :** Intégration de **Firebase Firestore** pour la synchronisation en temps réel des suggestions, votes, listes de morceaux et calendriers.
* **Persistance locale :** Utilisation de la base de données **Room** pour garantir un accès instantané aux données même sans connexion internet (mode "Studio").

### 2.3 Gestion des médias (Audio Local-First)

Contrairement aux versions précédentes, aucun fichier audio n'est stocké sur un serveur centralisé.

* **Stockage :** Les notes vocales et fichiers audio sont stockés sur l'espace local de l'appareil.
* **Partage :** Le partage entre membres s'effectue via des protocoles **Peer-to-Peer (P2P)** comme *Android Nearby Connections* ou par l'intermédiaire d'un service de stockage personnel tiers (Google Drive/Dropbox) lié par l'utilisateur, préservant ainsi la confidentialité et les coûts d'hébergement.

---

## 3. Identité et Gestion Multi-Groupes

### 3.1 Authentification

* Gestion via **Firebase Auth** (E-mail/Mot de passe ou SSO).
* **Authentification biométrique :** Utilisation de l'API `BiometricPrompt` d'Android pour sécuriser l'accès à l'application et aux fonctions d'administration.

### 3.2 Structure "Multi-Groupes"

L'application permet à un utilisateur unique de naviguer entre plusieurs espaces de travail (Groupes).

* **Sélecteur de contexte :** Un menu permet de basculer instantanément d'un groupe à l'autre sans se déconnecter.
* **Création d'espace :** Tout utilisateur peut créer un nouveau groupe et en devenir l'administrateur par défaut.

### 3.3 Système d'invitation

Le recrutement de nouveaux membres ne repose plus sur une création de compte manuelle par l'admin :

* **Code unique :** Génération d'un code alphanumérique à partager.
* **QR Code :** Un membre peut scanner le QR Code sur le téléphone de l'administrateur pour rejoindre instantanément le groupe.
* **Lien dynamique :** Envoi d'un lien d'invitation par messagerie.

---

## 4. Fonctionnalités Métier (Adaptation Mobile)

### 4.1 Suggestions et Répertoire

* **Interface tactile :** Système de vote "au pouce" optimisé pour l'usage à une main.
* **Liens externes :** Ouverture directe des liens YouTube/Spotify dans leurs applications respectives.
* **Conversion :** Transformation d'une suggestion en "Morceau en cours" en un clic, avec transfert des métadonnées.

### 4.2 Suivi de répétition et Progrès

* **Slider de maîtrise :** Curseur tactile (0 à 10) pour évaluer son niveau sur chaque titre.
* **Notes Audio :** Enregistreur natif intégré. Les fichiers sont indexés localement et associés au morceau.
* **Confidentialité :** Les notes personnelles restent stockées localement sur le téléphone par défaut, sauf demande de partage explicite en P2P.

### 4.3 Planification des prestations

* Gestion des dates passées et à venir avec notifications push système pour les rappels de concerts.
* **Accès rapide :** Depuis une prestation, accès direct aux structures des morceaux et notes associées pour une révision de dernière minute en coulisses.

---

## 5. Interface Utilisateur (UX)

### 5.1 Ergonomie Native

* **Navigation :** Barre de navigation fixe en bas (Bottom Navigation) pour les sections principales : *Suggestions, Répertoire, Prestations, Profil/Paramètres*.
* **Mode Sombre :** Design "High Contrast" pour une lisibilité maximale sur scène ou en studio sombre.

### 5.2 Performance hors-ligne

* Indicateur visuel de synchronisation (Cloud vs Local).
* Possibilité de modifier n'importe quelle donnée hors-ligne avec résolution de conflit automatique lors du retour du réseau.

---

## 6. Sécurité et Confidentialité

* **Chiffrement :** Les données locales sensibles peuvent être chiffrées via les *EncryptedSharedPreferences* d'Android.
* **Rôles :** Permissions gérées via Firebase Rules pour s'assurer qu'un utilisateur n'accède qu'aux données des groupes auxquels il appartient.

---
