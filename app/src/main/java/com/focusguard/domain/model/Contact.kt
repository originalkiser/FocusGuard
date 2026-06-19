package com.focusguard.domain.model

data class Contact(
    val id: String,
    val lookupKey: String,
    val displayName: String,
    val phoneNumbers: List<PhoneNumber> = emptyList(),
    val company: String? = null,
    val isStarred: Boolean = false,
    val tags: List<String> = emptyList()
)

data class PhoneNumber(
    val number: String,
    val normalized: String,
    val type: Int  // ContactsContract.CommonDataKinds.Phone.TYPE_*
)
