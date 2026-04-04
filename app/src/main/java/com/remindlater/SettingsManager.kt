package com.remindlater

import android.content.Context
import android.content.SharedPreferences

class SettingsManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("settings", Context.MODE_PRIVATE)

    var themeMode: String
        get() = prefs.getString("theme_mode", "auto") ?: "auto"
        set(value) = prefs.edit().putString("theme_mode", value).apply()

    var useDynamicColor: Boolean
        get() = prefs.getBoolean("use_dynamic_color", true)
        set(value) = prefs.edit().putBoolean("use_dynamic_color", value).apply()

    var language: String
        get() = prefs.getString("language", "auto") ?: "auto"
        set(value) = prefs.edit().putString("language", value).apply()

    var customAlarmUri: String?
        get() = prefs.getString("custom_alarm_uri", null)
        set(value) = prefs.edit().putString("custom_alarm_uri", value).apply()

    var useDefaultAlarm: Boolean
        get() = prefs.getBoolean("use_default_alarm", true)
        set(value) = prefs.edit().putBoolean("use_default_alarm", value).apply()
}
