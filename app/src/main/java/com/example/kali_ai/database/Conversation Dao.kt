package com.example.kali_ai.database

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ConversationDao {
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertConversation(conv: Conversation): Long
    
    @Query("SELECT * FROM conversations ORDER BY timestamp DESC")
    fun getAllConversations(): Flow<List<Conversation>>
    
    @Query("SELECT * FROM conversations WHERE question LIKE :query LIMIT 1")
    suspend fun findMatchingConversation(query: String): Conversation?
    
    @Query("SELECT * FROM conversations WHERE syncedToGitHub = 0")
    suspend fun getUnsyncedConversations(): List<Conversation>
    
    @Query("UPDATE conversations SET syncedToGitHub = 1 WHERE id = :id")
    suspend fun markAsSynced(id: Int)
    
    @Query("SELECT COUNT(*) FROM conversations")
    suspend fun getCount(): Int
    
    @Query("DELETE FROM conversations")
    suspend fun deleteAll()
    
    @Query("SELECT * FROM conversations WHERE timestamp > :since ORDER BY timestamp DESC")
    suspend fun getConversationsSince(since: Long): List<Conversation>
}
