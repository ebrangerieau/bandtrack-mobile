package com.bandtrack.ui.repertoire

import androidx.compose.foundation.layout.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import com.bandtrack.ui.repertoire.SortOption
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.bandtrack.data.models.Song
import com.bandtrack.data.repository.SongRepository
import kotlin.math.roundToInt
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RepertoireScreen(
    groupId: String,
    userId: String,
    songRepository: SongRepository,
    onNavigateToAudioNotes: (String) -> Unit
) {
    val viewModel: RepertoireViewModel = viewModel(
        factory = RepertoireViewModelFactory(songRepository)
    )
    val uiState by viewModel.uiState.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val sortOption by viewModel.sortOption.collectAsState()
    
    var showAddDialog by remember { mutableStateOf(false) }
    var showSortMenu by remember { mutableStateOf(false) }
    var showStats by remember { mutableStateOf(false) }

    LaunchedEffect(groupId) {
        viewModel.initialize(groupId, userId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Répertoire") },
                actions = {
                    IconButton(onClick = { showStats = true }) {
                        Icon(Icons.Default.Star, contentDescription = "Statistiques")
                    }
                    IconButton(onClick = { showSortMenu = true }) {
                        Icon(Icons.Default.Sort, contentDescription = "Trier")
                    }
                    DropdownMenu(
                        expanded = showSortMenu,
                        onDismissRequest = { showSortMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Titre") },
                            onClick = { 
                                viewModel.onSortOptionChanged(SortOption.TITLE)
                                showSortMenu = false
                            },
                            trailingIcon = { if (sortOption == SortOption.TITLE) Icon(Icons.Default.Check, null) }
                        )
                        DropdownMenuItem(
                            text = { Text("Artiste") },
                            onClick = { 
                                viewModel.onSortOptionChanged(SortOption.ARTIST)
                                showSortMenu = false
                            },
                            trailingIcon = { if (sortOption == SortOption.ARTIST) Icon(Icons.Default.Check, null) }
                        )
                        HorizontalDivider()
                        DropdownMenuItem(
                            text = { Text("Maîtrise -") },
                            onClick = { 
                                viewModel.onSortOptionChanged(SortOption.MASTERY_ASC)
                                showSortMenu = false
                            },
                            trailingIcon = { if (sortOption == SortOption.MASTERY_ASC) Icon(Icons.Default.Check, null) }
                        )
                        DropdownMenuItem(
                            text = { Text("Maîtrise +") },
                            onClick = { 
                                viewModel.onSortOptionChanged(SortOption.MASTERY_DESC)
                                showSortMenu = false
                            },
                            trailingIcon = { if (sortOption == SortOption.MASTERY_DESC) Icon(Icons.Default.Check, null) }
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onSecondaryContainer
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true }
            ) {
                Icon(Icons.Default.Add, contentDescription = "Ajouter un morceau")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = viewModel::onSearchQueryChanged,
                placeholder = { Text("Rechercher (titre, artiste)...") },
                leadingIcon = { Icon(Icons.Default.Search, null) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                singleLine = true,
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.onSearchQueryChanged("") }) {
                            Icon(Icons.Default.Clear, "Effacer")
                        }
                    }
                }
            )

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f)
            ) {
                when (val state = uiState) {
                    is RepertoireUiState.Loading -> {
                        CircularProgressIndicator(
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }
                    is RepertoireUiState.Success -> {
                        if (state.songs.isEmpty()) {
                            if (searchQuery.isNotEmpty()) {
                                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    Text("Aucun résultat pour \"$searchQuery\"")
                                }
                            } else {
                                EmptyRepertoireView(
                                    onAddClick = { showAddDialog = true }
                                )
                            }
                        } else {
                            SongsList(
                                songs = state.songs,
                                currentUserId = userId,
                                onMasteryChange = viewModel::updateMyMasteryLevel,
                                onUpdatePersonalConfig = viewModel::updatePersonalConfig,
                                onUpdatePersonalNotes = viewModel::updatePersonalNotes,
                                onUpdateSong = viewModel::updateSong,
                                onNavigateToAudioNotes = onNavigateToAudioNotes,
                                onDelete = viewModel::deleteSong
                            )
                        }
                    }
                    is RepertoireUiState.Error -> {
                        ErrorView(
                            message = state.message,
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AddSongDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { title, artist, duration, structure, key, tempo, notes, link ->
                viewModel.createSong(
                    title = title,
                    artist = artist,
                    duration = duration,
                    structure = structure,
                    key = key,
                    tempo = tempo,
                    notes = notes,
                    link = link
                )
                showAddDialog = false
            }
        )
    }
    
    if (showStats) {
        com.bandtrack.ui.stats.StatsScreen(
            groupId = groupId,
            onDismiss = { showStats = false }
        )
    }
}

@Composable
fun SongsList(
    songs: List<Song>,
    currentUserId: String,
    onMasteryChange: (String, Int) -> Unit,
    onUpdatePersonalConfig: (String, String) -> Unit,
    onUpdatePersonalNotes: (String, String) -> Unit,
    onUpdateSong: (String, Map<String, Any>) -> Unit,
    onNavigateToAudioNotes: (String) -> Unit,
    onDelete: (String) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(songs, key = { it.id }) { song ->
            SongCard(
                song = song,
                currentUserId = currentUserId,
                onMasteryChange = { level -> onMasteryChange(song.id, level) },
                onUpdatePersonalConfig = { config -> onUpdatePersonalConfig(song.id, config) },
                onUpdatePersonalNotes = { notes -> onUpdatePersonalNotes(song.id, notes) },
                onUpdateSong = { updates -> onUpdateSong(song.id, updates) },
                onNavigateToAudioNotes = { onNavigateToAudioNotes(song.id) },
                onDelete = { onDelete(song.id) }
            )
        }
    }
}

