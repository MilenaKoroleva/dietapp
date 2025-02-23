package com.example.dietapp

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "meals")
data class MealEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val calories: Int,
    val imageUrl: String,
    val recipe: String,
    val category: String,
    val day: String,
    val ingredients: String // Храним как JSON-строку
)