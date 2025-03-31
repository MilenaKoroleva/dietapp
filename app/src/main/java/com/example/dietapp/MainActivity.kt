package com.example.dietapp

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.dietapp.databinding.ActivityMainBinding
import com.google.android.material.tabs.TabLayout
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.io.InputStream

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var adapter: MealAdapter
    private var weeklyMenus = mutableListOf<List<Meal>>()
    private var weeklyMenu = mutableListOf<Meal>()
    private var currentMenuIndex = 0
    private var selectedDay = "Monday"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val prefs = getSharedPreferences("DietPrefs", MODE_PRIVATE)
        if (prefs.getBoolean("isFirstRun", true)) {
            startActivity(Intent(this, SetupActivity::class.java))
            finish()
            return
        }

        setupRecyclerView()
        setupTabLayout()
        setupMenuSwitcher()
        loadMeals()
    }

    private fun setupRecyclerView() {
        binding.mealList.layoutManager = LinearLayoutManager(this)
        adapter = MealAdapter(emptyList()) { meal ->
            showReplaceDialog(meal)
        }
        binding.mealList.adapter = adapter
    }

    private fun setupTabLayout() {
        val days = listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday")
        days.forEach { day -> binding.tabLayout.addTab(binding.tabLayout.newTab().setText(day)) }
        binding.tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                selectedDay = tab?.text.toString()
                updateMeals()
            }
            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })
    }

    private fun setupMenuSwitcher() {
        binding.nextMenuButton.setOnClickListener {
            currentMenuIndex = (currentMenuIndex + 1) % weeklyMenus.size
            weeklyMenu = weeklyMenus[currentMenuIndex].toMutableList()
            showMenuConfirmationDialog()
        }
        binding.prevMenuButton.setOnClickListener {
            // Переход на SetupActivity
            startActivity(Intent(this, SetupActivity::class.java))
            // finish() // Раскомментируй, если хочешь закрыть MainActivity после перехода
        }
    }

    private fun loadMeals() {
        CoroutineScope(Dispatchers.IO).launch {
            val meals = loadMealsFromJson()
            weeklyMenus.clear()
            repeat(3) { // Генерируем 3 варианта меню
                weeklyMenus.add(optimizeMeals(meals))
            }
            currentMenuIndex = 0
            weeklyMenu = weeklyMenus[currentMenuIndex].toMutableList()
            withContext(Dispatchers.Main) {
                showMenuConfirmationDialog()
            }
        }
    }

    private fun loadMealsFromJson(): List<Meal> {
        val inputStream: InputStream = assets.open("recipes.json")
        val jsonString = inputStream.bufferedReader().use { it.readText() }
        val jsonArray = JSONArray(jsonString)
        val meals = mutableListOf<Meal>()
        for (i in 0 until jsonArray.length()) {
            val jsonObject = jsonArray.getJSONObject(i)
            val ingredientsArray = jsonObject.getJSONArray("ingredients")
            val ingredients = mutableListOf<Ingredient>()
            for (j in 0 until ingredientsArray.length()) {
                val ingredient = ingredientsArray.getJSONObject(j)
                ingredients.add(
                    Ingredient(
                        name = ingredient.getString("name"),
                        amount = ingredient.getString("amount"),
                        cost = ingredient.getInt("cost")
                    )
                )
            }
            val imageName = jsonObject.getString("image")
            val imageResId = resources.getIdentifier(imageName, "drawable", packageName)
            meals.add(
                Meal(
                    name = jsonObject.getString("name"),
                    calories = jsonObject.getInt("calories"),
                    imageResId = imageResId,
                    recipe = jsonObject.getString("recipe"),
                    category = jsonObject.getString("category"),
                    day = "",
                    ingredients = ingredients
                )
            )
        }
        return meals
    }

    private fun optimizeMeals(meals: List<Meal>): List<Meal> {
        val prefs = getSharedPreferences("DietPrefs", MODE_PRIVATE)
        val budget = prefs.getInt("budget", 2000)
        val favorites = prefs.getString("favorites", "")?.split(",")?.map { it.trim().lowercase() } ?: emptyList()
        val allergies = prefs.getString("allergies", "")?.split(",")?.map { it.trim().lowercase() } ?: emptyList()
        val disliked = prefs.getString("disliked", "")?.split(",")?.map { it.trim().lowercase() } ?: emptyList()
        val days = listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday")
        val mealTypes = listOf("Breakfast", "Lunch", "Dinner")
        val categories = listOf("Vegetarian", "Keto", "Low Carb", "Vegan")

        val validMeals = meals.filter { meal ->
            meal.ingredients.none { ingredient ->
                allergies.contains(ingredient.name.lowercase()) || disliked.contains(ingredient.name.lowercase())
            }
        }.shuffled()

        val selectedMeals = mutableListOf<Meal>()
        var totalCost = 0

        days.forEach { day ->
            mealTypes.forEachIndexed { index, type ->
                val maxCalories = prefs.getInt("calories_$day", 2000) / 3
                val category = categories[(index + days.indexOf(day)) % categories.size]
                val meal = validMeals
                    .filter { it.category == category && it !in selectedMeals }
                    .sortedBy { meal ->
                        val hasFavorite = meal.ingredients.any { favorites.contains(it.name.lowercase()) }
                        if (hasFavorite) -1 else 1
                    }
                    .firstOrNull { meal ->
                        val mealCost = meal.ingredients.sumOf { ingredient -> ingredient.cost }
                        totalCost + mealCost <= budget && meal.calories <= maxCalories
                    }
                if (meal != null) {
                    selectedMeals.add(meal.copy(day = "$day - $type"))
                    totalCost += meal.ingredients.sumOf { it.cost }
                }
            }
        }

        return selectedMeals
    }

    private fun showMenuConfirmationDialog() {
        AlertDialog.Builder(this)
            .setTitle("Меню на неделю (Вариант ${currentMenuIndex + 1}/${weeklyMenus.size})")
            .setMessage(weeklyMenu.joinToString("\n") { "${it.day}: ${it.name} (${it.category})" })
            .setPositiveButton("Подтвердить") { _, _ -> updateMeals() }
            .setNegativeButton("Изменить") { _, _ -> }
            .show()
    }

    private fun showReplaceDialog(meal: Meal) {
        val alternatives = loadMealsFromJson().filter { it.category == meal.category && it !in weeklyMenu }
        if (alternatives.isNotEmpty()) {
            val newMeal = alternatives.random()
            weeklyMenu[weeklyMenu.indexOf(meal)] = newMeal.copy(day = meal.day)
            showMenuConfirmationDialog()
        }
    }

    private fun updateMeals() {
        val dayMeals = weeklyMenu.filter { it.day.startsWith(selectedDay) }
        adapter.updateMeals(dayMeals)
        val totalCalories = dayMeals.sumOf { it.calories }
        binding.totalCalories.text = "Калорий за день: $totalCalories ккал"
    }
}
