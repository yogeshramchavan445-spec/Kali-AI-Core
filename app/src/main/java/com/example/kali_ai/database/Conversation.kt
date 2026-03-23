package com.example.kali_ai.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "conversations")
data class Conversation(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val question: String,
    val answer: String,
    val command: String?,
    val timestamp: Long = System.currentTimeMillis(),
    val syncedToGitHub: Boolean = false,
    val source: String = "local" // local, deepseek, gemini, github, offline_command
)
