package com.focusguard.data.db.dao

import androidx.room.*
import com.focusguard.data.db.entity.EmergencyBypassEvent

@Dao
interface EmergencyBypassDao {

    @Insert
    suspend fun recordCall(event: EmergencyBypassEvent)

    @Query("""
        SELECT COUNT(*) FROM emergency_bypass_events
        WHERE ruleId = :ruleId AND callerNumber = :number AND timestamp > :since
    """)
    suspend fun getCallCount(ruleId: Long, number: String, since: Long): Int

    @Query("DELETE FROM emergency_bypass_events WHERE timestamp < :before")
    suspend fun pruneOldEvents(before: Long)
}
