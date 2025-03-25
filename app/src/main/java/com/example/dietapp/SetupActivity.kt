package com.example.dietapp

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.dietapp.databinding.ActivitySetupBinding
import com.google.android.material.tabs.TabLayout

class SetupActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySetupBinding
    private val days = listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday")
    private val dayCalories = mutableMapOf<String, Int>() // Храним калории для каждого дня
    private var currentDay = "Monday"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySetupBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val prefs = getSharedPreferences("DietPrefs", MODE_PRIVATE)
        if (!prefs.getBoolean("isFirstRun", true)) {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }

        setupTabLayout()
        loadDefaultCalories()

        binding.saveButton.setOnClickListener {
            saveCurrentDayCalories()
            val speed = when (binding.speedGroup.checkedRadioButtonId) {
                R.id.slow_radio -> "Slow" to 500
                R.id.medium_radio -> "Medium" to 700
                R.id.fast_radio -> "Fast" to 1000
                else -> "Medium" to 700
            }
            val budget = binding.budgetInput.editText?.text.toString().toIntOrNull() ?: 2000
            val favorites = binding.favoriteInput.editText?.text.toString()
            val allergies = binding.allergyInput.editText?.text.toString()
            val disliked = binding.dislikedInput.editText?.text.toString()

            prefs.edit().apply {
                putString("speed", speed.first)
                putInt("deficit", speed.second)
                putInt("budget", budget)
                putString("favorites", favorites)
                putString("allergies", allergies)
                putString("disliked", disliked)
                days.forEach { day ->
                    putInt("calories_$day", dayCalories[day] ?: 2000) // Сохраняем калории для каждого дня
                }
                putBoolean("isFirstRun", false)
            }.apply()

            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }
    }

    private fun setupTabLayout() {
        days.forEach { day -> binding.dayTabs.addTab(binding.dayTabs.newTab().setText(day)) }
        binding.dayTabs.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                saveCurrentDayCalories()
                currentDay = tab?.text.toString()
                loadDayCalories()
            }
            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })
    }

    private fun loadDefaultCalories() {
        days.forEach { day ->
            dayCalories[day] = 2000 // Значение по умолчанию
        }
        loadDayCalories()
    }

    private fun loadDayCalories() {
        binding.dayCaloriesInput.editText?.setText(dayCalories[currentDay]?.toString() ?: "2000")
    }

    private fun saveCurrentDayCalories() {
        val calories = binding.dayCaloriesInput.editText?.text.toString().toIntOrNull() ?: 2000
        dayCalories[currentDay] = calories
    }
}
