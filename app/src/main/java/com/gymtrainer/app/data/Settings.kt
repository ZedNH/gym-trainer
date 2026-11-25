package com.gymtrainer.app.data

import android.content.Context

/** Small user preferences (kept out of Room so they're readable synchronously). */
class Settings(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("gym_trainer_settings", Context.MODE_PRIVATE)

    var voiceCoach: Boolean
        get() = prefs.getBoolean("voice", true)
        set(v) = prefs.edit().putBoolean("voice", v).apply()

    var homeRestSeconds: Int
        get() = prefs.getInt("home_rest", 30)
        set(v) = prefs.edit().putInt("home_rest", v).apply()

    var countdownSeconds: Int
        get() = prefs.getInt("countdown", 10)
        set(v) = prefs.edit().putInt("countdown", v).apply()

    var reminderOn: Boolean
        get() = prefs.getBoolean("reminder_on", false)
        set(v) = prefs.edit().putBoolean("reminder_on", v).apply()

    var reminderHour: Int
        get() = prefs.getInt("reminder_hour", 18)
        set(v) = prefs.edit().putInt("reminder_hour", v).apply()

    var reminderMinute: Int
        get() = prefs.getInt("reminder_minute", 0)
        set(v) = prefs.edit().putInt("reminder_minute", v).apply()

    var themeMode: String
        get() = prefs.getString("theme_mode", "LIGHT") ?: "LIGHT"
        set(v) = prefs.edit().putString("theme_mode", v).apply()

    /** Optional, user-provided YouTube Data API key (stored only on this device). */
    var youtubeApiKey: String
        get() = prefs.getString("yt_key", "") ?: ""
        set(v) = prefs.edit().putString("yt_key", v.trim()).apply()

    /** Used only for the calorie estimate: MALE / FEMALE / UNSPECIFIED. */
    var sexForCalories: String
        get() = prefs.getString("sex_kcal", "UNSPECIFIED") ?: "UNSPECIFIED"
        set(v) = prefs.edit().putString("sex_kcal", v).apply()

    /** The first-run "How Gym Trainer works" guide on Today was dismissed. */
    var guideSeen: Boolean
        get() = prefs.getBoolean("guide_seen", false)
        set(v) = prefs.edit().putBoolean("guide_seen", v).apply()

    var weeklyGoal: Int
        get() = prefs.getInt("weekly_goal", 4)
        set(v) = prefs.edit().putInt("weekly_goal", v).apply()
}
