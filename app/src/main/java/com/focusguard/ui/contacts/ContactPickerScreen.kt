package com.focusguard.ui.contacts

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContactPickerScreen(
    preSelectedKeys: Set<String> = emptySet(),
    onDone: (Set<String>) -> Unit,
    viewModel: ContactPickerViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) { viewModel.init(preSelectedKeys) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Select Contacts") },
                navigationIcon = {
                    IconButton(onClick = { onDone(viewModel.getSelectedKeys()) }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    TextButton(onClick = { onDone(viewModel.getSelectedKeys()) }) {
                        Text("Done (${uiState.selectedLookupKeys.size})")
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            OutlinedTextField(
                value = uiState.searchQuery,
                onValueChange = viewModel::search,
                placeholder = { Text("Search contacts…") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            )

            if (uiState.isLoading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
                return@Scaffold
            }

            if (uiState.filtered.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No contacts found", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                return@Scaffold
            }

            LazyColumn {
                items(uiState.filtered, key = { it.lookupKey }) { contact ->
                    val selected = contact.lookupKey in uiState.selectedLookupKeys
                    ListItem(
                        headlineContent = { Text(contact.displayName) },
                        supportingContent = {
                            Text(contact.phoneNumbers.firstOrNull()?.number ?: "")
                        },
                        leadingContent = {
                            Icon(Icons.Default.Person, null,
                                tint = MaterialTheme.colorScheme.primary)
                        },
                        trailingContent = {
                            Checkbox(
                                checked = selected,
                                onCheckedChange = { viewModel.toggleContact(contact.lookupKey) }
                            )
                        },
                        modifier = Modifier.clickable { viewModel.toggleContact(contact.lookupKey) }
                    )
                    HorizontalDivider()
                }
            }
        }
    }
}
