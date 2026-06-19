package com.focusguard.ui.rules

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.hilt.navigation.compose.hiltViewModel
import com.focusguard.domain.model.RuleMode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RuleWizardScreen(
    ruleId: Long?,
    onNavigateBack: () -> Unit,
    onPickContacts: () -> Unit,
    viewModel: RuleWizardViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(ruleId) { ruleId?.let { viewModel.loadRule(it) } }
    LaunchedEffect(uiState.saveSuccess) { if (uiState.saveSuccess) onNavigateBack() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (ruleId != null) "Edit Rule" else "New Rule") },
                navigationIcon = {
                    IconButton(onClick = {
                        if (uiState.step == RuleWizardViewModel.Step.CONTACTS) onNavigateBack()
                        else viewModel.prevStep()
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            StepProgressBar(current = uiState.step)
            when (uiState.step) {
                RuleWizardViewModel.Step.CONTACTS -> ContactStep(
                    uiState, viewModel, onPickContacts
                )
                RuleWizardViewModel.Step.OPTIONS  -> OptionsStep(uiState, viewModel)
                RuleWizardViewModel.Step.SCHEDULE -> ScheduleStep(uiState, viewModel)
                RuleWizardViewModel.Step.REVIEW   -> ReviewStep(uiState, viewModel)
            }
        }
    }
}

// ── Step progress bar ─────────────────────────────────────────────────────────

@Composable
private fun StepProgressBar(current: RuleWizardViewModel.Step) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        RuleWizardViewModel.Step.entries.forEach { step ->
            val done = step.ordinal <= current.ordinal
            LinearProgressIndicator(
                progress = { if (done) 1f else 0f },
                modifier = Modifier.weight(1f).height(4.dp),
                color = if (done) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.surfaceVariant,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )
        }
    }
}

// ── Step 1: Contact / filter selection ───────────────────────────────────────

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ContactStep(
    s: RuleWizardViewModel.UiState,
    vm: RuleWizardViewModel,
    onPickContacts: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        OutlinedTextField(
            value = s.name,
            onValueChange = vm::updateName,
            label = { Text("Rule Name") },
            isError = s.nameError != null,
            supportingText = s.nameError?.let { { Text(it) } },
            modifier = Modifier.fillMaxWidth()
        )

        // Mode selection
        Text("Mode", style = MaterialTheme.typography.labelLarge)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = s.mode == RuleMode.BLOCK,
                onClick = { vm.updateMode(RuleMode.BLOCK) },
                label = { Text("Block") },
                leadingIcon = if (s.mode == RuleMode.BLOCK) {{ Icon(Icons.Default.Block, null, Modifier.size(FilterChipDefaults.IconSize)) }} else null
            )
            FilterChip(
                selected = s.mode == RuleMode.ALLOW,
                onClick = { vm.updateMode(RuleMode.ALLOW) },
                label = { Text("Allow Only") },
                leadingIcon = if (s.mode == RuleMode.ALLOW) {{ Icon(Icons.Default.CheckCircle, null, Modifier.size(FilterChipDefaults.IconSize)) }} else null
            )
        }
        if (s.mode == RuleMode.ALLOW) {
            Text(
                "Allow-Only mode silences everyone else while this rule is active (like a Focus mode).",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Filter type chips
        Text("Filter By", style = MaterialTheme.typography.labelLarge)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            RuleWizardViewModel.FilterType.entries.forEach { type ->
                FilterChip(
                    selected = s.filterType == type,
                    onClick = { vm.updateFilterType(type) },
                    label = { Text(type.displayName) }
                )
            }
        }

        // Filter-specific input
        when (s.filterType) {
            RuleWizardViewModel.FilterType.INDIVIDUAL -> {
                OutlinedButton(onClick = onPickContacts, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.PersonAdd, null)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        if (s.selectedContactKeys.isEmpty()) "Select Contacts"
                        else "${s.selectedContactKeys.size} contact(s) selected"
                    )
                }
            }
            RuleWizardViewModel.FilterType.COMPANY -> OutlinedTextField(
                value = s.filterValue, onValueChange = vm::updateFilterValue,
                label = { Text("Company name (partial match)") },
                modifier = Modifier.fillMaxWidth()
            )
            RuleWizardViewModel.FilterType.AREA_CODE -> OutlinedTextField(
                value = s.filterValue, onValueChange = vm::updateFilterValue,
                label = { Text("Area code (e.g. 212)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )
            RuleWizardViewModel.FilterType.PATTERN -> OutlinedTextField(
                value = s.filterValue, onValueChange = vm::updateFilterValue,
                label = { Text("Number pattern (* = wildcard, e.g. 1800*)") },
                modifier = Modifier.fillMaxWidth()
            )
            RuleWizardViewModel.FilterType.GROUP -> OutlinedTextField(
                value = s.filterValue, onValueChange = vm::updateFilterValue,
                label = { Text("Group name") },
                modifier = Modifier.fillMaxWidth()
            )
            RuleWizardViewModel.FilterType.TAG -> OutlinedTextField(
                value = s.filterValue, onValueChange = vm::updateFilterValue,
                label = { Text("Custom tag") },
                modifier = Modifier.fillMaxWidth()
            )
        }

        Spacer(Modifier.weight(1f))
        Button(onClick = vm::nextStep, modifier = Modifier.fillMaxWidth()) {
            Text("Next: Options")
        }
    }
}

