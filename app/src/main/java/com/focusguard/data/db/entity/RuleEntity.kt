package com.focusguard.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "rules")
data class RuleEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val mode: String,                     // "BLOCK" | "ALLOW"
    val enabled: Boolean = true,
    val priority: Int = 0,
    val contactLookupKeys: String = "",   // comma-separated lookup keys for INDIVIDUAL filter
    val ruleType: String = "INDIVIDUAL",  // INDIVIDUAL | COMPANY | AREA_CODE | PATTERN | GROUP | TAG | ALL
    val ruleValue: String = "",           // company name, area code, pattern, "groupId:groupName", or tag
    val suppressCalls: Boolean = true,
    val suppressSms: Boolean = true,
    val suppressNotifications: Boolean = true,
    val autoReplyEnabled: Boolean = false,
    val autoReplyMessage: String = "",
    val emergencyBypassEnabled: Boolean = false,
    val emergencyBypassCount: Int = 3,    // allow through after X calls...
    val emergencyBypassMinutes: Int = 5,  // ...within Y minutes
    val allowStarredBypass: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
