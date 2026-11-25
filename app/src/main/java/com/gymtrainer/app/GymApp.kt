package com.gymtrainer.app

import android.app.Application
import com.gymtrainer.app.data.AppDatabase
import com.gymtrainer.app.data.GymRepository
import com.gymtrainer.app.data.NutritionRepository
import com.gymtrainer.app.data.VideoRepository

/** Application entry point holding the repositories (manual DI). */
class GymApp : Application() {
    /** Created on first use so tests can install a database first. */
    val repository: GymRepository by lazy { GymRepository(AppDatabase.get(this), assets, com.gymtrainer.app.data.PrefsTrainingStore(this)) }
    val videos: VideoRepository by lazy { VideoRepository(assets, AppDatabase.get(this).videoDao()) }
    val nutrition: NutritionRepository by lazy { NutritionRepository(AppDatabase.get(this).nutritionDao()) }

    override fun onCreate() {
        super.onCreate()
        com.gymtrainer.app.reminders.Reminders.createChannel(this)
    }
}
