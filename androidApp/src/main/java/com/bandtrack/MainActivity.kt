package com.bandtrack

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.bandtrack.data.models.Group
import com.bandtrack.ui.auth.AuthViewModel
import com.bandtrack.ui.auth.LoginScreen
import com.bandtrack.ui.auth.RegisterScreen
import com.bandtrack.ui.groups.GroupSelectorScreen
import com.bandtrack.ui.profile.ProfileScreen
import com.bandtrack.ui.theme.BandTrackTheme

/**
 * Activité principale de BandTrack
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Planifier la synchronisation périodique
        com.bandtrack.workers.SyncWorker.enqueuePeriodicSync(this)
        
        setContent {
            var isDarkTheme by remember { mutableStateOf(false) }
            val systemDark = androidx.compose.foundation.isSystemInDarkTheme()
            LaunchedEffect(Unit) { isDarkTheme = systemDark }

            BandTrackTheme(darkTheme = isDarkTheme) {
                BandTrackApp(
                    isDarkTheme = isDarkTheme,
                    onThemeChange = { isDarkTheme = it }
                )
            }
        }
    }
}

@Composable
fun BandTrackApp(
    authViewModel: AuthViewModel = viewModel(),
    isDarkTheme: Boolean = false,
    onThemeChange: (Boolean) -> Unit = {}
) {
    val currentUser by authViewModel.currentUser.collectAsState()
    var selectedGroup by remember { mutableStateOf<Group?>(null) }
    var currentScreen by remember { mutableStateOf<Screen>(Screen.Login) }
    var livePerformanceId by remember { mutableStateOf<String?>(null) }
    
    // Initialisation DB Locale
    val context = androidx.compose.ui.platform.LocalContext.current
    val database = remember { com.bandtrack.data.local.AppDatabase.getDatabase(context) }
    val songRepository = remember { 
        com.bandtrack.data.repository.SongRepository(
            context, 
            database.songDao(), 
            database.pendingActionDao()
        ) 
    }
    val suggestionRepository = remember { 
        com.bandtrack.data.repository.SuggestionRepository(
            context, 
            database.suggestionDao(), 
            database.pendingActionDao()
        ) 
    }
    val performanceRepository = remember { 
        com.bandtrack.data.repository.PerformanceRepository(
            context, 
            database.performanceDao(), 
            database.pendingActionDao()
        ) 
    }
    
    // Moniteur réseau
    val networkMonitor = remember { com.bandtrack.data.network.NetworkMonitor(context) }
    val isOnline by networkMonitor.isOnline.collectAsState()


    Box(modifier = Modifier.fillMaxSize()) {
        // Image de fond
        Image(
            painter = painterResource(id = R.drawable.app_background),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
            alpha = 0.25f // Très subtil pour ne pas gêner la lisibilité
        )

        Surface(
            modifier = Modifier.fillMaxSize(),
            color = Color.Transparent // On laisse voir l'image par transparence
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
                        },
                        userName = currentUser?.displayName ?: "Utilisateur"
                    )
                }
                // Si connecté et groupe sélectionné
                else -> {
                    when (currentScreen) {
                        Screen.Settings -> {
                            com.bandtrack.ui.settings.SettingsScreen(
                                isDarkTheme = isDarkTheme,
                                onThemeChange = onThemeChange,
                                onNavigateBack = { currentScreen = Screen.Profile }
                            )
                        }
                        Screen.Profile -> {
                             com.bandtrack.ui.profile.ProfileScreen(
                                onNavigateBack = { currentScreen = Screen.Home },
                                onNavigateToSettings = { currentScreen = Screen.Settings },
                                onLogout = {
                                    authViewModel.signOut()
                                    selectedGroup = null
                                    currentScreen = Screen.Login
                                }
                            )
                        }
                        Screen.LiveMode -> {
                            if (livePerformanceId != null && currentUser != null) {
                                com.bandtrack.ui.performance.LiveModeScreen(
                                    performanceId = livePerformanceId!!,
                                    groupId = selectedGroup!!.id,
                                    performanceRepository = performanceRepository,
                                    songRepository = songRepository,
                                    onExit = { 
                                        livePerformanceId = null
                                        currentScreen = Screen.Home 
                                    }
                                )
                            } else {
                                currentScreen = Screen.Home
                            }
                        }
                        else -> {
                            MainContent(
                                group = selectedGroup!!,
                                isOnline = isOnline,
                                onChangeGroup = {
                                    selectedGroup = null
                                    currentScreen = Screen.GroupSelector
                                },
                                onNavigateToProfile = {
                                    currentScreen = Screen.Profile
                                },
                                onStartLiveMode = { performanceId ->
                                    livePerformanceId = performanceId
                                    currentScreen = Screen.LiveMode
                                },
                                songRepository = songRepository,
                                suggestionRepository = suggestionRepository,
                                performanceRepository = performanceRepository
                            )
                        }
                    }
                }
            }
        }
    }
}

enum class Screen {
    Login,
    Register,
    GroupSelector,
    Home,
    Profile,
    Settings,
    LiveMode
}

@Composable
fun MainContent(
    group: Group,
    isOnline: Boolean,
    onChangeGroup: () -> Unit,
    onNavigateToProfile: () -> Unit,
    onStartLiveMode: (String) -> Unit,
    songRepository: com.bandtrack.data.repository.SongRepository,
    suggestionRepository: com.bandtrack.data.repository.SuggestionRepository,
    performanceRepository: com.bandtrack.data.repository.PerformanceRepository
) {
    // Delegate to HomeScreen with offline banner
    Column(modifier = Modifier.fillMaxSize()) {
        // Bandeau Mode Hors-Ligne
        AnimatedVisibility(
            visible = !isOnline,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.errorContainer)
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    Icons.Default.WifiOff,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Mode hors-ligne — les modifications seront synchronisées au retour du réseau",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    textAlign = TextAlign.Center
                )
            }
        }
        
        HomeScreen(
            group = group,
            onChangeGroup = onChangeGroup,
            onNavigateToProfile = onNavigateToProfile,
            onStartLiveMode = onStartLiveMode,
            songRepository = songRepository,
            suggestionRepository = suggestionRepository,
            performanceRepository = performanceRepository
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    group: Group,
    onChangeGroup: () -> Unit,
    onNavigateToProfile: () -> Unit,
    onStartLiveMode: (String) -> Unit,
    songRepository: com.bandtrack.data.repository.SongRepository,
    suggestionRepository: com.bandtrack.data.repository.SuggestionRepository,
    performanceRepository: com.bandtrack.data.repository.PerformanceRepository
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
            factory = com.bandtrack.ui.viewmodels.AudioNoteViewModelFactory(context, audioNoteRepository, songRepository)
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
                        IconButton(onClick = onNavigateToProfile) {
                            Icon(Icons.Default.Person, contentDescription = "Mon Profil")
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
                    NavigationBarItem(
                        icon = { Icon(Icons.Default.Event, contentDescription = null) },
                        label = { Text("Planning") },
                        selected = selectedTab == 2,
                        onClick = { selectedTab = 2 }
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
                                userName = currentUser!!.displayName,
                                songRepository = songRepository,
                                suggestionRepository = suggestionRepository
                            )
                        }
                    }
                    1 -> {
                        if (currentUser != null) {
                            com.bandtrack.ui.repertoire.RepertoireScreen(
                                groupId = group.id,
                                userId = currentUser!!.id,
                                songRepository = songRepository,
                                onNavigateToAudioNotes = { songId ->
                                    selectedSongId = songId
                                    selectedSongTitle = "Morceau" 
                                }
                            )
                        }
                    }
                    2 -> {
                        if (currentUser != null) {
                            com.bandtrack.ui.performance.PerformanceScreen(
                                groupId = group.id,
                                userId = currentUser!!.id,
                                performanceRepository = performanceRepository,
                                songRepository = songRepository,
                                onStartLiveMode = onStartLiveMode
                            )
                        }
                    }
                }
            }
        }
    }
}
