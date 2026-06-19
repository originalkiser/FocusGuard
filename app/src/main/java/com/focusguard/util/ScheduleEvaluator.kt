package com.focusguard.util

import com.focusguard.domain.model.ScheduleSlot
import java.util.Calendar

object ScheduleEvaluator {

    /** Returns true if the current wall-clock time falls within any of the provided slots. */
    fun isActiveNow(slots: List<ScheduleSlot>): Boolean {
        if (slots.isEmpty()) return true  // no schedule = always active
        val now = Calendar.getInstance()
        return slots.any { isSlotActiveAt(it, now) }
    }

    private fun isSlotActiveAt(slot: ScheduleSlot, cal: Calendar): Boolean {
        val nowMillis = cal.timeInMillis

        if (slot.startDate != null && nowMillis < slot.startDate) return false
        if (slot.endDate != null && nowMillis > slot.endDate) return false

        // daysOfWeekMask: bit 0=Mon … bit 6=Sun; 0 means any day
        if (slot.daysOfWeekMask != 0) {
            // Calendar.DAY_OF_WEEK: Sun=1, Mon=2 … Sat=7
            // Our bit index: Mon=0 … Sun=6
            val bitIndex = (cal.get(Calendar.DAY_OF_WEEK) - 2 + 7) % 7
            if ((slot.daysOfWeekMask shr bitIndex) and 1 == 0) return false
        }

        val nowMinutes   = cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)
        val startMinutes = slot.startHour * 60 + slot.startMinute
        val endMinutes   = slot.endHour * 60 + slot.endMinute

        return if (startMinutes <= endMinutes) {
            nowMinutes in startMinutes..endMinutes
        } else {
            // Overnight span (e.g., 22:00–06:00 wraps midnight)
            nowMinutes >= startMinutes || nowMinutes <= endMinutes
        }
    }
}
