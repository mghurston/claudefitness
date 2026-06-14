package com.mhurston.ascendant.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkoutDao {

    @Query("SELECT * FROM workout_day ORDER BY date ASC")
    fun observeAll(): Flow<List<WorkoutDayEntity>>

    @Query("SELECT * FROM workout_day WHERE date = :date LIMIT 1")
    suspend fun getDay(date: String): WorkoutDayEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(day: WorkoutDayEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(days: List<WorkoutDayEntity>)

    @Query("SELECT COUNT(*) FROM workout_day")
    suspend fun count(): Int
}
