package com.focusguard.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.focusguard.data.repository.RuleRepository
import com.focusguard.domain.model.Rule
import com.focusguard.util.ScheduleEvaluator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val ruleRepository: RuleRepository
) : ViewModel() {

    data class RuleUiState(
        val rule: Rule,
        val isCurrentlyActive: Boolean
    )

    data class UiState(
        val rules: List<RuleUiState> = emptyList(),
        val isLoading: Boolean = true
    )

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            ruleRepository.getAllRules().collect { rules ->
                _uiState.update {
                    it.copy(
                        rules = rules.map { rule ->
                            RuleUiState(
                                rule = rule,
                                isCurrentlyActive = rule.enabled &&
                                    ScheduleEvaluator.isActiveNow(rule.scheduleSlots)
                            )
                        },
                        isLoading = false
                    )
                }
            }
        }
    }

    fun toggleRule(ruleId: Long, enabled: Boolean) {
        viewModelScope.launch { ruleRepository.setRuleEnabled(ruleId, enabled) }
    }

    fun deleteRule(rule: Rule) {
        viewModelScope.launch { ruleRepository.deleteRule(rule) }
    }
}
