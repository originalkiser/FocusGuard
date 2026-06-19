package com.focusguard.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.focusguard.data.db.dao.*
import com.focusguard.data.db.entity.*

@Database(
    entities = [
        RuleEntity::class,
        ScheduleSlotEntity::class,
        ContactTagEntity::class,
        EmergencyBypassEvent::class
    ],
    version = 1,
    exportSchema = true  // schema files land in app/schemas/ for migration tracking
)
abstract class FocusGuardDatabase : RoomDatabase() {
    abstract fun ruleDao(): RuleDao
    abstract fun scheduleSlotDao(): ScheduleSlotDao
    abstract fun contactTagDao(): ContactTagDao
    abstract fun emergencyBypassDao(): EmergencyBypassDao
}
