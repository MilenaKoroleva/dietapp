package com.example.dietapp

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface MealDao {
    @Insert
    suspend fun insertAll(meals: List<MealEntity>)

    @Query("SELECT * FROM meals WHERE day = :day AND category = :category")
    suspend fun getMealsForDayAndCategory(day: String, category: String): List<MealEntity>

    @Query("SELECT DISTINCT day FROM meals")
    suspend fun getAllDays(): List<String>
}