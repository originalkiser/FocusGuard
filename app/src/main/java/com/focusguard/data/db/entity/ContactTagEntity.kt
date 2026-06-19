package com.focusguard.data.db.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "contact_tags",
    indices = [Index(value = ["contactLookupKey", "tag"], unique = true)]
)
data class ContactTagEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val contactLookupKey: String,
    val tag: String
)
