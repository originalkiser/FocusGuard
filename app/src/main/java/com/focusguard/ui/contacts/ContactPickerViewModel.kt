package com.focusguard.ui.contacts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.focusguard.domain.model.Contact
import com.focusguard.util.ContactQueryHelper
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ContactPickerViewModel @Inject constructor(
    private val contactQueryHelper: ContactQueryHelper
) : ViewModel() {

    data class UiState(
        val allContacts: List<Contact> = emptyList(),
        val filtered: List<Contact> = emptyList(),
        val selectedLookupKeys: Set<String> = emptySet(),
        val searchQuery: String = "",
        val isLoading: Boolean = true
    )

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    /** Must be called before showing the picker with any pre-existing selection. */
    fun init(preSelectedKeys: Set<String>) {
        _uiState.update { it.copy(selectedLookupKeys = preSelectedKeys) }
        loadContacts()
    }

    private fun loadContacts() {
        viewModelScope.launch(Dispatchers.IO) {
            val contacts = contactQueryHelper.loadAllContacts()
            _uiState.update { it.copy(allContacts = contacts, filtered = contacts, isLoading = false) }
        }
    }

    fun search(query: String) {
        _uiState.update { state ->
            val filtered = if (query.isBlank()) state.allContacts
            else state.allContacts.filter {
                it.displayName.contains(query, ignoreCase = true) ||
                it.phoneNumbers.any { p -> p.number.contains(query) }
            }
            state.copy(searchQuery = query, filtered = filtered)
        }
    }

    fun toggleContact(lookupKey: String) {
        _uiState.update { state ->
            val keys = state.selectedLookupKeys.toMutableSet()
            if (!keys.add(lookupKey)) keys.remove(lookupKey)
            state.copy(selectedLookupKeys = keys)
        }
    }

    /** Returns the final selection when the user taps Done. */
    fun getSelectedKeys(): Set<String> = _uiState.value.selectedLookupKeys
}
