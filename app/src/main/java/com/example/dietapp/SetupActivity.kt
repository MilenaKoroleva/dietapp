package com.example.dietapp

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.dietapp.databinding.ActivitySetupBinding

class SetupActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySetupBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySetupBinding.inflate(layoutInflater)
        setContentView(binding.root)

        loadPreferences() // Загружаем текущие настройки
        setupDoneButton()
    }

    private fun loadPreferences() {
        val prefs = getSharedPreferences("DietPrefs", MODE_PRIVATE)
        binding.caloriesInput.setText(prefs.getInt("daily_calories", 2000).toString())
        binding.nutsCheckbox.isChecked = prefs.getBoolean("allergy_nuts", false)
        binding.honeyCheckbox.isChecked = prefs.getBoolean("allergy_honey", false)
        binding.milkCheckbox.isChecked = prefs.getBoolean("allergy_milk", false)
        binding.glutenCheckbox.isChecked = prefs.getBoolean("allergy_gluten", false)
    }

    private fun setupDoneButton() {
        binding.doneButton.setOnClickListener {
            savePreferences()
            android.util.Log.d("SetupActivity", "Done button clicked - Returning to MainActivity")
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish()
        }
    }

    private fun savePreferences() {
        val prefs = getSharedPreferences("DietPrefs", MODE_PRIVATE).edit()
        val calories = binding.caloriesInput.text.toString().toIntOrNull() ?: 2000
        prefs.putInt("daily_calories", calories)
        prefs.putBoolean("allergy_nuts", binding.nutsCheckbox.isChecked)
        prefs.putBoolean("allergy_honey", binding.honeyCheckbox.isChecked)
        prefs.putBoolean("allergy_milk", binding.milkCheckbox.isChecked)
        prefs.putBoolean("allergy_gluten", binding.glutenCheckbox.isChecked)
        prefs.putBoolean("isFirstRun", false) // Отключаем первый запуск
        prefs.apply()
    }
}