package com.bandtrack.ui.performance

import android.app.Activity
import android.view.WindowManager
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.bandtrack.data.models.Performance
import com.bandtrack.data.models.Song
import com.bandtrack.data.repository.PerformanceRepository
import com.bandtrack.data.repository.SongRepository
import com.bandtrack.ui.viewmodels.PerformanceViewModel
import com.bandtrack.ui.viewmodels.PerformanceViewModelFactory
import kotlinx.coroutines.launch

@Composable
fun LiveModeScreen(
    performanceId: String,
    groupId: String,
    performanceRepository: PerformanceRepository,
    songRepository: SongRepository,
    onExit: () -> Unit
) {
    val context = LocalContext.current
    val viewModel: PerformanceViewModel = viewModel(
        factory = PerformanceViewModelFactory(performanceRepository, songRepository)
    )
    
    // Charger les données
    LaunchedEffect(groupId) {
        viewModel.initialize(groupId)
    }

    val uiState by viewModel.uiState.collectAsState()
    val songsCache by viewModel.songsCache.collectAsState()
    
    // Garder l'écran allumé
    DisposableEffect(Unit) {
        val window = (context as? Activity)?.window
        window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        onDispose {
            window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    // Trouver la performance
    // Note: Idéalement le ViewModel devrait exposer selectedPerformance, ici on le cherche dans la liste
    val performance = (uiState as? com.bandtrack.ui.viewmodels.PerformanceUiState.Success)
        ?.performances?.find { it.id == performanceId }

    if (performance == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    // État du Pager pour les chansons
    val pagerState = rememberPagerState(pageCount = { performance.setlist.size })
    val scope = rememberCoroutineScope()
    var showSetlistDrawer by remember { mutableStateOf(false) }

    // Obtenir la chanson courante
    val currentSongId = performance.setlist.getOrNull(pagerState.currentPage)
    val currentSong = songsCache[currentSongId]

    // Thème Sombre & Contraste Élevé forcé pour le Live Mode
    MaterialTheme(
        colorScheme = darkColorScheme(
            background = Color.Black,
            surface = Color(0xFF121212),
            onBackground = Color.White,
            onSurface = Color.White,
            primary = Color(0xFFBB86FC),
            secondary = Color(0xFF03DAC6)
        )
    ) {
        Scaffold(
            topBar = {
                LiveTopBar(
                    title = currentSong?.title ?: "Inconnu",
                    subtitle = "${pagerState.currentPage + 1}/${performance.setlist.size} - ${performance.title.ifBlank { "Live" }}",
                    onShowSetlist = { showSetlistDrawer = !showSetlistDrawer },
                    onExit = onExit
                )
            },
            bottomBar = {
                LiveControlBar(
                    canPrev = pagerState.currentPage > 0,
                    canNext = pagerState.currentPage < performance.setlist.lastIndex,
                    onPrev = { scope.launch { pagerState.animateScrollToPage(pagerState.currentPage - 1) } },
                    onNext = { scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) } }
                )
            }
        ) { padding ->
            Box(modifier = Modifier.padding(padding)) {
                if (showSetlistDrawer) {
                    LiveSetlistView(
                        performance = performance,
                        songsCache = songsCache,
                        currentIndex = pagerState.currentPage,
                        onSelect = { index -> 
                            scope.launch { 
                                pagerState.scrollToPage(index) 
                                showSetlistDrawer = false
                            }
                        }
                    )
                } else {
                    HorizontalPager(state = pagerState) { page ->
                        val songId = performance.setlist.getOrNull(page)
                        val song = songsCache[songId]
                        if (song != null) {
                            LiveSongDetail(song = song)
                        } else {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text("Chanson introuvable", style = MaterialTheme.typography.headlineMedium)
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LiveTopBar(
    title: String,
    subtitle: String,
    onShowSetlist: () -> Unit,
    onExit: () -> Unit
) {
    Column(modifier = Modifier.background(Color.Black).fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(onClick = onExit) {
                Icon(Icons.Default.Close, "Quitter", tint = Color.Gray)
            }
            
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    maxLines = 1,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.Gray
                )
            }

            IconButton(onClick = onShowSetlist) {
                Icon(Icons.Default.QueueMusic, "Setlist", tint = Color.White)
            }
        }
        Divider(color = Color.DarkGray)
    }
}

@Composable
fun LiveControlBar(
    canPrev: Boolean,
    canNext: Boolean,
    onPrev: () -> Unit,
    onNext: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.Black)
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Button(
            onClick = onPrev,
            enabled = canPrev,
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF333333)),
            modifier = Modifier.weight(1f).height(64.dp)
        ) {
            Icon(Icons.Default.ArrowBack, null, modifier = Modifier.size(32.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Précédent", fontSize = 20.sp)
        }
        
        Spacer(modifier = Modifier.width(16.dp))
        
        Button(
            onClick = onNext,
            enabled = canNext,
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
             modifier = Modifier.weight(1f).height(64.dp)
        ) {
            Text("Suivant", fontSize = 20.sp)
            Spacer(modifier = Modifier.width(8.dp))
            Icon(Icons.Default.ArrowForward, null, modifier = Modifier.size(32.dp))
        }
    }
}

@Composable
fun LiveSongDetail(song: Song) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .verticalScroll(rememberScrollState())
            .padding(24.dp)
    ) {
        // Affichage Paroles / Accords en gros
        // Pour l'instant on affiche brut, plus tard on pourra parser
        Text(
            text = song.lyrics.ifBlank { "Aucune parole" },
            style = MaterialTheme.typography.headlineSmall,
            color = Color.White,
            lineHeight = 32.sp
        )
        
        Spacer(modifier = Modifier.height(100.dp)) // Padding pour scroller au dessus de la bottom bar
    }
}

@Composable
fun LiveSetlistView(
    performance: Performance,
    songsCache: Map<String, Song>,
    currentIndex: Int,
    onSelect: (Int) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .padding(16.dp)
    ) {
        itemsIndexed(performance.setlist) { index, songId ->
            val song = songsCache[songId]
            val isSelected = index == currentIndex
            
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color(0xFF1E1E1E)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
                    .clickable { onSelect(index) }
            ) {
                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "${index + 1}",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = if (isSelected) Color.Black else Color.Gray,
                        modifier = Modifier.width(40.dp)
                    )
                    Column {
                        Text(
                            song?.title ?: "Inconnu",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = if (isSelected) Color.Black else Color.White
                        )
                        if (song?.artist?.isNotBlank() == true) {
                            Text(
                                song.artist,
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (isSelected) Color.DarkGray else Color.Gray
                            )
                        }
                    }
                }
            }
        }
    }
}