// ── Step 2: Suppression options ───────────────────────────────────────────────

@Composable
private fun OptionsStep(s: RuleWizardViewModel.UiState, vm: RuleWizardViewModel) {
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text("What to suppress", style = MaterialTheme.typography.titleSmall)
        ToggleRow("Silence incoming calls", s.suppressCalls, vm::updateSuppressCalls)
        ToggleRow("Suppress SMS notification banners", s.suppressSms, vm::updateSuppressSms)
        ToggleRow("Suppress other app notifications", s.suppressNotifications, vm::updateSuppressNotifications)

        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
        Text("Auto-reply", style = MaterialTheme.typography.titleSmall)
        ToggleRow("Send auto-reply SMS when blocking", s.autoReplyEnabled, vm::updateAutoReplyEnabled)
        if (s.autoReplyEnabled) {
            OutlinedTextField(
                value = s.autoReplyMessage, onValueChange = vm::updateAutoReplyMessage,
                label = { Text("Message") }, minLines = 2, modifier = Modifier.fillMaxWidth()
            )
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
        Text("Emergency bypass", style = MaterialTheme.typography.titleSmall)
        ToggleRow("Allow through after repeated calls", s.emergencyBypassEnabled, vm::updateEmergencyBypassEnabled)
        if (s.emergencyBypassEnabled) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("Allow if called", style = MaterialTheme.typography.bodyMedium)
                NumberStepper(value = s.emergencyBypassCount, range = 2..10, onChange = vm::updateEmergencyBypassCount)
                Text("times within")
                NumberStepper(value = s.emergencyBypassMinutes, range = 1..30, onChange = vm::updateEmergencyBypassMinutes)
                Text("min")
            }
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
        ToggleRow("Always allow starred / favourite contacts", s.allowStarredBypass, vm::updateAllowStarredBypass)

        Spacer(Modifier.weight(1f))
        Button(onClick = vm::nextStep, modifier = Modifier.fillMaxWidth()) {
            Text("Next: Schedule")
        }
    }
}

// ── Step 3: Schedule ──────────────────────────────────────────────────────────

@Composable
private fun ScheduleStep(s: RuleWizardViewModel.UiState, vm: RuleWizardViewModel) {
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Schedule (Optional)", style = MaterialTheme.typography.titleSmall)
        Text(
            "Without a schedule the rule is always active.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        ToggleRow("Use a time schedule", s.hasSchedule, vm::updateHasSchedule)

        if (s.hasSchedule) {
            Text("Quick presets", style = MaterialTheme.typography.labelLarge)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AssistChip(onClick = {
                    vm.setScheduleSlots(listOf(RuleWizardViewModel.ScheduleSlotDraft(9, 0, 17, 0, 0b0011111)))
                }, label = { Text("Work hours") })
                AssistChip(onClick = {
                    vm.setScheduleSlots(listOf(RuleWizardViewModel.ScheduleSlotDraft(22, 0, 8, 0, 0)))
                }, label = { Text("Sleep") })
                AssistChip(onClick = {
                    vm.setScheduleSlots(listOf(RuleWizardViewModel.ScheduleSlotDraft(0, 0, 23, 59, 0b1100000)))
                }, label = { Text("Weekend") })
            }

            s.scheduleSlots.forEachIndexed { index, slot ->
                SlotEditor(
                    slot = slot,
                    onUpdate = { vm.updateScheduleSlot(index, it) },
                    onRemove = { vm.removeScheduleSlot(index) }
                )
            }

            OutlinedButton(onClick = vm::addScheduleSlot, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Default.Add, null)
                Spacer(Modifier.width(8.dp))
                Text("Add Time Slot")
            }
        }

        Spacer(Modifier.weight(1f))
        Button(onClick = vm::nextStep, modifier = Modifier.fillMaxWidth()) { Text("Review") }
    }
}

