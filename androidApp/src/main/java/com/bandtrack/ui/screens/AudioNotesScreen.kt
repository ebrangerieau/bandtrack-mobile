package com.bandtrack.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.bandtrack.data.models.AudioNote
import com.bandtrack.ui.viewmodels.AudioNoteViewModel
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

/**
 * Écran de gestion des notes audio pour un morceau
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AudioNotesScreen(
    songId: String,
    groupId: String,
    userId: String,
    songTitle: String,
    viewModel: AudioNoteViewModel,
    onNavigateBack: () -> Unit
) {
    val audioNotes by viewModel.audioNotes.collectAsState()
    val isRecording by viewModel.isRecording.collectAsState()
    val recordingDuration by viewModel.recordingDuration.collectAsState()
    val isPlaying by viewModel.isPlaying.collectAsState()
    val currentPlayingNoteId by viewModel.currentPlayingNoteId.collectAsState()
    val error by viewModel.error.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    var showRecordingDialog by remember { mutableStateOf(false) }

    // Charger les notes au démarrage
    LaunchedEffect(songId) {
        viewModel.loadAudioNotes(songId)
    }

    // Afficher les erreurs
    LaunchedEffect(error) {
        error?.let {
            scope.launch {
                snackbarHostState.showSnackbar(it)
                viewModel.clearError()
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Column {
                        Text("Notes audio")
                        Text(
                            text = songTitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, "Retour")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        },
        floatingActionButton = {
            if (!isRecording) {
                FloatingActionButton(
                    onClick = { showRecordingDialog = true },
                    containerColor = MaterialTheme.colorScheme.primary
                ) {
                    Icon(Icons.Default.Mic, "Enregistrer")
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when {
                isLoading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                audioNotes.isEmpty() -> {
                    EmptyStateView(
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                else -> {
                    AudioNotesList(
                        notes = audioNotes,
                        currentPlayingNoteId = currentPlayingNoteId,
                        isPlaying = isPlaying,
                        onPlayClick = { viewModel.startPlaying(it) },
                        onPauseClick = { viewModel.pausePlaying() },
                        onStopClick = { viewModel.stopPlaying() },
                        onDeleteClick = { viewModel.deleteAudioNote(it, songId) },
                        onRenameClick = { noteId, newTitle ->
                            viewModel.updateNoteTitle(noteId, newTitle, songId)
                        }
                    )
                }
            }

            // Indicateur d'enregistrement en cours
            AnimatedVisibility(
                visible = isRecording,
                enter = slideInVertically() + fadeIn(),
                exit = slideOutVertically() + fadeOut(),
                modifier = Modifier.align(Alignment.BottomCenter)
            ) {
                RecordingIndicator(
                    duration = recordingDuration,
                    onStopClick = { 
                        viewModel.stopRecording(songId, groupId, userId)
                        showRecordingDialog = false
                    },
                    onCancelClick = { 
                        viewModel.cancelRecording()
                        showRecordingDialog = false
                    }
                )
            }
        }
    }

    // Dialog d'enregistrement
    if (showRecordingDialog && !isRecording) {
        RecordingDialog(
            onStartRecording = { viewModel.startRecording() },
            onDismiss = { showRecordingDialog = false }
        )
    }
}

@Composable
private fun AudioNotesList(
    notes: List<AudioNote>,
    currentPlayingNoteId: String?,
    isPlaying: Boolean,
    onPlayClick: (String) -> Unit,
    onPauseClick: () -> Unit,
    onStopClick: () -> Unit,
    onDeleteClick: (String) -> Unit,
    onRenameClick: (String, String) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(notes, key = { it.id }) { note ->
            AudioNoteItem(
                note = note,
                isCurrentlyPlaying = currentPlayingNoteId == note.id,
                isPlaying = isPlaying && currentPlayingNoteId == note.id,
                onPlayClick = { onPlayClick(note.id) },
                onPauseClick = onPauseClick,
                onStopClick = onStopClick,
                onDeleteClick = { onDeleteClick(note.id) },
                onRenameClick = { newTitle -> onRenameClick(note.id, newTitle) }
            )
        }
    }
}

@Composable
private fun AudioNoteItem(
    note: AudioNote,
    isCurrentlyPlaying: Boolean,
    isPlaying: Boolean,
    onPlayClick: () -> Unit,
    onPauseClick: () -> Unit,
    onStopClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onRenameClick: (String) -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }
    var showRenameDialog by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isCurrentlyPlaying) 
                MaterialTheme.colorScheme.primaryContainer 
            else 
                MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Bouton Play/Pause
            IconButton(
                onClick = {
                    when {
                        !isCurrentlyPlaying -> onPlayClick()
                        isPlaying -> onPauseClick()
                        else -> onPlayClick()
                    }
                },
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary)
            ) {
                Icon(
                    imageVector = when {
                        isCurrentlyPlaying && isPlaying -> Icons.Default.Pause
                        else -> Icons.Default.PlayArrow
                    },
                    contentDescription = "Lecture",
                    tint = MaterialTheme.colorScheme.onPrimary
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Informations
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = note.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = formatDuration(note.duration * 1000L),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = formatDate(note.createdAt),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Bouton Stop (si en lecture)
            if (isCurrentlyPlaying) {
                IconButton(onClick = onStopClick) {
                    Icon(Icons.Default.Stop, "Arrêter")
                }
            }

            // Menu
            Box {
                IconButton(onClick = { showMenu = true }) {
                    Icon(Icons.Default.MoreVert, "Menu")
                }
                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Renommer") },
                        onClick = {
                            showMenu = false
                            showRenameDialog = true
                        },
                        leadingIcon = { Icon(Icons.Default.Edit, null) }
                    )
                    DropdownMenuItem(
                        text = { Text("Supprimer") },
                        onClick = {
                            showMenu = false
                            onDeleteClick()
                        },
                        leadingIcon = { Icon(Icons.Default.Delete, null) }
                    )
                }
            }
        }
    }

    // Dialog de renommage
    if (showRenameDialog) {
        RenameDialog(
            currentTitle = note.title,
            onConfirm = { newTitle ->
                onRenameClick(newTitle)
                showRenameDialog = false
            },
            onDismiss = { showRenameDialog = false }
        )
    }
}

@Composable
private fun RecordingIndicator(
    duration: Long,
    onStopClick: () -> Unit,
    onCancelClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.error)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "Enregistrement en cours",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = formatDuration(duration),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }

            Row {
                IconButton(onClick = onCancelClick) {
                    Icon(Icons.Default.Close, "Annuler")
                }
                IconButton(onClick = onStopClick) {
                    Icon(Icons.Default.Check, "Terminer")
                }
            }
        }
    }
}

@Composable
private fun RecordingDialog(
    onStartRecording: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Default.Mic, null) },
        title = { Text("Nouvelle note audio") },
        text = { Text("Prêt à enregistrer une note audio ?") },
        confirmButton = {
            Button(onClick = {
                onStartRecording()
                onDismiss()
            }) {
                Text("Démarrer")
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
private fun RenameDialog(
    currentTitle: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var title by remember { mutableStateOf(currentTitle) }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Default.Edit, null) },
        title = { Text("Renommer la note") },
        text = {
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Titre") },
                singleLine = true
            )
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(title) },
                enabled = title.isNotBlank()
            ) {
                Text("Renommer")
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
private fun EmptyStateView(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Default.MusicNote,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Aucune note audio",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Appuyez sur + pour enregistrer votre première note",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

// Fonctions utilitaires
private fun formatDuration(millis: Long): String {
    val duration = millis.milliseconds
    val minutes = duration.inWholeMinutes
    val seconds = duration.inWholeSeconds % 60
    return String.format("%02d:%02d", minutes, seconds)
}

private fun formatDate(timestamp: Long): String {
    val now = System.currentTimeMillis()
    val diff = now - timestamp
    
    return when {
        diff < 60_000 -> "À l'instant"
        diff < 3600_000 -> "${diff / 60_000} min"
        diff < 86400_000 -> "${diff / 3600_000} h"
        diff < 604800_000 -> "${diff / 86400_000} j"
        else -> {
            val date = java.util.Date(timestamp)
            java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.FRENCH).format(date)
        }
    }
}