@Composable
fun SongCard(
    song: Song,
    currentUserId: String,
    onMasteryChange: (Int) -> Unit,
    onUpdatePersonalConfig: (String) -> Unit,
    onUpdatePersonalNotes: (String) -> Unit,
    onUpdateSong: (Map<String, Any>) -> Unit,
    onNavigateToAudioNotes: () -> Unit,
    onDelete: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }
    var showDetailsDialog by remember { mutableStateOf(false) }
    val myMastery = song.getMasteryLevel(currentUserId)
    val avgMastery = song.getAverageMasteryLevel()

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (song.isWellMastered()) 
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
            else 
                MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // En-tête
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = song.title,
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (song.isWellMastered()) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = "Bien maîtrisé",
                                modifier = Modifier.size(20.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    Text(
                        text = song.artist,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    val personalConfig = song.memberInstrumentConfigs[currentUserId]
                    if (!personalConfig.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "👉 $personalConfig",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    if (song.duration > 0) {
                        val mins = song.duration / 60
                        val secs = song.duration % 60
                        Text(
                            text = "⏱️ ${mins}:${secs.toString().padStart(2, '0')}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                
                IconButton(onClick = { showMenu = true }) {
                    Icon(Icons.Default.MoreVert, contentDescription = "Menu")
                }
                
                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Détails") },
                        onClick = {
                            showDetailsDialog = true
                            showMenu = false
                        },
                        leadingIcon = {
                            Icon(Icons.Default.Info, contentDescription = null)
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Notes Audio") },
                        onClick = {
                            onNavigateToAudioNotes()
                            showMenu = false
                        },
                        leadingIcon = {
                            Icon(Icons.Default.Mic, contentDescription = null)
                        }
                    )
                    HorizontalDivider()
                    DropdownMenuItem(
                        text = { Text("Supprimer") },
                        onClick = {
                            onDelete()
                            showMenu = false
                        },
                        leadingIcon = {
                            Icon(Icons.Default.Delete, contentDescription = null)
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Niveau de maîtrise moyen du groupe
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Groupe:",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = String.format("%.1f/10", avgMastery),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Slider de maîtrise personnel
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Ma maîtrise:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "$myMastery/10",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
                
                Slider(
                    value = myMastery.toFloat(),
                    onValueChange = { onMasteryChange(it.roundToInt()) },
                    valueRange = 0f..10f,
                    steps = 9,
                    colors = SliderDefaults.colors(
                        thumbColor = MaterialTheme.colorScheme.secondary,
                        activeTrackColor = MaterialTheme.colorScheme.secondary
                    )
                )
            }

            // Informations supplémentaires
            if (song.structure.isNotBlank() || song.key != null || song.tempo != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    song.key?.let {
                        Chip(text = "🎵 $it")
                    }
                    song.tempo?.let {
                        Chip(text = "⏱️ $it BPM")
                    }
                }
            }
        }
    }

    if (showDetailsDialog) {
        SongDetailsDialog(
            song = song,
            currentUserId = currentUserId,
            onUpdatePersonalConfig = onUpdatePersonalConfig,
            onUpdatePersonalNotes = onUpdatePersonalNotes,
            onUpdateSong = onUpdateSong,
            onDismiss = { showDetailsDialog = false }
        )
    }
}

@Composable
fun Chip(text: String) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = MaterialTheme.shapes.small
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            style = MaterialTheme.typography.bodySmall
        )
    }
}

