package com.example.dietapp

data class Meal(
    val name: String,
    val calories: Int,
    val imageResId: Int,
    val recipe: String,
    val category: String,
    val day: String,
    val ingredients: List<Ingredient>
) {
    // Добавим пустой конструктор для совместимости, если нужно
    constructor() : this("", 0, 0, "", "", "", emptyList())
}

data class Ingredient(
    val name: String,
    val amount: String,
    val cost: Int
)