package com.remindlater

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.media.AudioAttributes
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import androidx.core.app.NotificationCompat
import androidx.work.Worker
import androidx.work.WorkerParameters
import java.util.*
import java.util.concurrent.CountDownLatch

class ReminderWorker(context: Context, params: WorkerParameters) : Worker(context, params) {
    private var tts: TextToSpeech? = null

    override fun doWork(): Result {
        val title = inputData.getString("reminder_title") ?: "Reminder"
        val desc = inputData.getString("reminder_desc") ?: ""
        val id = inputData.getInt("reminder_id", 0)
        val isVoice = inputData.getBoolean("is_voice", false)

        showNotification(title, desc, id)

        if (isVoice) {
            speakReminder(title, desc)
        }

        return Result.success()
    }

    private fun showNotification(title: String, desc: String, id: Int) {
        val channelId = "reminder_channel"
        val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val settingsManager = SettingsManager(applicationContext)

        val soundUri: Uri = if (settingsManager.useDefaultAlarm) {
            android.provider.Settings.System.DEFAULT_ALARM_ALERT_URI
        } else {
            settingsManager.customAlarmUri?.let { Uri.parse(it) } ?: android.provider.Settings.System.DEFAULT_ALARM_ALERT_URI
        }

        val audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ALARM)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        val channel = NotificationChannel(
            channelId,
            "Reminders",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            setSound(soundUri, audioAttributes)
        }
        notificationManager.createNotificationChannel(channel)

        val notification = NotificationCompat.Builder(applicationContext, channelId)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle(title)
            .setContentText(desc)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setSound(soundUri)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(id, notification)
    }

    private fun speakReminder(title: String, desc: String) {
        val latch = CountDownLatch(1)
        val handler = Handler(Looper.getMainLooper())
        
        handler.post {
            tts = TextToSpeech(applicationContext) { status ->
                if (status == TextToSpeech.SUCCESS) {
                    tts?.language = Locale.getDefault()
                    val textToSpeak = "Reminder: $title. $desc"
                    tts?.speak(textToSpeak, TextToSpeech.QUEUE_FLUSH, null, "ReminderTTS")
                }
                latch.countDown()
            }
        }
        
        try {
            latch.await()
            Thread.sleep(5000) 
        } catch (e: InterruptedException) {
            e.printStackTrace()
        }
    }
}
