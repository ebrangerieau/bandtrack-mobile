package com.bandtrack.ui.groups

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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.bandtrack.data.models.Group

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupSelectorScreen(
    onGroupSelected: (Group) -> Unit,
    onLogout: () -> Unit,
    userName: String = "Utilisateur",
    viewModel: GroupSelectorViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showCreateDialog by remember { mutableStateOf(false) }
    var showJoinDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Mes Groupes") },
                actions = {
                    IconButton(onClick = viewModel::loadUserGroups) {
                        Icon(Icons.Default.Refresh, contentDescription = "Actualiser")
                    }
                    IconButton(onClick = onLogout) {
                        Icon(Icons.Default.ExitToApp, contentDescription = "Déconnexion")
                    }
                }
            )
        },
        containerColor = androidx.compose.ui.graphics.Color.Transparent,
        floatingActionButton = {
            Column {
                FloatingActionButton(
                    onClick = { showJoinDialog = true },
                    containerColor = MaterialTheme.colorScheme.secondary
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Rejoindre un groupe")
                }
                Spacer(modifier = Modifier.height(16.dp))
                FloatingActionButton(
                    onClick = { showCreateDialog = true }
                ) {
                    Icon(Icons.Default.AddCircle, contentDescription = "Créer un groupe")
                }
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when (val state = uiState) {
                is GroupUiState.Loading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                is GroupUiState.Success -> {
                    if (state.groups.isEmpty()) {
                        EmptyGroupsView(
                            onCreateGroup = { showCreateDialog = true },
                            onJoinGroup = { showJoinDialog = true }
                        )
                    } else {
                        GroupList(
                            groups = state.groups,
                            onGroupClick = onGroupSelected
                        )
                    }
                }
                is GroupUiState.Error -> {
                    ErrorView(
                        message = state.message,
                        onRetry = viewModel::loadUserGroups
                    )
                }
            }
        }
    }

    // Dialogue de création de groupe
    if (showCreateDialog) {
        CreateGroupDialog(
            onDismiss = { showCreateDialog = false },
            onConfirm = { name, description ->
                viewModel.createGroup(name, description, userName)
                showCreateDialog = false
            }
        )
    }

    // Dialogue pour rejoindre un groupe
    if (showJoinDialog) {
        JoinGroupDialog(
            onDismiss = { showJoinDialog = false },
            onConfirm = { code ->
                viewModel.joinGroupWithCode(code, userName)
                showJoinDialog = false
            }
        )
    }
}

@Composable
fun GroupList(
    groups: List<Group>,
    onGroupClick: (Group) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(groups) { group ->
            GroupCard(group = group, onClick = { onGroupClick(group) })
        }
    }
}

@Composable
fun GroupCard(
    group: Group,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.MusicNote,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = group.name,
                    style = MaterialTheme.typography.titleMedium
                )
                if (group.description.isNotBlank()) {
                    Text(
                        text = group.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    text = "${group.memberCount} membre(s)",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = "Sélectionner"
            )
        }
    }
}

@Composable
fun EmptyGroupsView(
    onCreateGroup: () -> Unit,
    onJoinGroup: () -> Unit
) {
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
            text = "Aucun groupe",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Text(
            text = "Créez votre premier groupe ou rejoignez-en un",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        Button(onClick = onCreateGroup) {
            Icon(Icons.Default.Add, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Créer un groupe")
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        OutlinedButton(onClick = onJoinGroup) {
            Icon(Icons.Default.QrCode, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Rejoindre un groupe")
        }
    }
}

@Composable
fun ErrorView(
    message: String,
    onRetry: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Error,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = MaterialTheme.colorScheme.error
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = "Erreur",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.error
        )
        
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Button(onClick = onRetry) {
            Icon(Icons.Default.Refresh, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Réessayer")
        }
    }
}

@Composable
fun CreateGroupDialog(
    onDismiss: () -> Unit,
    onConfirm: (name: String, description: String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Créer un groupe") },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nom du groupe") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description (optionnel)") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 3
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(name, description) },
                enabled = name.isNotBlank()
            ) {
                Text("Créer")
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
fun JoinGroupDialog(
    onDismiss: () -> Unit,
    onConfirm: (code: String) -> Unit
) {
    var code by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Rejoindre un groupe") },
        text = {
            Column {
                Text(
                    text = "Entrez le code d'invitation",
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = code,
                    onValueChange = { code = it.uppercase() },
                    label = { Text("Code d'invitation") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    placeholder = { Text("ABCD1234") }
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(code) },
                enabled = code.isNotBlank() && code.length >= 8
            ) {
                Text("Rejoindre")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Annuler")
            }
        }
    )
}
