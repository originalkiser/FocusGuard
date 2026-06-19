package com.focusguard.domain.model

data class Rule(
    val id: Long = 0,
    val name: String,
    val mode: RuleMode,
    val enabled: Boolean = true,
    val priority: Int = 0,
    val contactFilter: ContactFilter,
    val suppressCalls: Boolean = true,
    val suppressSms: Boolean = true,
    val suppressNotifications: Boolean = true,
    val autoReply: AutoReplyConfig? = null,
    val emergencyBypass: EmergencyBypassConfig? = null,
    val allowStarredBypass: Boolean = false,
    val scheduleSlots: List<ScheduleSlot> = emptyList()
)

enum class RuleMode { BLOCK, ALLOW }

sealed class ContactFilter {
    data class Individual(val lookupKeys: List<String>) : ContactFilter()
    data class Company(val companyName: String) : ContactFilter()
    data class AreaCode(val areaCode: String) : ContactFilter()
    data class PhonePattern(val pattern: String) : ContactFilter()  // supports * wildcard
    data class Group(val groupId: String, val groupName: String) : ContactFilter()
    data class Tag(val tag: String) : ContactFilter()
    data object All : ContactFilter()
}

data class ScheduleSlot(
    val id: Long = 0,
    val startHour: Int,
    val startMinute: Int,
    val endHour: Int,
    val endMinute: Int,
    val daysOfWeekMask: Int = 0,  // 0 = every day
    val startDate: Long? = null,
    val endDate: Long? = null
)

data class AutoReplyConfig(val message: String)

data class EmergencyBypassConfig(
    val callCount: Int = 3,
    val withinMinutes: Int = 5
)
