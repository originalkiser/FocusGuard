package com.focusguard.data.db.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Records each call that was blocked so we can count consecutive attempts from the same number.
 * Used to implement the "allow through after X calls in Y minutes" emergency bypass feature.
 */
@Entity(
    tableName = "emergency_bypass_events",
    indices = [Index(value = ["ruleId", "callerNumber", "timestamp"])]
)
data class EmergencyBypassEvent(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val ruleId: Long,
    val callerNumber: String,
    val timestamp: Long = System.currentTimeMillis()
)
