package com.focusguard.di

import android.content.Context
import androidx.room.Room
import com.focusguard.data.db.FocusGuardDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): FocusGuardDatabase =
        Room.databaseBuilder(context, FocusGuardDatabase::class.java, "focusguard.db")
            // Add migrations here as the schema evolves:
            // .addMigrations(MIGRATION_1_2)
            .fallbackToDestructiveMigrationOnDowngrade()
            .build()

    @Provides fun provideRuleDao(db: FocusGuardDatabase) = db.ruleDao()
    @Provides fun provideScheduleSlotDao(db: FocusGuardDatabase) = db.scheduleSlotDao()
    @Provides fun provideContactTagDao(db: FocusGuardDatabase) = db.contactTagDao()
    @Provides fun provideEmergencyBypassDao(db: FocusGuardDatabase) = db.emergencyBypassDao()
}
