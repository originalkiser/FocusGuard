package com.focusguard.ui.rules

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.focusguard.data.repository.RuleRepository
import com.focusguard.domain.model.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class RuleWizardViewModel @Inject constructor(
    private val ruleRepository: RuleRepository
) : ViewModel() {

    enum class Step { CONTACTS, OPTIONS, SCHEDULE, REVIEW }

    enum class FilterType {
        INDIVIDUAL, COMPANY, AREA_CODE, PATTERN, GROUP, TAG;

        val displayName: String get() = when (this) {
            INDIVIDUAL -> "Specific Contacts"
            COMPANY    -> "Company / Organization"
            AREA_CODE  -> "Area Code"
            PATTERN    -> "Number Pattern"
            GROUP      -> "Contact Group"
            TAG        -> "Custom Tag"
        }
    }

    data class ScheduleSlotDraft(
        val id: Long = 0,
        val startHour: Int = 22,
        val startMinute: Int = 0,
        val endHour: Int = 8,
        val endMinute: Int = 0,
        val daysOfWeekMask: Int = 0
    )

    data class UiState(
        val step: Step = Step.CONTACTS,
        val existingRuleId: Long? = null,
        val name: String = "",
        val nameError: String? = null,
        val mode: RuleMode = RuleMode.BLOCK,
        val filterType: FilterType = FilterType.INDIVIDUAL,
        val selectedContactKeys: Set<String> = emptySet(),
        val filterValue: String = "",           // for COMPANY, AREA_CODE, PATTERN, GROUP id, TAG
        val filterDisplayName: String = "",     // human-readable label for GROUP
        val suppressCalls: Boolean = true,
        val suppressSms: Boolean = true,
        val suppressNotifications: Boolean = true,
        val autoReplyEnabled: Boolean = false,
        val autoReplyMessage: String = "I'm unavailable right now.",
        val emergencyBypassEnabled: Boolean = false,
        val emergencyBypassCount: Int = 3,
        val emergencyBypassMinutes: Int = 5,
        val allowStarredBypass: Boolean = false,
        val hasSchedule: Boolean = false,
        val scheduleSlots: List<ScheduleSlotDraft> = emptyList(),
        val isSaving: Boolean = false,
        val saveSuccess: Boolean = false
    )

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    fun loadRule(ruleId: Long) {
        viewModelScope.launch {
            val rule = ruleRepository.getRuleById(ruleId) ?: return@launch
            _uiState.update { buildStateFromRule(rule) }
        }
    }

    private fun buildStateFromRule(rule: Rule): UiState {
        val filter = rule.contactFilter
        return UiState(
            existingRuleId = rule.id,
            name = rule.name,
            mode = rule.mode,
            filterType = when (filter) {
                is ContactFilter.Individual  -> FilterType.INDIVIDUAL
                is ContactFilter.Company     -> FilterType.COMPANY
                is ContactFilter.AreaCode    -> FilterType.AREA_CODE
                is ContactFilter.PhonePattern -> FilterType.PATTERN
                is ContactFilter.Group       -> FilterType.GROUP
                is ContactFilter.Tag         -> FilterType.TAG
                ContactFilter.All            -> FilterType.INDIVIDUAL
            },
            selectedContactKeys = (filter as? ContactFilter.Individual)?.lookupKeys?.toSet() ?: emptySet(),
            filterValue = when (filter) {
                is ContactFilter.Company      -> filter.companyName
                is ContactFilter.AreaCode     -> filter.areaCode
                is ContactFilter.PhonePattern -> filter.pattern
                is ContactFilter.Group        -> filter.groupId
                is ContactFilter.Tag          -> filter.tag
                else                          -> ""
            },
            filterDisplayName = (filter as? ContactFilter.Group)?.groupName ?: "",
            suppressCalls = rule.suppressCalls,
            suppressSms = rule.suppressSms,
            suppressNotifications = rule.suppressNotifications,
            autoReplyEnabled = rule.autoReply != null,
            autoReplyMessage = rule.autoReply?.message ?: "I'm unavailable right now.",
            emergencyBypassEnabled = rule.emergencyBypass != null,
            emergencyBypassCount = rule.emergencyBypass?.callCount ?: 3,
            emergencyBypassMinutes = rule.emergencyBypass?.withinMinutes ?: 5,
            allowStarredBypass = rule.allowStarredBypass,
            hasSchedule = rule.scheduleSlots.isNotEmpty(),
            scheduleSlots = rule.scheduleSlots.map { s ->
                ScheduleSlotDraft(
                    id = s.id,
                    startHour = s.startHour, startMinute = s.startMinute,
                    endHour = s.endHour, endMinute = s.endMinute,
                    daysOfWeekMask = s.daysOfWeekMask
                )
            }
        )
    }

    // ── Field updaters ────────────────────────────────────────────────────────

    fun updateName(v: String) = _uiState.update { it.copy(name = v, nameError = null) }
    fun updateMode(v: RuleMode) = _uiState.update { it.copy(mode = v) }
    fun updateFilterType(v: FilterType) = _uiState.update { it.copy(filterType = v) }
    fun updateFilterValue(v: String) = _uiState.update { it.copy(filterValue = v) }
    fun updateSelectedContacts(keys: Set<String>) = _uiState.update { it.copy(selectedContactKeys = keys) }
    fun updateSuppressCalls(v: Boolean) = _uiState.update { it.copy(suppressCalls = v) }
    fun updateSuppressSms(v: Boolean) = _uiState.update { it.copy(suppressSms = v) }
    fun updateSuppressNotifications(v: Boolean) = _uiState.update { it.copy(suppressNotifications = v) }
    fun updateAutoReplyEnabled(v: Boolean) = _uiState.update { it.copy(autoReplyEnabled = v) }
    fun updateAutoReplyMessage(v: String) = _uiState.update { it.copy(autoReplyMessage = v) }
    fun updateEmergencyBypassEnabled(v: Boolean) = _uiState.update { it.copy(emergencyBypassEnabled = v) }
    fun updateEmergencyBypassCount(v: Int) = _uiState.update { it.copy(emergencyBypassCount = v) }
    fun updateEmergencyBypassMinutes(v: Int) = _uiState.update { it.copy(emergencyBypassMinutes = v) }
    fun updateAllowStarredBypass(v: Boolean) = _uiState.update { it.copy(allowStarredBypass = v) }
    fun updateHasSchedule(v: Boolean) = _uiState.update { it.copy(hasSchedule = v) }

    fun setScheduleSlots(slots: List<ScheduleSlotDraft>) = _uiState.update { it.copy(scheduleSlots = slots) }
    fun addScheduleSlot() = _uiState.update { it.copy(scheduleSlots = it.scheduleSlots + ScheduleSlotDraft()) }
    fun removeScheduleSlot(index: Int) = _uiState.update {
        it.copy(scheduleSlots = it.scheduleSlots.filterIndexed { i, _ -> i != index })
    }
    fun updateScheduleSlot(index: Int, slot: ScheduleSlotDraft) = _uiState.update {
        val list = it.scheduleSlots.toMutableList()
        list[index] = slot
        it.copy(scheduleSlots = list)
    }

    fun nextStep() = _uiState.update {
        val next = Step.entries.getOrElse(it.step.ordinal + 1) { Step.REVIEW }
        it.copy(step = next)
    }

    fun prevStep() = _uiState.update {
        val prev = Step.entries.getOrElse(it.step.ordinal - 1) { Step.CONTACTS }
        it.copy(step = prev)
    }

    fun save() {
        val s = _uiState.value
        if (s.name.isBlank()) {
            _uiState.update { it.copy(nameError = "Rule name is required") }
            return
        }

        val filter: ContactFilter = when (s.filterType) {
            FilterType.INDIVIDUAL -> ContactFilter.Individual(s.selectedContactKeys.toList())
            FilterType.COMPANY    -> ContactFilter.Company(s.filterValue)
            FilterType.AREA_CODE  -> ContactFilter.AreaCode(s.filterValue)
            FilterType.PATTERN    -> ContactFilter.PhonePattern(s.filterValue)
            FilterType.GROUP      -> ContactFilter.Group(s.filterValue, s.filterDisplayName)
            FilterType.TAG        -> ContactFilter.Tag(s.filterValue)
        }

        val rule = Rule(
            id = s.existingRuleId ?: 0L,
            name = s.name,
            mode = s.mode,
            contactFilter = filter,
            suppressCalls = s.suppressCalls,
            suppressSms = s.suppressSms,
            suppressNotifications = s.suppressNotifications,
            autoReply = if (s.autoReplyEnabled) AutoReplyConfig(s.autoReplyMessage) else null,
            emergencyBypass = if (s.emergencyBypassEnabled)
                EmergencyBypassConfig(s.emergencyBypassCount, s.emergencyBypassMinutes)
            else null,
            allowStarredBypass = s.allowStarredBypass,
            scheduleSlots = if (s.hasSchedule) s.scheduleSlots.map { d ->
                ScheduleSlot(
                    id = d.id,
                    startHour = d.startHour, startMinute = d.startMinute,
                    endHour = d.endHour, endMinute = d.endMinute,
                    daysOfWeekMask = d.daysOfWeekMask
                )
            } else emptyList()
        )

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            ruleRepository.saveRule(rule)
            _uiState.update { it.copy(isSaving = false, saveSuccess = true) }
        }
    }
}
