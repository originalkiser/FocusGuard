package com.focusguard.data.db.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "schedule_slots",
    foreignKeys = [ForeignKey(
        entity = RuleEntity::class,
        parentColumns = ["id"],
        childColumns = ["ruleId"],
        onDelete = ForeignKey.CASCADE  // slots auto-deleted when parent rule is deleted
    )],
    indices = [Index("ruleId")]
)
data class ScheduleSlotEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val ruleId: Long,
    val startHour: Int,    // 0–23
    val startMinute: Int,  // 0–59
    val endHour: Int,
    val endMinute: Int,
    // Bitmask: bit 0=Mon, 1=Tue, 2=Wed, 3=Thu, 4=Fri, 5=Sat, 6=Sun
    // Value 0 means "every day" (no day restriction)
    val daysOfWeekMask: Int = 0,
    val startDate: Long? = null,  // epoch millis; null = no date range start
    val endDate: Long? = null     // epoch millis; null = no date range end
)
