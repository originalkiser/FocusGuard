package com.focusguard.ui.dashboard

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.focusguard.domain.model.RuleMode
import com.focusguard.ui.theme.AllowGreen
import com.focusguard.ui.theme.BlockRed

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    onCreateRule: () -> Unit,
    onEditRule: (Long) -> Unit,
    onOpenSettings: () -> Unit,
    viewModel: DashboardViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("FocusGuard") },
                actions = {
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onCreateRule,
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("New Rule") }
            )
        }
    ) { padding ->
        if (uiState.isLoading) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        if (uiState.rules.isEmpty()) {
            EmptyState(
                modifier = Modifier.fillMaxSize().padding(padding),
                onCreateRule = onCreateRule
            )
        } else {
            LazyColumn(
                modifier = Modifier.padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(uiState.rules, key = { it.rule.id }) { ruleState ->
                    RuleCard(
                        ruleState = ruleState,
                        onToggle = { enabled -> viewModel.toggleRule(ruleState.rule.id, enabled) },
                        onEdit   = { onEditRule(ruleState.rule.id) },
                        onDelete = { viewModel.deleteRule(ruleState.rule) }
                    )
                }
                item { Spacer(Modifier.height(80.dp)) }  // clear the FAB
            }
        }
    }
}

@Composable
private fun EmptyState(modifier: Modifier = Modifier, onCreateRule: () -> Unit) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Default.Shield,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
        )
        Spacer(Modifier.height(16.dp))
        Text("No rules yet", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(8.dp))
        Text(
            "Create a rule to start filtering calls and notifications",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(24.dp))
        Button(onClick = onCreateRule) { Text("Create First Rule") }
    }
}

@Composable
private fun RuleCard(
    ruleState: DashboardViewModel.RuleUiState,
    onToggle: (Boolean) -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val rule = ruleState.rule
    var showDeleteDialog by remember { mutableStateOf(false) }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Rule") },
            text = { Text("Delete \"${rule.name}\"? This cannot be undone.") },
            confirmButton = {
                TextButton(onClick = { onDelete(); showDeleteDialog = false }) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("Cancel") }
            }
        )
    }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {

            // Header row: mode badge, name, enable toggle
            Row(verticalAlignment = Alignment.CenterVertically) {
                val (badgeColor, badgeLabel) = if (rule.mode == RuleMode.BLOCK)
                    BlockRed to "BLOCK" else AllowGreen to "ALLOW"
                Surface(
                    shape = MaterialTheme.shapes.small,
                    color = badgeColor.copy(alpha = 0.15f)
                ) {
                    Text(
                        badgeLabel,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = badgeColor
                    )
                }
                Spacer(Modifier.width(8.dp))
                Text(
                    rule.name,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Switch(checked = rule.enabled, onCheckedChange = onToggle)
            }

            Spacer(Modifier.height(6.dp))

            // Status line
            val (statusLabel, statusColor) = when {
                !rule.enabled             -> "Disabled" to MaterialTheme.colorScheme.onSurfaceVariant
                ruleState.isCurrentlyActive -> "Active now" to AllowGreen
                rule.scheduleSlots.isNotEmpty() -> "Scheduled" to MaterialTheme.colorScheme.onSurfaceVariant
                else                      -> "Active" to AllowGreen
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    if (ruleState.isCurrentlyActive && rule.enabled) Icons.Default.CheckCircle
                    else Icons.Default.RadioButtonUnchecked,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                    tint = statusColor
                )
                Spacer(Modifier.width(4.dp))
                Text(statusLabel, style = MaterialTheme.typography.bodySmall, color = statusColor)
            }

            // Feature chips
            if (rule.suppressCalls || rule.suppressSms || rule.suppressNotifications ||
                rule.autoReply != null || rule.emergencyBypass != null
            ) {
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    if (rule.suppressCalls) FeatureChip("Calls")
                    if (rule.suppressSms) FeatureChip("SMS")
                    if (rule.suppressNotifications) FeatureChip("Notifs")
                    if (rule.autoReply != null) FeatureChip("Auto-reply")
                    if (rule.emergencyBypass != null) FeatureChip("E-Bypass")
                }
            }

            // Action row
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = onEdit) { Text("Edit") }
                TextButton(onClick = { showDeleteDialog = true }) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

@Composable
private fun FeatureChip(label: String) {
    Surface(
        shape = MaterialTheme.shapes.extraSmall,
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Text(
            label,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            style = MaterialTheme.typography.labelSmall
        )
    }
}
