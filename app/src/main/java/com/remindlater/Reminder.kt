package com.remindlater

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "reminders")
data class Reminder(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val title: String,
    val description: String,
    val dateTime: Long,
    val isCompleted: Boolean = false,
    val isVoiceReminder: Boolean = false
)