@Composable
fun EmptyRepertoireView(onAddClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.MusicNote,
            contentDescription = null,
            modifier = Modifier.size(120.dp),
            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Text(
            text = "Répertoire vide",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Text(
            text = "Ajoutez vos premiers morceaux",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        Button(onClick = onAddClick) {
            Icon(Icons.Default.Add, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Ajouter un morceau")
        }
    }
}

@Composable
fun AddSongDialog(
    onDismiss: () -> Unit,
    onConfirm: (
        title: String,
        artist: String,
        duration: Int,
        structure: String,
        key: String?,
        tempo: Int?,
        notes: String,
        link: String?
    ) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var artist by remember { mutableStateOf("") }
    var durationMinutes by remember { mutableStateOf("") }
    var durationSeconds by remember { mutableStateOf("") }
    var structure by remember { mutableStateOf("") }
    var key by remember { mutableStateOf("") }
    var tempo by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var link by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Nouveau morceau") },
        text = {
            LazyColumn {
                item {
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("Titre *") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
                item {
                    OutlinedTextField(
                        value = artist,
                        onValueChange = { artist = it },
                        label = { Text("Artiste *") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = durationMinutes,
                            onValueChange = { durationMinutes = it.filter { c -> c.isDigit() } },
                            label = { Text("Durée (min)") },
                            placeholder = { Text("3") },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                                keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                            )
                        )
                        OutlinedTextField(
                            value = durationSeconds,
                            onValueChange = { durationSeconds = it.filter { c -> c.isDigit() }.take(2) },
                            label = { Text("sec") },
                            placeholder = { Text("30") },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                                keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                            )
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }
                item {
                    OutlinedTextField(
                        value = structure,
                        onValueChange = { structure = it },
                        label = { Text("Structure") },
                        placeholder = { Text("Intro-Couplet-Refrain...") },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 2
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = key,
                            onValueChange = { key = it },
                            label = { Text("Tonalité") },
                            placeholder = { Text("Am, G...") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = tempo,
                            onValueChange = { tempo = it.filter { c -> c.isDigit() } },
                            label = { Text("BPM") },
                            placeholder = { Text("120") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }
                item {
                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        label = { Text("Notes") },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 3
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
                item {
                    OutlinedTextField(
                        value = link,
                        onValueChange = { link = it },
                        label = { Text("Lien") },
                        placeholder = { Text("YouTube, Spotify...") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val totalSeconds = (durationMinutes.toIntOrNull() ?: 0) * 60 + (durationSeconds.toIntOrNull() ?: 0)
                    onConfirm(
                        title,
                        artist,
                        totalSeconds,
                        structure,
                        key.ifBlank { null },
                        tempo.toIntOrNull(),
                        notes,
                        link.ifBlank { null }
                    )
                },
                enabled = title.isNotBlank() && artist.isNotBlank()
            ) {
                Text("Ajouter")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Annuler")
            }
        }
    )
}

@Composable
fun SongDetailsDialog(
    song: Song,
    currentUserId: String,
    onUpdatePersonalConfig: (String) -> Unit,
    onUpdatePersonalNotes: (String) -> Unit,
    onUpdateSong: (Map<String, Any>) -> Unit,
    onDismiss: () -> Unit
) {
    var title by remember { mutableStateOf(song.title) }
    var artist by remember { mutableStateOf(song.artist) }
    var durationMinutes by remember { mutableStateOf((song.duration / 60).toString()) }
    var durationSeconds by remember { mutableStateOf((song.duration % 60).toString()) }
    var key by remember { mutableStateOf(song.key ?: "") }
    var tempo by remember { mutableStateOf(song.tempo?.toString() ?: "") }
    
    var personalConfig by remember { mutableStateOf(song.memberInstrumentConfigs[currentUserId] ?: "") }
    var personalNotes by remember { mutableStateOf(song.memberPersonalNotes[currentUserId] ?: "") }
    
    val hasChanges = remember(title, artist, durationMinutes, durationSeconds, key, tempo, personalConfig, personalNotes) {
        title != song.title ||
        artist != song.artist ||
        durationMinutes.toIntOrNull() != (song.duration / 60) ||
        durationSeconds.toIntOrNull() != (song.duration % 60) ||
        key != (song.key ?: "") ||
        tempo != (song.tempo?.toString() ?: "") ||
        personalConfig != (song.memberInstrumentConfigs[currentUserId] ?: "") ||
        personalNotes != (song.memberPersonalNotes[currentUserId] ?: "")
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { 
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Titre") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.verticalScroll(rememberScrollState())
            ) {
                OutlinedTextField(
                    value = artist,
                    onValueChange = { artist = it },
                    label = { Text("Artiste") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = durationMinutes,
                        onValueChange = { durationMinutes = it.filter { c -> c.isDigit() } },
                        label = { Text("Durée (min)") },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                            keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                        )
                    )
                    OutlinedTextField(
                        value = durationSeconds,
                        onValueChange = { durationSeconds = it.filter { c -> c.isDigit() }.take(2) },
                        label = { Text("sec") },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                            keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                        )
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = key,
                        onValueChange = { key = it },
                        label = { Text("Tonalité") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = tempo,
                        onValueChange = { tempo = it.filter { c -> c.isDigit() } },
                        label = { Text("BPM") },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                            keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                        )
                    )
                }
                
                HorizontalDivider()

                // Section Personnelle
                Text(
                    text = "Ma Configuration 🎵",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary
                )
                
                OutlinedTextField(
                    value = personalConfig,
                    onValueChange = { personalConfig = it },
                    label = { Text("Mon Instrument / Clé") },
                    placeholder = { Text("Ex: Harmo G, Capo 2...") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = personalNotes,
                    onValueChange = { personalNotes = it },
                    label = { Text("Mes Notes") },
                    placeholder = { Text("Notes personnelles pour ce morceau...") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                    maxLines = 5
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (hasChanges) {
                        // Mise à jour de la config perso
                        if (personalConfig != (song.memberInstrumentConfigs[currentUserId] ?: "") ||
                            personalNotes != (song.memberPersonalNotes[currentUserId] ?: "")) {
                            onUpdatePersonalConfig(personalConfig)
                            onUpdatePersonalNotes(personalNotes)
                        }
                        
                        // Mise à jour des infos globales
                        val totalSeconds = (durationMinutes.toIntOrNull() ?: 0) * 60 + (durationSeconds.toIntOrNull() ?: 0)
                        val updates = mutableMapOf<String, Any>()
                        if (title != song.title) updates["title"] = title
                        if (artist != song.artist) updates["artist"] = artist
                        if (totalSeconds != song.duration) updates["duration"] = totalSeconds
                        if (key != (song.key ?: "")) updates["key"] = key
                        if (tempo != (song.tempo?.toString() ?: "")) updates["tempo"] = tempo.toIntOrNull() ?: 0
                        
                        if (updates.isNotEmpty()) {
                            onUpdateSong(updates)
                        }
                    }
                    onDismiss()
                }
            ) {
                Text(if (hasChanges) "Enregistrer" else "Fermer")
            }
        },
        dismissButton = {
            if (hasChanges) {
                TextButton(onClick = onDismiss) {
                    Text("Annuler")
                }
            }
        }
    )
}

@Composable
fun DetailRow(label: String, value: String) {
    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@Composable
fun ErrorView(message: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Default.Error,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.error
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.error
        )
    }
}
