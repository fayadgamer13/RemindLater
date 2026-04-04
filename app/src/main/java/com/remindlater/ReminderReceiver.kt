package com.remindlater

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val id = intent.getIntExtra("reminder_id", 0)
        val title = intent.getStringExtra("reminder_title") ?: ""
        val desc = intent.getStringExtra("reminder_desc") ?: ""
        val isVoice = intent.getBooleanExtra("is_voice", false)

        // We use WorkManager to handle the actual notification and TTS 
        // because BroadcastReceivers have a very short lifespan.
        val workRequest = OneTimeWorkRequestBuilder<ReminderWorker>()
            .setInputData(workDataOf(
                "reminder_id" to id,
                "reminder_title" to title,
                "reminder_desc" to desc,
                "is_voice" to isVoice
            ))
            .build()

        WorkManager.getInstance(context).enqueue(workRequest)
    }
}
