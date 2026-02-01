package com.bandtrack

import android.app.Application
import com.google.firebase.FirebaseApp

/**
 * Application principale de BandTrack
 * Initialisation Firebase et autres services globaux
 */
class BandTrackApplication : Application() {
    
    override fun onCreate() {
        super.onCreate()
        
        // Initialisation Firebase
        FirebaseApp.initializeApp(this)
        
        // TODO: Initialiser Room Database
        // TODO: Initialiser WorkManager pour la synchronisation
    }
}
