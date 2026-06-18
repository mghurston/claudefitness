package com.mhurston.ascendant.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [WorkoutDayEntity::class], version = 8, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun workoutDao(): WorkoutDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null

        // v1 → v2: add the per-day "mood" journal column (0 = unset). Preserves all logged data.
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE workout_day ADD COLUMN mood INTEGER NOT NULL DEFAULT 0")
            }
        }

        // v2 → v3: add the per-day supplementary "customReps" column ("" = none).
        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE workout_day ADD COLUMN customReps TEXT NOT NULL DEFAULT ''")
            }
        }

        // v3 → v4: add the per-day "pushVariants" column for Push-ups alternatives ("" = none).
        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE workout_day ADD COLUMN pushVariants TEXT NOT NULL DEFAULT ''")
            }
        }

        // v4 → v5: add the per-day "oneOffs" column for ad-hoc one-off activities ("" = none).
        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE workout_day ADD COLUMN oneOffs TEXT NOT NULL DEFAULT ''")
            }
        }

        // v5 → v6: add the per-day "coreVariants" column for Core alternatives ("" = none).
        private val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE workout_day ADD COLUMN coreVariants TEXT NOT NULL DEFAULT ''")
            }
        }

        // v6 → v7: add the per-day "cardioMinutes" column for time-based extra cardio ("" = none).
        private val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE workout_day ADD COLUMN cardioMinutes TEXT NOT NULL DEFAULT ''")
            }
        }

        // v7 → v8: add passive-activity columns (Health Connect steps + active calories).
        private val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE workout_day ADD COLUMN passiveSteps INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE workout_day ADD COLUMN passiveKcal INTEGER NOT NULL DEFAULT 0")
            }
        }

        fun get(context: Context): AppDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "ascendant.db"
                ).addMigrations(
                    MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6,
                    MIGRATION_6_7, MIGRATION_7_8
                ).build().also { INSTANCE = it }
            }
    }
}
