package com.bandtrack.data.repository

import com.bandtrack.data.models.User
import com.bandtrack.data.remote.FirebaseAuthService
import com.bandtrack.data.remote.FirestoreService
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Repository pour la gestion de l'authentification  
 * Couche d'abstraction entre les ViewModels et les services Firebase
 */
class AuthRepository(
    private val authService: FirebaseAuthService = FirebaseAuthService(),
    private val firestoreService: FirestoreService = FirestoreService()
) {

    /**
     * Observable de l'utilisateur connecté
     */
    val authStateFlow: Flow<User?> = authService.authStateFlow.map { firebaseUser ->
        firebaseUser?.let {
            // Récupérer les données complètes depuis Firestore
            firestoreService.getUser(it.uid).getOrNull()
        }
    }

    /**
     * ID de l'utilisateur actuel
     */
    val currentUserId: String?
        get() = authService.currentUserId

    /**
     * Vérifier si un utilisateur est connecté
     */
    fun isUserSignedIn(): Boolean = authService.isUserSignedIn()

    /**
     * Inscription avec email et mot de passe
     */
    suspend fun signUp(email: String, password: String, displayName: String): Result<User> {
        // 1. Créer le compte Firebase Auth
        val authResult = authService.signUpWithEmail(email, password, displayName)
        if (authResult.isFailure) {
            return Result.failure(authResult.exceptionOrNull()!!)
        }

        val firebaseUser = authResult.getOrNull()!!

        // 2. Créer le document utilisateur dans Firestore
        val user = User(
            id = firebaseUser.uid,
            email = email,
            displayName = displayName,
            groupIds = emptyList()
        )

        val firestoreResult = firestoreService.saveUser(user)
        if (firestoreResult.isFailure) {
            return Result.failure(firestoreResult.exceptionOrNull()!!)
        }

        return Result.success(user)
    }

    /**
     * Connexion avec email et mot de passe
     */
    suspend fun signIn(email: String, password: String): Result<User> {
        // 1. Se connecter avec Firebase Auth
        val authResult = authService.signInWithEmail(email, password)
        if (authResult.isFailure) {
            return Result.failure(authResult.exceptionOrNull()!!)
        }

        val firebaseUser = authResult.getOrNull()!!

        // 2. Récupérer les données utilisateur depuis Firestore
        return firestoreService.getUser(firebaseUser.uid)
    }

    /**
     * Déconnexion
     */
    fun signOut() {
        authService.signOut()
    }

    /**
     * Envoyer un email de réinitialisation de mot de passe
     */
    suspend fun sendPasswordResetEmail(email: String): Result<Unit> {
        return authService.sendPasswordResetEmail(email)
    }

    /**
     * Supprimer le compte
     */
    suspend fun deleteAccount(): Result<Unit> {
        val userId = currentUserId ?: return Result.failure(Exception("No user signed in"))
        
        // TODO: Supprimer aussi toutes les données utilisateur dans Firestore
        
        return authService.deleteAccount()
    }
}
