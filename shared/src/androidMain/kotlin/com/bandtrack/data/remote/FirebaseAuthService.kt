package com.bandtrack.data.remote

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.UserProfileChangeRequest
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

/**
 * Service d'authentification Firebase
 * Gère la connexion, inscription, et état de l'utilisateur
 */
class FirebaseAuthService {
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()

    /**
     * Utilisateur actuellement connecté
     */
    val currentUser: FirebaseUser?
        get() = auth.currentUser

    /**
     * ID de l'utilisateur actuellement connecté
     */
    val currentUserId: String?
        get() = currentUser?.uid

    /**
     * Observable de l'état d'authentification
     */
    val authStateFlow: Flow<FirebaseUser?> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { auth ->
            trySend(auth.currentUser)
        }
        auth.addAuthStateListener(listener)
        awaitClose { auth.removeAuthStateListener(listener) }
    }

    /**
     * Inscription avec email et mot de passe
     */
    suspend fun signUpWithEmail(
        email: String,
        password: String,
        displayName: String
    ): Result<FirebaseUser> = try {
        val result = auth.createUserWithEmailAndPassword(email, password).await()
        val user = result.user ?: throw Exception("User is null after signup")
        
        // Mettre à jour le nom d'affichage
        val profileUpdates = UserProfileChangeRequest.Builder()
            .setDisplayName(displayName)
            .build()
        user.updateProfile(profileUpdates).await()
        
        Result.success(user)
    } catch (e: Exception) {
        Result.failure(e)
    }

    /**
     * Connexion avec email et mot de passe
     */
    suspend fun signInWithEmail(email: String, password: String): Result<FirebaseUser> = try {
        val result = auth.signInWithEmailAndPassword(email, password).await()
        val user = result.user ?: throw Exception("User is null after sign in")
        Result.success(user)
    } catch (e: Exception) {
        Result.failure(e)
    }

    /**
     * Déconnexion
     */
    fun signOut() {
        auth.signOut()
    }

    /**
     * Envoyer un email de réinitialisation de mot de passe
     */
    suspend fun sendPasswordResetEmail(email: String): Result<Unit> = try {
        auth.sendPasswordResetEmail(email).await()
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    /**
     * Supprimer le compte utilisateur
     */
    suspend fun deleteAccount(): Result<Unit> = try {
        currentUser?.delete()?.await()
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    /**
     * Vérifier si un utilisateur est connecté
     */
    fun isUserSignedIn(): Boolean = currentUser != null
}
