package com.bandtrack.ui.suggestions

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.bandtrack.data.models.Suggestion
import com.bandtrack.data.repository.SongRepository
import com.bandtrack.data.repository.SuggestionRepository
import com.bandtrack.ui.suggestions.SuggestionsViewModelFactory

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SuggestionsScreen(
    groupId: String,
    userId: String,
    userName: String,
    songRepository: SongRepository,
    suggestionRepository: SuggestionRepository
) {
    val viewModel: SuggestionsViewModel = viewModel(
        factory = SuggestionsViewModelFactory(suggestionRepository, songRepository)
    )
    val uiState by viewModel.uiState.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }
    var editingSuggestion by remember { mutableStateOf<Suggestion?>(null) }

    LaunchedEffect(groupId) {
        viewModel.initialize(groupId, userId, userName)
    }

    Scaffold(
        containerColor = androidx.compose.ui.graphics.Color.Transparent,
        topBar = {
            TopAppBar(
                title = { Text("Suggestions") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true }
            ) {
                Icon(Icons.Default.Add, contentDescription = "Ajouter une suggestion")
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when (val state = uiState) {
                is SuggestionsUiState.Loading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                is SuggestionsUiState.Success -> {
                    if (state.suggestions.isEmpty()) {
                        EmptySuggestionsView(
                            onAddClick = { showAddDialog = true }
                        )
                    } else {
                        SuggestionsList(
                            suggestions = state.suggestions,
                            currentUserId = userId,
                            onVote = viewModel::toggleVote,
                            onConvert = viewModel::convertToSong,
                            onDelete = viewModel::deleteSuggestion,
                            onEdit = { editingSuggestion = it }
                        )
                    }
                }
                is SuggestionsUiState.Error -> {
                    ErrorView(
                        message = state.message,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
            }
        }
    }

    if (showAddDialog) {
        SuggestionDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { title, artist, duration, link ->
                viewModel.createSuggestion(title, artist, duration, link)
                showAddDialog = false
            }
        )
    }

    if (editingSuggestion != null) {
        SuggestionDialog(
            suggestion = editingSuggestion,
            onDismiss = { editingSuggestion = null },
            onConfirm = { title, artist, duration, link ->
                viewModel.updateSuggestion(editingSuggestion!!.id, title, artist, duration, link)
                editingSuggestion = null
            }
        )
    }
}

@Composable
fun SuggestionsList(
    suggestions: List<Suggestion>,
    currentUserId: String,
    onVote: (String) -> Unit,
    onConvert: (Suggestion) -> Unit,
    onDelete: (String) -> Unit,
    onEdit: (Suggestion) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(suggestions, key = { it.id }) { suggestion ->
            SuggestionCard(
                suggestion = suggestion,
                currentUserId = currentUserId,
                onVote = { onVote(suggestion.id) },
                onConvert = { onConvert(suggestion) },
                onDelete = { onDelete(suggestion.id) },
                onEdit = { onEdit(suggestion) }
            )
        }
    }
}

@Composable
fun SuggestionCard(
    suggestion: Suggestion,
    currentUserId: String,
    onVote: () -> Unit,
    onConvert: () -> Unit,
    onDelete: () -> Unit,
    onEdit: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }
    val uriHandler = LocalUriHandler.current
    val hasVoted = suggestion.hasUserVoted(currentUserId)
    val isCreator = suggestion.createdBy == currentUserId

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // En-tête avec titre et menu
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = suggestion.title,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = suggestion.artist,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (suggestion.duration > 0) {
                        val mins = suggestion.duration / 60
                        val secs = suggestion.duration % 60
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
                        text = { Text("Convertir en morceau") },
                        onClick = {
                            onConvert()
                            showMenu = false
                        },
                        leadingIcon = {
                            Icon(Icons.Default.Check, contentDescription = null)
                        }
                    )
                    if (isCreator) {
                        DropdownMenuItem(
                            text = { Text("Modifier") },
                            onClick = {
                                onEdit()
                                showMenu = false
                            },
                            leadingIcon = {
                                Icon(Icons.Default.Edit, contentDescription = null)
                            }
                        )
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
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Informations supplémentaires
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Proposé par ${suggestion.createdByName}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Bouton de vote
                FilledTonalButton(
                    onClick = onVote,
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = if (hasVoted) 
                            MaterialTheme.colorScheme.primaryContainer 
                        else 
                            MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Icon(
                        imageVector = if (hasVoted) Icons.Default.ThumbUp else Icons.Default.ThumbUpOffAlt,
                        contentDescription = "Voter",
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("${suggestion.voteCount}")
                }
            }

            // Lien si présent
            suggestion.link?.let { link ->
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        Icons.Default.Link,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = link,
                        style = MaterialTheme.typography.bodySmall.copy(
                            textDecoration = TextDecoration.Underline
                        ),
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.fillMaxWidth().clickable {
                            try {
                                val url = if (link.startsWith("http")) link else "https://$link"
                                uriHandler.openUri(url)
                            } catch (e: Exception) {
                                // Gérer l'erreur si l'URL est malformée
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun EmptySuggestionsView(onAddClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Lightbulb,
            contentDescription = null,
            modifier = Modifier.size(120.dp),
            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Text(
            text = "Aucune suggestion",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Text(
            text = "Proposez un nouveau morceau à travailler",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        Button(onClick = onAddClick) {
            Icon(Icons.Default.Add, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Proposer un morceau")
        }
    }
}

@Composable
fun SuggestionDialog(
    suggestion: Suggestion? = null,
    onDismiss: () -> Unit,
    onConfirm: (title: String, artist: String, duration: Int, link: String?) -> Unit
) {
    var title by remember { mutableStateOf(suggestion?.title ?: "") }
    var artist by remember { mutableStateOf(suggestion?.artist ?: "") }
    var durationMinutes by remember { mutableStateOf(suggestion?.let { (it.duration / 60).toString() } ?: "") }
    var durationSeconds by remember { mutableStateOf(suggestion?.let { (it.duration % 60).toString() } ?: "") }
    var link by remember { mutableStateOf(suggestion?.link ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (suggestion == null) "Nouvelle suggestion" else "Modifier la suggestion") },
        text = {
            Column {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Titre du morceau") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = artist,
                    onValueChange = { artist = it },
                    label = { Text("Artiste") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(8.dp))
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
                OutlinedTextField(
                    value = link,
                    onValueChange = { link = it },
                    label = { Text("Lien (optionnel)") },
                    placeholder = { Text("YouTube, Spotify...") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
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
                        link.ifBlank { null }
                    )
                },
                enabled = title.isNotBlank() && artist.isNotBlank()
            ) {
                Text(if (suggestion == null) "Proposer" else "Enregistrer")
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
