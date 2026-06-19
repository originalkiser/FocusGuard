package com.focusguard.util

import android.content.ContentResolver
import android.net.Uri
import android.provider.ContactsContract
import com.focusguard.domain.model.Contact
import com.focusguard.domain.model.PhoneNumber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ContactQueryHelper @Inject constructor(
    private val contentResolver: ContentResolver
) {
    /**
     * Looks up a phone number in the system Contacts Provider.
     * Returns null if the number isn't in the user's contacts.
     * This is the hot path called for every inbound call/notification.
     */
    fun resolveNumber(phoneNumber: String): Contact? {
        val uri = Uri.withAppendedPath(
            ContactsContract.PhoneLookup.CONTENT_FILTER_URI,
            Uri.encode(phoneNumber)
        )
        // PhoneLookup doesn't expose COMPANY — company is fetched separately if needed
        val cursor = contentResolver.query(
            uri,
            arrayOf(
                ContactsContract.PhoneLookup.LOOKUP_KEY,   // 0
                ContactsContract.PhoneLookup.DISPLAY_NAME, // 1
                ContactsContract.PhoneLookup.STARRED,      // 2
                ContactsContract.PhoneLookup._ID           // 3
            ),
            null, null, null
        ) ?: return null

        return cursor.use {
            if (!it.moveToFirst()) return null
            Contact(
                id = it.getString(3) ?: return null,
                lookupKey = it.getString(0) ?: return null,
                displayName = it.getString(1) ?: phoneNumber,
                company = null,  // lazy-loaded via queryByCompany when needed for COMPANY rules
                isStarred = it.getInt(2) != 0,
                phoneNumbers = listOf(PhoneNumber(phoneNumber, phoneNumber, 0))
            )
        }
    }

    /**
     * Returns all contacts that match a company name (partial, case-insensitive).
     * Used at rule-creation time to preview matches, and at evaluation time for COMPANY rules.
     */
    fun queryByCompany(company: String): List<String> {
        val keys = mutableListOf<String>()
        val cursor = contentResolver.query(
            ContactsContract.Data.CONTENT_URI,
            arrayOf(ContactsContract.Data.LOOKUP_KEY),
            "${ContactsContract.CommonDataKinds.Organization.COMPANY} LIKE ? AND " +
            "${ContactsContract.Data.MIMETYPE} = ?",
            arrayOf("%$company%", ContactsContract.CommonDataKinds.Organization.CONTENT_ITEM_TYPE),
            null
        ) ?: return emptyList()
        cursor.use { while (it.moveToNext()) it.getString(0)?.let { k -> keys.add(k) } }
        return keys
    }

    /** Returns lookup keys for all members of a contact group. */
    fun queryByGroup(groupId: String): List<String> {
        val keys = mutableListOf<String>()
        val cursor = contentResolver.query(
            ContactsContract.Data.CONTENT_URI,
            arrayOf(ContactsContract.Data.LOOKUP_KEY),
            "${ContactsContract.CommonDataKinds.GroupMembership.GROUP_ROW_ID} = ? AND " +
            "${ContactsContract.Data.MIMETYPE} = ?",
            arrayOf(groupId, ContactsContract.CommonDataKinds.GroupMembership.CONTENT_ITEM_TYPE),
            null
        ) ?: return emptyList()
        cursor.use { while (it.moveToNext()) it.getString(0)?.let { k -> keys.add(k) } }
        return keys
    }

    /**
     * Looks up a contact by their display name. Used when a notification title contains a name
     * instead of a phone number (e.g. Google Messages, Samsung Messages).
     */
    fun resolveByName(displayName: String): Contact? {
        val cursor = contentResolver.query(
            ContactsContract.Contacts.CONTENT_URI,
            arrayOf(
                ContactsContract.Contacts._ID,
                ContactsContract.Contacts.LOOKUP_KEY,
                ContactsContract.Contacts.DISPLAY_NAME_PRIMARY,
                ContactsContract.Contacts.STARRED
            ),
            "${ContactsContract.Contacts.DISPLAY_NAME_PRIMARY} = ? AND ${ContactsContract.Contacts.HAS_PHONE_NUMBER} = 1",
            arrayOf(displayName),
            null
        ) ?: return null

        return cursor.use {
            if (!it.moveToFirst()) return null
            val id         = it.getString(0) ?: return null
            val lookupKey  = it.getString(1) ?: return null
            val name       = it.getString(2) ?: displayName
            val starred    = it.getInt(3) != 0
            Contact(
                id = id,
                lookupKey = lookupKey,
                displayName = name,
                company = null,
                isStarred = starred,
                phoneNumbers = getPhoneNumbers(id)
            )
        }
    }

    /** Loads all contacts that have at least one phone number, sorted by display name. */
    fun loadAllContacts(): List<Contact> {
        val contacts = mutableListOf<Contact>()
        val cursor = contentResolver.query(
            ContactsContract.Contacts.CONTENT_URI,
            arrayOf(
                ContactsContract.Contacts._ID,
                ContactsContract.Contacts.LOOKUP_KEY,
                ContactsContract.Contacts.DISPLAY_NAME_PRIMARY,
                ContactsContract.Contacts.STARRED
            ),
            "${ContactsContract.Contacts.HAS_PHONE_NUMBER} = 1",
            null,
            "${ContactsContract.Contacts.DISPLAY_NAME_PRIMARY} ASC"
        ) ?: return emptyList()

        cursor.use {
            while (it.moveToNext()) {
                val id = it.getString(0) ?: continue
                val lookupKey = it.getString(1) ?: continue
                val name = it.getString(2) ?: continue
                val starred = it.getInt(3) != 0
                contacts.add(Contact(
                    id = id,
                    lookupKey = lookupKey,
                    displayName = name,
                    phoneNumbers = getPhoneNumbers(id),
                    isStarred = starred
                ))
            }
        }
        return contacts
    }

    private fun getPhoneNumbers(contactId: String): List<PhoneNumber> {
        val phones = mutableListOf<PhoneNumber>()
        val cursor = contentResolver.query(
            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            arrayOf(
                ContactsContract.CommonDataKinds.Phone.NUMBER,
                ContactsContract.CommonDataKinds.Phone.TYPE
            ),
            "${ContactsContract.CommonDataKinds.Phone.CONTACT_ID} = ?",
            arrayOf(contactId),
            null
        ) ?: return emptyList()
        cursor.use {
            while (it.moveToNext()) {
                val number = it.getString(0) ?: continue
                phones.add(PhoneNumber(number, number, it.getInt(1)))
            }
        }
        return phones
    }

    /** Loads all contact groups for group-based rule creation. */
    fun loadContactGroups(): List<Pair<String, String>> {
        val groups = mutableListOf<Pair<String, String>>()
        val cursor = contentResolver.query(
            ContactsContract.Groups.CONTENT_URI,
            arrayOf(ContactsContract.Groups._ID, ContactsContract.Groups.TITLE),
            "${ContactsContract.Groups.DELETED} = 0",
            null,
            "${ContactsContract.Groups.TITLE} ASC"
        ) ?: return emptyList()
        cursor.use {
            while (it.moveToNext()) {
                val id = it.getString(0) ?: continue
                val title = it.getString(1) ?: continue
                groups.add(id to title)
            }
        }
        return groups
    }
}