@Composable
private fun SlotEditor(
    slot: RuleWizardViewModel.ScheduleSlotDraft,
    onUpdate: (RuleWizardViewModel.ScheduleSlotDraft) -> Unit,
    onRemove: () -> Unit
) {
    Card {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Time Slot", style = MaterialTheme.typography.labelMedium)
                IconButton(onClick = onRemove, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Default.Close, "Remove", modifier = Modifier.size(16.dp))
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("From")
                TimeButton(slot.startHour, slot.startMinute) { h, m ->
                    onUpdate(slot.copy(startHour = h, startMinute = m))
                }
                Text("To")
                TimeButton(slot.endHour, slot.endMinute) { h, m ->
                    onUpdate(slot.copy(endHour = h, endMinute = m))
                }
            }
            DayPicker(mask = slot.daysOfWeekMask, onChanged = { onUpdate(slot.copy(daysOfWeekMask = it)) })
        }
    }
}

@Composable
private fun TimeButton(hour: Int, minute: Int, onSelect: (Int, Int) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        OutlinedButton(onClick = { expanded = true }) { Text("%02d:%02d".format(hour, minute)) }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            for (h in 0..23) for (m in listOf(0, 15, 30, 45)) {
                DropdownMenuItem(
                    text = { Text("%02d:%02d".format(h, m)) },
                    onClick = { onSelect(h, m); expanded = false }
                )
            }
        }
    }
}

@Composable
private fun DayPicker(mask: Int, onChanged: (Int) -> Unit) {
    val labels = listOf("M", "T", "W", "T", "F", "S", "S")
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
        Text("Days:", style = MaterialTheme.typography.labelSmall)
        labels.forEachIndexed { i, label ->
            val on = mask == 0 || (mask shr i) and 1 == 1
            FilterChip(
                selected = on,
                onClick = {
                    val newMask = if (mask == 0) (0b1111111 xor (1 shl i)) else (mask xor (1 shl i))
                    onChanged(if (newMask == 0b1111111 || newMask == 0) 0 else newMask)
                },
                label = { Text(label) },
                modifier = Modifier.defaultMinSize(minWidth = 36.dp)
            )
        }
    }
}

// ── Step 4: Review & save ─────────────────────────────────────────────────────

@Composable
private fun ReviewStep(s: RuleWizardViewModel.UiState, vm: RuleWizardViewModel) {
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text("Review Rule", style = MaterialTheme.typography.titleMedium)

        ReviewRow("Name", s.name.ifBlank { "(none)" })
        ReviewRow("Mode", if (s.mode == RuleMode.BLOCK) "Block" else "Allow Only")
        ReviewRow("Filter", buildString {
            append(s.filterType.displayName)
            if (s.filterType == RuleWizardViewModel.FilterType.INDIVIDUAL) {
                append(" — ${s.selectedContactKeys.size} contact(s)")
            } else if (s.filterValue.isNotBlank()) {
                append(" — \"${s.filterValue}\"")
            }
        })
        ReviewRow("Suppress", buildList {
            if (s.suppressCalls) add("Calls")
            if (s.suppressSms) add("SMS")
            if (s.suppressNotifications) add("Notifications")
        }.joinToString(", ").ifBlank { "Nothing" })
        if (s.autoReplyEnabled) ReviewRow("Auto-reply", s.autoReplyMessage)
        if (s.emergencyBypassEnabled)
            ReviewRow("Emergency bypass", "${s.emergencyBypassCount} calls in ${s.emergencyBypassMinutes} min")
        ReviewRow("Schedule", if (s.hasSchedule && s.scheduleSlots.isNotEmpty())
            "${s.scheduleSlots.size} time slot(s)" else "Always active")

        s.nameError?.let {
            Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        }

        Spacer(Modifier.weight(1f))
        Button(
            onClick = vm::save,
            enabled = !s.isSaving,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (s.isSaving) {
                CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                Spacer(Modifier.width(8.dp))
            }
            Text("Save Rule")
        }
    }
}

@Composable
private fun ReviewRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth()) {
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(110.dp)
        )
        Text(value, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
    }
}

// ── Shared composables ────────────────────────────────────────────────────────

@Composable
private fun ToggleRow(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun NumberStepper(value: Int, range: IntRange, onChange: (Int) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        IconButton(
            onClick = { if (value > range.first) onChange(value - 1) },
            modifier = Modifier.size(32.dp)
        ) { Icon(Icons.Default.Remove, null, Modifier.size(16.dp)) }
        Text(value.toString(), style = MaterialTheme.typography.bodyMedium, modifier = Modifier.widthIn(min = 24.dp))
        IconButton(
            onClick = { if (value < range.last) onChange(value + 1) },
            modifier = Modifier.size(32.dp)
        ) { Icon(Icons.Default.Add, null, Modifier.size(16.dp)) }
    }
}
