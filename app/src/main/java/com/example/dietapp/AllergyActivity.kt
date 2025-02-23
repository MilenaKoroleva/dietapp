package com.example.dietapp

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.dietapp.databinding.ActivityAllergyBinding // Импорт для View Binding

class AllergyActivity : AppCompatActivity() {
    private lateinit var prefs: android.content.SharedPreferences
    private lateinit var binding: ActivityAllergyBinding // Переменная для binding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAllergyBinding.inflate(layoutInflater) // Инициализация binding
        setContentView(binding.root) // Установка корневого view

        prefs = getSharedPreferences("DietPrefs", MODE_PRIVATE)

        if (prefs.getBoolean("isFirstRun", true)) {
            binding.saveButton.setOnClickListener { // Используем binding вместо синтетики
                val allergies = binding.allergyInput.text.toString()
                val budget = binding.budgetInput.text.toString().toIntOrNull() ?: 1000 // По умолчанию 1000 руб
                prefs.edit()
                    .putString("allergies", allergies)
                    .putInt("weeklyBudget", budget)
                    .putBoolean("isFirstRun", false)
                    .apply()
                startActivity(Intent(this, MainActivity::class.java))
                finish()
            }
        } else {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }
    }
}