package com.example.dietapp

import android.content.SharedPreferences
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.room.Room
import com.example.dietapp.databinding.ActivityMainBinding // Импорт для View Binding
import com.google.android.material.tabs.TabLayout
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.InputStream

class MainActivity : AppCompatActivity() {
    private lateinit var db: AppDatabase
    private lateinit var adapter: MealAdapter
    private lateinit var prefs: SharedPreferences
    private lateinit var binding: ActivityMainBinding // Переменная для binding
    private val dietCategories = listOf("Vegetarian", "Keto", "Low Carb", "Vegan")
    private var selectedDay = "Monday"
    private var selectedCategory = "Vegetarian"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater) // Инициализация binding
        setContentView(binding.root) // Установка корневого view

        prefs = getSharedPreferences("DietPrefs", MODE_PRIVATE)
        db = Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java, "diet-database"
        ).build()

        setupRecyclerView()
        setupTabLayout()
        loadInitialData()

        binding.backButton.setOnClickListener {
            finish() // Возвращаемся назад
        }
    }

    private fun setupRecyclerView() {
        binding.mealList.layoutManager = LinearLayoutManager(this)
        adapter = MealAdapter(emptyList()) { meal ->
            // Можно добавить обработку клика
        }
        binding.mealList.adapter = adapter
    }

    private fun setupTabLayout() {
        val days = listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday")
        days.forEach { day ->
            binding.tabLayout.addTab(binding.tabLayout.newTab().setText(day))
        }

        binding.tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                selectedDay = tab?.text.toString()
                updateMeals()
            }
            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })

        dietCategories.forEach { category ->
            binding.dietCategoryTabs.addTab(binding.dietCategoryTabs.newTab().setText(category))
        }
        binding.dietCategoryTabs.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                selectedCategory = tab?.text.toString()
                updateMeals()
            }
            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })
    }

    private fun loadInitialData() {
        CoroutineScope(Dispatchers.IO).launch {
            val days = db.mealDao().getAllDays()
            if (days.isEmpty()) {
                val meals = loadMealsFromJson()
                val weeklyBudget = prefs.getInt("weeklyBudget", 1000)
                val optimizedMeals = optimizeMealsForBudget(meals, weeklyBudget)
                db.mealDao().insertAll(optimizedMeals)
                generateShoppingList(optimizedMeals)
            }
            updateMeals()
        }
    }

    private fun loadMealsFromJson(): List<MealEntity> {
        return try {
            val inputStream: InputStream = assets.open("recipes.json")
            val jsonString = inputStream.bufferedReader().use { it.readText() }
            println("JSON content: $jsonString") // Лог для проверки
            val jsonArray = JSONArray(jsonString)
            val meals = mutableListOf<MealEntity>()

            for (i in 0 until jsonArray.length()) {
                val jsonObject = jsonArray.getJSONObject(i)
                meals.add(jsonObject(jsonObject))
            }
            println("Loaded ${meals.size} meals") // Лог для проверки
            meals
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    private fun jsonObject(jsonObject: JSONObject): MealEntity {
        val ingredientsArray = jsonObject.getJSONArray("ingredients")
        val ingredientsJson = ingredientsArray.toString()
        return MealEntity(
            name = jsonObject.getString("name"),
            calories = jsonObject.getInt("calories"),
            imageUrl = jsonObject.getString("imageUrl"),
            recipe = jsonObject.getString("recipe"),
            category = jsonObject.getString("category"),
            day = jsonObject.getString("day"),
            ingredients = ingredientsJson
        )
    }

    private fun optimizeMealsForBudget(meals: List<MealEntity>, budget: Int): List<MealEntity> {
        val allergies = prefs.getString("allergies", "")?.split(",")?.map { it.trim().lowercase() } ?: emptyList()
        val affordableMeals = mutableListOf<MealEntity>()
        var totalCost = 0

        val sortedMeals = meals.sortedBy { meal ->
            val ingredients = JSONArray(meal.ingredients)
            var cost = 0
            for (i in 0 until ingredients.length()) {
                cost += ingredients.getJSONObject(i).getInt("cost")
            }
            cost
        }

        for (meal in sortedMeals) {
            val ingredients = JSONArray(meal.ingredients)
            var mealCost = 0
            var hasAllergy = false

            for (i in 0 until ingredients.length()) {
                val ingredient = ingredients.getJSONObject(i)
                val name = ingredient.getString("name").lowercase()
                val cost = when (val costValue = ingredient.get("cost")) {
                    is Int -> costValue
                    is String -> costValue.toIntOrNull() ?: 0
                    else -> 0
                }
                mealCost += cost
                if (allergies.contains(name)) {
                    hasAllergy = true
                    break
                }
            }

            if (!hasAllergy && totalCost + mealCost <= budget && affordableMeals.count { it.day == meal.day } < 1) {
                affordableMeals.add(meal)
                totalCost += mealCost
            }
        }

        val days = listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday")
        days.forEach { day ->
            if (affordableMeals.none { it.day == day }) {
                sortedMeals.firstOrNull { meal ->
                    val ingredients = JSONArray(meal.ingredients)
                    var cost = 0
                    var hasAllergy = false
                    for (i in 0 until ingredients.length()) {
                        val ingredient = ingredients.getJSONObject(i)
                        cost += ingredient.getInt("cost")
                        if (allergies.contains(ingredient.getString("name").lowercase())) {
                            hasAllergy = true
                            break
                        }
                    }
                    !hasAllergy && totalCost + cost <= budget && affordableMeals.none { it.day == day }
                }?.let {
                    affordableMeals.add(it.copy(day = day))
                    totalCost += JSONArray(it.ingredients).sumOf { it.getInt("cost") }
                }
            }
        }

        return affordableMeals
    }

    private fun generateShoppingList(meals: List<MealEntity>) {
        val shoppingList = mutableMapOf<String, Pair<Double, String>>()
        meals.forEach { meal ->
            val ingredients = JSONArray(meal.ingredients)
            for (i in 0 until ingredients.length()) {
                val ingredient = ingredients.getJSONObject(i)
                val name = ingredient.getString("name")
                val amount = ingredient.getString("amount").toDoubleOrNull() ?: 1.0
                val unit = ingredient.getString("amount").replace("[0-9.]".toRegex(), "").trim()
                val cost = ingredient.getInt("cost").toDouble()

                if (shoppingList.containsKey(name)) {
                    val (currentAmount, currentUnit) = shoppingList[name]!!
                    shoppingList[name] = Pair(currentAmount + amount, unit)
                } else {
                    shoppingList[name] = Pair(amount, unit)
                }
            }
        }

        val listText = shoppingList.entries.joinToString("\n") { (name, pair) ->
            // Ищем стоимость среди всех ингредиентов всех блюд
            val costPerUnit = meals.firstOrNull { meal ->
                val ingredients = JSONArray(meal.ingredients)
                (0 until ingredients.length()).any { i ->
                    ingredients.getJSONObject(i).getString("name") == name
                }
            }?.let { meal ->
                val ingredients = JSONArray(meal.ingredients)
                (0 until ingredients.length()).map { i ->
                    ingredients.getJSONObject(i)
                }.first { it.getString("name") == name }.getInt("cost")
            } ?: 0 // Если не нашли, стоимость = 0

            "$name: ${pair.first} ${pair.second} (примерная стоимость: ${pair.first * costPerUnit} руб)"
        }
        prefs.edit().putString("shoppingList", listText).apply()
    }

    private fun updateMeals() {
        CoroutineScope(Dispatchers.IO).launch {
            val meals = db.mealDao().getMealsForDayAndCategory(selectedDay, selectedCategory)
            val totalCalories = meals.sumOf { it.calories }
            val shoppingList = prefs.getString("shoppingList", "Список покупок пуст") ?: "Список покупок пуст"
            val budget = prefs.getInt("weeklyBudget", 1000)
            withContext(Dispatchers.Main) {
                adapter.updateMeals(meals)
                binding.totalCalories.text = "Бюджет на неделю: $budget руб\nВсего калорий: $totalCalories ккал\nСписок покупок:\n$shoppingList"
            }
        }
    }
}

fun JSONArray.sumOf(selector: (JSONObject) -> Int): Int {
    var sum = 0
    for (i in 0 until length()) {
        sum += selector(getJSONObject(i))
    }
    return sum
}
