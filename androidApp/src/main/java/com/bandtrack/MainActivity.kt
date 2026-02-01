package com.bandtrack

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.bandtrack.data.models.Group
import com.bandtrack.ui.auth.AuthViewModel
import com.bandtrack.ui.auth.LoginScreen
import com.bandtrack.ui.auth.RegisterScreen
import com.bandtrack.ui.groups.GroupSelectorScreen
import com.bandtrack.ui.theme.BandTrackTheme

/**
 * Activité principale de BandTrack
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            BandTrackTheme {
                BandTrackApp()
            }
        }
    }
}

@Composable
fun BandTrackApp(
    authViewModel: AuthViewModel = viewModel()
) {
    val currentUser by authViewModel.currentUser.collectAsState()
    var selectedGroup by remember { mutableStateOf<Group?>(null) }
    var currentScreen by remember { mutableStateOf<Screen>(Screen.Login) }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        when {
            // Si pas connecté, afficher login/register
            currentUser == null -> {
                when (currentScreen) {
                    Screen.Login -> {
                        LoginScreen(
                            onLoginSuccess = {
                                currentScreen = Screen.GroupSelector
                            },
                            onNavigateToRegister = {
                                currentScreen = Screen.Register
                            }
                        )
                    }
                    Screen.Register -> {
                        RegisterScreen(
                            onRegisterSuccess = {
                                currentScreen = Screen.GroupSelector
                            },
                            onNavigateToLogin = {
                                currentScreen = Screen.Login
                            }
                        )
                    }
                    else -> {}
                }
            }
            // Si connecté mais pas de groupe sélectionné
            selectedGroup == null -> {
                GroupSelectorScreen(
                    onGroupSelected = { group ->
                        selectedGroup = group
                        currentScreen = Screen.Home
                    },
                    onLogout = {
                        authViewModel.signOut()
                        selectedGroup = null
                        currentScreen = Screen.Login
                    }
                )
            }
            // Si connecté et groupe sélectionné
            else -> {
                HomeScreen(
                    group = selectedGroup!!,
                    onChangeGroup = {
                        selectedGroup = null
                        currentScreen = Screen.GroupSelector
                    },
                    onLogout = {
                        authViewModel.signOut()
                        selectedGroup = null
                        currentScreen = Screen.Login
                    }
                )
            }
        }
    }
}

enum class Screen {
    Login,
    Register,
    GroupSelector,
    Home
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    group: Group,
    onChangeGroup: () -> Unit,
    onLogout: () -> Unit
) {
    var selectedTab by remember { mutableStateOf(0) }
    val authViewModel: AuthViewModel = viewModel()
    val currentUser by authViewModel.currentUser.collectAsState()

    // États pour la navigation vers les notes audio
    var selectedSongId by remember { mutableStateOf<String?>(null) }
    var selectedSongTitle by remember { mutableStateOf("") }
    
    // Contexte et Repository pour AudioNotes
    val context = androidx.compose.ui.platform.LocalContext.current
    val audioNoteRepository = remember { com.bandtrack.data.repository.AudioNoteRepository(context) }
    
    // Gestion du bouton retour
    androidx.activity.compose.BackHandler(enabled = selectedSongId != null) {
        selectedSongId = null
    }

    if (selectedSongId != null && currentUser != null) {
        val audioNoteViewModel: com.bandtrack.ui.viewmodels.AudioNoteViewModel = viewModel(
            factory = com.bandtrack.ui.viewmodels.AudioNoteViewModelFactory(context, audioNoteRepository)
        )
        
        com.bandtrack.ui.screens.AudioNotesScreen(
            songId = selectedSongId!!,
            groupId = group.id,
            userId = currentUser!!.id,
            songTitle = selectedSongTitle,
            viewModel = audioNoteViewModel,
            onNavigateBack = { selectedSongId = null }
        )
    } else {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { 
                        Column {
                            Text(
                                text = "BandTrack",
                                style = MaterialTheme.typography.titleLarge
                            )
                            Text(
                                text = group.name,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    },
                    actions = {
                        IconButton(onClick = onChangeGroup) {
                            Icon(Icons.Default.SwapHoriz, contentDescription = "Changer de groupe")
                        }
                        IconButton(onClick = onLogout) {
                            Icon(Icons.Default.ExitToApp, contentDescription = "Déconnexion")
                        }
                    }
                )
            },
            bottomBar = {
                NavigationBar {
                    NavigationBarItem(
                        icon = { Icon(Icons.Default.Lightbulb, contentDescription = null) },
                        label = { Text("Suggestions") },
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 }
                    )
                    NavigationBarItem(
                        icon = { Icon(Icons.Default.MusicNote, contentDescription = null) },
                        label = { Text("Répertoire") },
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 }
                    )
                }
            }
        ) { padding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                when (selectedTab) {
                    0 -> {
                        if (currentUser != null) {
                            com.bandtrack.ui.suggestions.SuggestionsScreen(
                                groupId = group.id,
                                userId = currentUser!!.id,
                                userName = currentUser!!.displayName
                            )
                        }
                    }
                    1 -> {
                        if (currentUser != null) {
                            // Nous avons besoin de récupérer le titre du morceau depuis l'ID dans le callback
                            // Pour simplifier ici, nous allons juste passer l'ID et RepertoireScreen devra peut-être changer
                            // Ou mieux, on change le callback de RepertoireScreen pour passer le titre aussi
                            
                            // Petite astuce : on va modifier RepertoireScreen pour qu'il nous file le titre
                            // Mais comme je ne peux pas modifier RepertoireScreen instantanément ici sans recompiler,
                            // je vais devoir adapter RepertoireScreen juste après.
                            
                            // Pour l'instant on suppose que RepertoireScreen va être adapté
                            com.bandtrack.ui.repertoire.RepertoireScreen(
                                groupId = group.id,
                                userId = currentUser!!.id,
                                onNavigateToAudioNotes = { songId ->
                                    selectedSongId = songId
                                    // Idéalement on récupérerait le titre ici, mais on va le faire "lazy"
                                    // ou modifier RepertoireScreen pour passer le titre.
                                    // Pour l'instant mettons un placeholder qui sera mis à jour
                                    selectedSongTitle = "Morceau" 
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
