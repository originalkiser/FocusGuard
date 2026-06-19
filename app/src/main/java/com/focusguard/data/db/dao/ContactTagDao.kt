package com.focusguard.data.db.dao

import androidx.room.*
import com.focusguard.data.db.entity.ContactTagEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ContactTagDao {

    @Query("SELECT * FROM contact_tags WHERE contactLookupKey = :lookupKey")
    fun getTagsForContact(lookupKey: String): Flow<List<ContactTagEntity>>

    @Query("SELECT DISTINCT tag FROM contact_tags ORDER BY tag ASC")
    fun getAllTags(): Flow<List<String>>

    @Query("SELECT contactLookupKey FROM contact_tags WHERE tag = :tag")
    suspend fun getContactsWithTag(tag: String): List<String>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertTag(tag: ContactTagEntity)

    @Query("DELETE FROM contact_tags WHERE contactLookupKey = :lookupKey AND tag = :tag")
    suspend fun deleteTagForContact(lookupKey: String, tag: String)

    @Delete
    suspend fun deleteTag(tag: ContactTagEntity)
}
