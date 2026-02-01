package com.bandtrack.ui.performance

import androidx.compose.foundation.background
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.bandtrack.data.models.Performance
import com.bandtrack.data.models.Song

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SetlistEditorDialog(
    performance: Performance,
    allSongs: Map<String, Song>,
    onDismiss: () -> Unit,
    onReorder: (Int, Int) -> Unit,
    onRemove: (String) -> Unit,
    onAddSongs: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Toolbar
                TopAppBar(
                    title = { 
                        Column {
                            Text("Setlist")
                            Text(
                                performance.title.ifEmpty { "Édition" },
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, "Fermer")
                        }
                    },
                    actions = {
                        IconButton(onClick = onAddSongs) {
                            Icon(Icons.Default.Add, "Ajouter des morceaux")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp)
                    )
                )

                if (performance.setlist.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Default.QueueMusic,
                                contentDescription = null,
                                modifier = Modifier.size(64.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                "La setlist est vide",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Button(
                                onClick = onAddSongs,
                                modifier = Modifier.padding(top = 16.dp)
                            ) {
                                Icon(Icons.Default.Add, null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Ajouter des morceaux")
                            }
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .weight(1f),
                        contentPadding = PaddingValues(16.dp)
                    ) {
                        itemsIndexed(performance.setlist) { index, songId ->
                            val song = allSongs[songId]
                            val isFirst = index == 0
                            val isLast = index == performance.setlist.size - 1
                            
                            SetlistItem(
                                index = index + 1,
                                title = song?.title ?: "Morceau inconnu",
                                artist = song?.artist ?: "",
                                duration = song?.duration ?: 0,
                                isFirst = isFirst,
                                isLast = isLast,
                                onMoveUp = { if (!isFirst) onReorder(index, index - 1) },
                                onMoveDown = { if (!isLast) onReorder(index, index + 1) },
                                onRemove = { onRemove(songId) }
                            )
                            if (!isLast) {
                                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                            }
                        }
                        
                        item {
                            // Résumé
                            val totalDuration = performance.setlist.sumOf { allSongs[it]?.duration ?: 0 }
                            val minutes = totalDuration / 60
                            
                            Card(
                                modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                                )
                            ) {
                                Row(
                                    modifier = Modifier.padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.Timer, null)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        "Durée totale estimée : ${minutes} min",
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SetlistItem(
    index: Int,
    title: String,
    artist: String,
    duration: Int,
    isFirst: Boolean,
    isLast: Boolean,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onRemove: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Numéro
        Box(
            modifier = Modifier
                .size(32.dp)
                .background(MaterialTheme.colorScheme.primaryContainer, MaterialTheme.shapes.small),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "$index",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
        
        Spacer(modifier = Modifier.width(12.dp))
        
        // Info
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )
            if (artist.isNotBlank()) {
                Text(
                    text = artist,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        
        // Actions
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            IconButton(
                onClick = onMoveUp,
                enabled = !isFirst,
                modifier = Modifier.size(24.dp)
            ) {
                Icon(
                    Icons.Default.KeyboardArrowUp, 
                    contentDescription = "Monter",
                    tint = if (!isFirst) MaterialTheme.colorScheme.onSurface else Color.LightGray
                )
            }
            IconButton(
                onClick = onMoveDown,
                enabled = !isLast,
                modifier = Modifier.size(24.dp)
            ) {
                Icon(
                    Icons.Default.KeyboardArrowDown, 
                    contentDescription = "Descendre",
                    tint = if (!isLast) MaterialTheme.colorScheme.onSurface else Color.LightGray
                )
            }
        }
        
        IconButton(onClick = onRemove) {
            Icon(
                Icons.Default.Delete,
                contentDescription = "Retirer",
                tint = MaterialTheme.colorScheme.error
            )
        }
    }
}

@Composable
fun SongSelectorDialog(
    availableSongs: List<Song>,
    onDismiss: () -> Unit,
    onSongSelected: (String) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    
    val filteredSongs = remember(availableSongs, searchQuery) {
        if (searchQuery.isBlank()) {
            availableSongs
        } else {
            availableSongs.filter { 
                it.title.contains(searchQuery, ignoreCase = true) || 
                it.artist.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Ajouter un morceau") },
        text = {
            Column(
                modifier = Modifier.heightIn(max = 400.dp)
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    label = { Text("Rechercher") },
                    leadingIcon = { Icon(Icons.Default.Search, null) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                if (filteredSongs.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Aucun morceau trouvé")
                    }
                } else {
                    LazyColumn {
                        items(filteredSongs) { song ->
                            ListItem(
                                headlineContent = { Text(song.title) },
                                supportingContent = { 
                                    if (song.artist.isNotBlank()) Text(song.artist) 
                                },
                                modifier = Modifier
                                    .clickable { onSongSelected(song.id) }
                                    .fillMaxWidth(),
                                leadingContent = {
                                    Icon(Icons.Default.MusicNote, null)
                                }
                            )
                            HorizontalDivider()
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Fermer")
            }
        }
    )
}
