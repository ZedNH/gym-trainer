package com.gymtrainer.app.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        UserProfileEntity::class,
        ExerciseEntity::class,
        WorkoutPlanEntity::class,
        WorkoutDayEntity::class,
        WorkoutDayExerciseEntity::class,
        WorkoutSessionEntity::class,
        SetLogEntity::class,
        BodyMeasurementEntity::class,
        PersonalRecordEntity::class,
        ChallengeProgressEntity::class,
        UserVideoEntity::class,
        FoodLogEntity::class,
        WaterLogEntity::class
    ],
    version = 6,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun profileDao(): ProfileDao
    abstract fun exerciseDao(): ExerciseDao
    abstract fun planDao(): PlanDao
    abstract fun sessionDao(): SessionDao
    abstract fun measurementDao(): MeasurementDao
    abstract fun recordDao(): RecordDao
    abstract fun challengeDao(): ChallengeDao
    abstract fun videoDao(): VideoDao
    abstract fun nutritionDao(): NutritionDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        /** v2 → v3: personalization fields on the profile. Keeps all history. */
        val MIGRATION_2_3 = object : androidx.room.migration.Migration(2, 3) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE user_profile ADD COLUMN trainingPlace TEXT NOT NULL DEFAULT 'BOTH'")
                db.execSQL("ALTER TABLE user_profile ADD COLUMN focusAreas TEXT NOT NULL DEFAULT ''")
            }
        }

        /** SQL for the v4 tables; must match what Room generates (checked by a unit test). */
        val V4_TABLES = listOf(
            "CREATE TABLE IF NOT EXISTS `user_videos` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `exerciseId` TEXT NOT NULL, `videoId` TEXT NOT NULL, `title` TEXT NOT NULL, `channel` TEXT NOT NULL, `addedAt` INTEGER NOT NULL)",
            "CREATE INDEX IF NOT EXISTS `index_user_videos_exerciseId` ON `user_videos` (`exerciseId`)",
            "CREATE TABLE IF NOT EXISTS `food_logs` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `day` INTEGER NOT NULL, `meal` TEXT NOT NULL, `name` TEXT NOT NULL, `amountLabel` TEXT NOT NULL, `kcal` REAL NOT NULL, `protein` REAL NOT NULL, `carbs` REAL NOT NULL, `fat` REAL NOT NULL, `loggedAt` INTEGER NOT NULL)",
            "CREATE INDEX IF NOT EXISTS `index_food_logs_day` ON `food_logs` (`day`)",
            "CREATE TABLE IF NOT EXISTS `water_logs` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `day` INTEGER NOT NULL, `ml` INTEGER NOT NULL, `loggedAt` INTEGER NOT NULL)",
            "CREATE INDEX IF NOT EXISTS `index_water_logs_day` ON `water_logs` (`day`)"
        )

        /** v3 → v4: videos, food and water logs. Keeps all history. */
        val MIGRATION_3_4 = object : androidx.room.migration.Migration(3, 4) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                V4_TABLES.forEach { db.execSQL(it) }
            }
        }

        /** v4 → v5: equipment variants share photos with their base exercise. */
        val MIGRATION_4_5 = object : androidx.room.migration.Migration(4, 5) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE exercises ADD COLUMN imageOf TEXT NOT NULL DEFAULT ''")
            }
        }

        /** v5 → v6: alternative exercise names for search. */
        val MIGRATION_5_6 = object : androidx.room.migration.Migration(5, 6) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE exercises ADD COLUMN aliases TEXT NOT NULL DEFAULT ''")
            }
        }

        /** Tests only: install a database before the app first asks for one. */
        fun setInstanceForTesting(db: AppDatabase) { INSTANCE = db }

        fun get(context: Context): AppDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "gym_trainer.db"
                ).addMigrations(MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6).fallbackToDestructiveMigration().build().also { INSTANCE = it }
            }
    }
}
