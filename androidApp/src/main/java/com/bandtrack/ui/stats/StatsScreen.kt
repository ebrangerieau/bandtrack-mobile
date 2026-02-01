package com.bandtrack.ui.stats

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.viewmodel.compose.viewModel
import com.bandtrack.data.models.Song

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsScreen(
    groupId: String,
    onDismiss: () -> Unit,
    viewModel: StatsViewModel = viewModel()
) {
    val stats by viewModel.stats.collectAsState()

    LaunchedEffect(groupId) {
        viewModel.loadStats(groupId)
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                TopAppBar(
                    title = { Text("Statistiques du Groupe") },
                    navigationIcon = {
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, "Fermer")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp)
                    )
                )

                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item {
                        // Global Stats Card
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .padding(16.dp)
                                    .fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceAround
                            ) {
                                StatItem(
                                    label = "Répertoire",
                                    value = "${stats.totalSongs}",
                                    style = MaterialTheme.typography.displaySmall
                                )
                                StatItem(
                                    label = "Maîtrise Moy.",
                                    value = String.format("%.1f", stats.globalMastery),
                                    style = MaterialTheme.typography.displaySmall,
                                    suffix = "/10"
                                )
                            }
                        }
                    }

                    item {
                        Text(
                            "Top Maîtrise",
                            style = MaterialTheme.typography.titleLarge,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }

                    item {
                        if (stats.bestSongs.isEmpty()) {
                            Text("Pas assez de données", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        } else {
                            Card {
                                Column {
                                    stats.bestSongs.forEach { song ->
                                        SongStatRow(song, Icons.Default.TrendingUp, MaterialTheme.colorScheme.primary)
                                        if (song != stats.bestSongs.last()) Divider()
                                    }
                                }
                            }
                        }
                    }

                    item {
                        Text(
                            "À Travailler",
                            style = MaterialTheme.typography.titleLarge,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }

                    item {
                        if (stats.worstSongs.isEmpty()) {
                            Text("Pas assez de données", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        } else {
                            Card {
                                Column {
                                    stats.worstSongs.forEach { song ->
                                        SongStatRow(song, Icons.Default.TrendingDown, MaterialTheme.colorScheme.error)
                                        if (song != stats.worstSongs.last()) Divider()
                                    }
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
fun StatItem(
    label: String,
    value: String,
    style: androidx.compose.ui.text.TextStyle,
    suffix: String = ""
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, style = MaterialTheme.typography.labelMedium)
        Row(verticalAlignment = Alignment.Bottom) {
            Text(value, style = style, fontWeight = FontWeight.Bold)
            if (suffix.isNotEmpty()) {
                Text(suffix, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(bottom = 4.dp))
            }
        }
    }
}

@Composable
fun SongStatRow(song: Song, icon: ImageVector, iconColor: Color) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = iconColor)
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(song.title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
            Text(song.artist, style = MaterialTheme.typography.bodySmall)
        }
        Text(
            String.format("%.1f/10", song.getAverageMasteryLevel()),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold
        )
    }
}
