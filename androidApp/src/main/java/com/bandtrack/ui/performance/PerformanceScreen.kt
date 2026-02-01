package com.bandtrack.ui.performance

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.bandtrack.data.models.Performance
import com.bandtrack.data.models.PerformanceType
import com.bandtrack.data.models.Song
import com.bandtrack.ui.viewmodels.PerformanceUiState
import com.bandtrack.ui.viewmodels.PerformanceViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PerformanceScreen(
    groupId: String,
    userId: String,
    viewModel: PerformanceViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val songsCache by viewModel.songsCache.collectAsState()
    
    var selectedTab by remember { mutableStateOf(0) }
    var showAddDialog by remember { mutableStateOf(false) }
    var selectedPerformance by remember { mutableStateOf<Performance?>(null) }
    
    // États pour l'édition de setlist
    var showSetlistEditor by remember { mutableStateOf(false) }
    var showSongSelector by remember { mutableStateOf(false) }

    LaunchedEffect(groupId) {
        viewModel.initialize(groupId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Planning & Prestations") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onSecondaryContainer
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) {
                Icon(Icons.Default.Add, "Ajouter un événement")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            TabRow(selectedTabIndex = selectedTab) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("À venir") }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Passés") }
                )
            }

            when (val state = uiState) {
                is PerformanceUiState.Loading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                is PerformanceUiState.Error -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Erreur : ${state.message}", color = MaterialTheme.colorScheme.error)
                    }
                }
                is PerformanceUiState.Success -> {
                    val listToShow = if (selectedTab == 0) state.upcomingPerformances else state.pastPerformances
                    
                    if (listToShow.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text(
                                if (selectedTab == 0) "Aucun événement à venir" else "Aucun événement passé",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        LazyColumn(
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(listToShow) { performance ->
                                PerformanceItem(
                                    performance = performance,
                                    onClick = { selectedPerformance = performance }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AddPerformanceDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { type, date, location, title, notes ->
                viewModel.createPerformance(
                    groupId = groupId,
                    type = type,
                    date = date,
                    time = 0,
                    durationMinutes = 120,
                    location = location,
                    title = title,
                    notes = notes,
                    userId = userId
                )
                showAddDialog = false
            }
        )
    }
    
    selectedPerformance?.let { performance ->
        if (showSetlistEditor) {
            SetlistEditorDialog(
                performance = performance,
                allSongs = songsCache,
                onDismiss = { showSetlistEditor = false },
                onReorder = { from, to -> 
                    viewModel.reorderSetlist(groupId, performance.id, from, to)
                    // Mettre à jour l'objet localement pour fluidité (sera écrasé par le flow)
                    val newList = performance.setlist.toMutableList()
                    val item = newList.removeAt(from)
                    newList.add(to, item)
                    selectedPerformance = performance.copy(setlist = newList)
                },
                onRemove = { songId ->
                    viewModel.removeFromSetlist(groupId, performance.id, songId)
                    val newList = performance.setlist - songId
                    selectedPerformance = performance.copy(setlist = newList)
                },
                onAddSongs = { showSongSelector = true }
            )
        } else {
            PerformanceDetailsDialog(
                performance = performance,
                songsCache = songsCache,
                onDismiss = { selectedPerformance = null },
                onDelete = {
                    viewModel.deletePerformance(groupId, performance.id)
                    selectedPerformance = null
                },
                onEditSetlist = { showSetlistEditor = true }
            )
        }
        
        if (showSongSelector) {
            SongSelectorDialog(
                availableSongs = songsCache.values.toList().sortedBy { it.title },
                onDismiss = { showSongSelector = false },
                onSongSelected = { songId ->
                    viewModel.addToSetlist(groupId, performance.id, songId)
                    val newList = performance.setlist + songId
                    selectedPerformance = performance.copy(setlist = newList)
                    showSongSelector = false
                }
            )
        }
    }
}

@Composable
fun PerformanceItem(
    performance: Performance,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier
                    .width(60.dp)
                    .padding(end = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = SimpleDateFormat("dd", Locale.getDefault()).format(Date(performance.date)),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = SimpleDateFormat("MMM", Locale.getDefault()).format(Date(performance.date)).uppercase(),
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold
                )
            }
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = performance.getDisplayName(),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.AccessTime, 
                        contentDescription = null, 
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(performance.date)),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    if (performance.location.isNotBlank()) {
                        Icon(
                            Icons.Default.Place, 
                            contentDescription = null, 
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = performance.location,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1
                        )
                    }
                }
            }
            
            val icon = when (performance.type) {
                PerformanceType.GIG -> Icons.Default.MusicNote
                PerformanceType.REHEARSAL -> Icons.Default.Headphones
                else -> Icons.Default.Event
            }
            
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddPerformanceDialog(
    onDismiss: () -> Unit,
    onConfirm: (PerformanceType, Long, String, String, String) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var location by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var type by remember { mutableStateOf(PerformanceType.REHEARSAL) }
    
    // Simplification date/heure pour MVP
    var selectedDate by remember { mutableStateOf(System.currentTimeMillis()) }
    
    val datePickerState = rememberDatePickerState(initialSelectedDateMillis = selectedDate)
    var showDatePicker by remember { mutableStateOf(false) }
    
    // Heure (TimePicker est plus complexe en Compose M3, on va faire simple ou omettre pour le MVP strict)
    // On va mettre une heure par défaut ou utiliser un Text pour l'instant
    var hour by remember { mutableStateOf(20) }
    var minute by remember { mutableStateOf(0) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Nouvel événement") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                // Type selector
                Row(modifier = Modifier.fillMaxWidth()) {
                    PerformanceType.entries.forEach { t ->
                        FilterChip(
                            selected = type == t,
                            onClick = { type = t },
                            label = { 
                                Text(when(t) {
                                    PerformanceType.REHEARSAL -> "Répétition"
                                    PerformanceType.GIG -> "Concert"
                                    PerformanceType.OTHER -> "Autre"
                                }) 
                            },
                            modifier = Modifier.padding(end = 8.dp)
                        )
                    }
                }

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Titre (Optionnel)") },
                    modifier = Modifier.fillMaxWidth()
                )
                
                OutlinedTextField(
                    value = location,
                    onValueChange = { location = it },
                    label = { Text("Lieu") },
                    modifier = Modifier.fillMaxWidth()
                )

                // Date Picker Trigger
                OutlinedCard(
                    onClick = { showDatePicker = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.CalendarToday, null)
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(
                            SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date(selectedDate))
                        )
                    }
                }
                
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { 
                    onConfirm(type, selectedDate, location, title, notes) 
                }
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
    
    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let {
                        // Conserver l'heure actuelle lors du changement de date pour simplifier
                        val calendar = Calendar.getInstance()
                        calendar.timeInMillis = it
                        calendar.set(Calendar.HOUR_OF_DAY, hour)
                        calendar.set(Calendar.MINUTE, minute)
                        selectedDate = calendar.timeInMillis
                    }
                    showDatePicker = false
                }) {
                    Text("OK")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

@Composable
fun PerformanceDetailsDialog(
    performance: Performance,
    songsCache: Map<String, Song>,
    onDismiss: () -> Unit,
    onDelete: () -> Unit,
    onEditSetlist: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(performance.getDisplayName()) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Date : ${SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date(performance.date))}")
                if (performance.location.isNotBlank()) Text("Lieu : ${performance.location}")
                if (performance.notes.isNotBlank()) {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                    Text("Notes :", style = MaterialTheme.typography.labelLarge)
                    Text(performance.notes)
                }
                
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Setlist (${performance.setlist.size} morceaux)",
                        style = MaterialTheme.typography.titleMedium
                    )
                    TextButton(onClick = onEditSetlist) {
                        Text("Gérer")
                    }
                }
                
                if (performance.setlist.isNotEmpty()) {
                    LazyColumn(
                        modifier = Modifier
                            .heightIn(max = 200.dp)
                            .fillMaxWidth()
                    ) {
                        itemsIndexed(performance.setlist) { index, songId ->
                            val song = songsCache[songId]
                            Row(modifier = Modifier.padding(vertical = 4.dp)) {
                                Text(
                                    "${index + 1}.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.width(28.dp)
                                )
                                Text(
                                    song?.title ?: "Inconnu",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    }
                } else {
                    Text(
                        "Aucun morceau dans la setlist",
                        style = MaterialTheme.typography.bodySmall,
                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Fermer")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDelete,
                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
            ) {
                Text("Supprimer")
            }
        }
    )
}
