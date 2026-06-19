package com.focusguard.data.db.dao

import androidx.room.*
import com.focusguard.data.db.entity.ScheduleSlotEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ScheduleSlotDao {

    @Query("SELECT * FROM schedule_slots WHERE ruleId = :ruleId")
    fun getSlotsForRule(ruleId: Long): Flow<List<ScheduleSlotEntity>>

    @Query("SELECT * FROM schedule_slots WHERE ruleId = :ruleId")
    suspend fun getSlotsForRuleList(ruleId: Long): List<ScheduleSlotEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSlots(slots: List<ScheduleSlotEntity>)

    @Delete
    suspend fun deleteSlot(slot: ScheduleSlotEntity)

    @Query("DELETE FROM schedule_slots WHERE ruleId = :ruleId")
    suspend fun deleteSlotsForRule(ruleId: Long)
}
