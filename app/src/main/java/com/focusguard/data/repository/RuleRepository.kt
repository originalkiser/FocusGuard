package com.focusguard.data.repository

import com.focusguard.data.db.dao.RuleDao
import com.focusguard.data.db.dao.ScheduleSlotDao
import com.focusguard.data.db.entity.RuleEntity
import com.focusguard.data.db.entity.ScheduleSlotEntity
import com.focusguard.domain.model.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RuleRepository @Inject constructor(
    private val ruleDao: RuleDao,
    private val scheduleSlotDao: ScheduleSlotDao
) {
    fun getAllRules(): Flow<List<Rule>> =
        ruleDao.getAllRules().map { list -> list.map { mapEntityToRule(it) } }

    suspend fun getActiveRulesList(): List<Rule> =
        ruleDao.getActiveRulesList().map { mapEntityToRule(it) }

    suspend fun getRuleById(id: Long): Rule? =
        ruleDao.getRuleById(id)?.let { mapEntityToRule(it) }

    suspend fun saveRule(rule: Rule): Long {
        val entity = mapRuleToEntity(rule)
        val savedId = ruleDao.insertRule(entity)
        // Replace existing slots with the current set
        scheduleSlotDao.deleteSlotsForRule(savedId)
        scheduleSlotDao.insertSlots(rule.scheduleSlots.map { slot ->
            ScheduleSlotEntity(
                ruleId = savedId,
                startHour = slot.startHour,
                startMinute = slot.startMinute,
                endHour = slot.endHour,
                endMinute = slot.endMinute,
                daysOfWeekMask = slot.daysOfWeekMask,
                startDate = slot.startDate,
                endDate = slot.endDate
            )
        })
        return savedId
    }

    suspend fun deleteRule(rule: Rule) {
        ruleDao.getRuleById(rule.id)?.let { ruleDao.deleteRule(it) }
    }

    suspend fun setRuleEnabled(id: Long, enabled: Boolean) {
        ruleDao.setEnabled(id, enabled)
    }

    // ── Mapping helpers ──────────────────────────────────────────────────────

    private suspend fun mapEntityToRule(entity: RuleEntity): Rule {
        val slots = scheduleSlotDao.getSlotsForRuleList(entity.id).map { s ->
            ScheduleSlot(
                id = s.id,
                startHour = s.startHour,
                startMinute = s.startMinute,
                endHour = s.endHour,
                endMinute = s.endMinute,
                daysOfWeekMask = s.daysOfWeekMask,
                startDate = s.startDate,
                endDate = s.endDate
            )
        }

        val filter: ContactFilter = when (entity.ruleType) {
            "COMPANY"   -> ContactFilter.Company(entity.ruleValue)
            "AREA_CODE" -> ContactFilter.AreaCode(entity.ruleValue)
            "PATTERN"   -> ContactFilter.PhonePattern(entity.ruleValue)
            "GROUP"     -> {
                val parts = entity.ruleValue.split(":", limit = 2)
                ContactFilter.Group(
                    groupId = parts.getOrElse(0) { "" },
                    groupName = parts.getOrElse(1) { "" }
                )
            }
            "TAG"  -> ContactFilter.Tag(entity.ruleValue)
            "ALL"  -> ContactFilter.All
            else   -> ContactFilter.Individual(
                entity.contactLookupKeys.split(",").filter { it.isNotBlank() }
            )
        }

        return Rule(
            id = entity.id,
            name = entity.name,
            mode = if (entity.mode == "BLOCK") RuleMode.BLOCK else RuleMode.ALLOW,
            enabled = entity.enabled,
            priority = entity.priority,
            contactFilter = filter,
            suppressCalls = entity.suppressCalls,
            suppressSms = entity.suppressSms,
            suppressNotifications = entity.suppressNotifications,
            autoReply = if (entity.autoReplyEnabled) AutoReplyConfig(entity.autoReplyMessage) else null,
            emergencyBypass = if (entity.emergencyBypassEnabled) EmergencyBypassConfig(
                entity.emergencyBypassCount, entity.emergencyBypassMinutes
            ) else null,
            allowStarredBypass = entity.allowStarredBypass,
            scheduleSlots = slots
        )
    }

    private fun mapRuleToEntity(rule: Rule): RuleEntity {
        val (ruleType, ruleValue, lookupKeys) = when (val f = rule.contactFilter) {
            is ContactFilter.Individual  -> Triple("INDIVIDUAL", "", f.lookupKeys.joinToString(","))
            is ContactFilter.Company     -> Triple("COMPANY", f.companyName, "")
            is ContactFilter.AreaCode    -> Triple("AREA_CODE", f.areaCode, "")
            is ContactFilter.PhonePattern -> Triple("PATTERN", f.pattern, "")
            is ContactFilter.Group       -> Triple("GROUP", "${f.groupId}:${f.groupName}", "")
            is ContactFilter.Tag         -> Triple("TAG", f.tag, "")
            ContactFilter.All            -> Triple("ALL", "", "")
        }

        return RuleEntity(
            id = rule.id,
            name = rule.name,
            mode = rule.mode.name,
            enabled = rule.enabled,
            priority = rule.priority,
            contactLookupKeys = lookupKeys,
            ruleType = ruleType,
            ruleValue = ruleValue,
            suppressCalls = rule.suppressCalls,
            suppressSms = rule.suppressSms,
            suppressNotifications = rule.suppressNotifications,
            autoReplyEnabled = rule.autoReply != null,
            autoReplyMessage = rule.autoReply?.message ?: "",
            emergencyBypassEnabled = rule.emergencyBypass != null,
            emergencyBypassCount = rule.emergencyBypass?.callCount ?: 3,
            emergencyBypassMinutes = rule.emergencyBypass?.withinMinutes ?: 5,
            allowStarredBypass = rule.allowStarredBypass,
            updatedAt = System.currentTimeMillis()
        )
    }
}
