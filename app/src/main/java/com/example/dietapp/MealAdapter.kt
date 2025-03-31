package com.example.dietapp

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.dietapp.databinding.MealItemBinding

class MealAdapter(
    private var meals: List<Meal>,
    private val onItemClick: (Meal) -> Unit // Обработчик клика
) : RecyclerView.Adapter<MealAdapter.MealViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MealViewHolder {
        val binding = MealItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return MealViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MealViewHolder, position: Int) {
        holder.bind(meals[position])
    }

    override fun getItemCount(): Int = meals.size

    fun updateMeals(newMeals: List<Meal>) {
        meals = newMeals
        notifyDataSetChanged()
    }

    inner class MealViewHolder(private val binding: MealItemBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(meal: Meal) {
            binding.mealName.text = meal.name
            binding.mealCalories.text = "${meal.calories} ккал"
            binding.mealCategory.text = meal.category
            Glide.with(binding.root.context)
                .load(meal.imageResId)
                .into(binding.mealImage)
            binding.root.setOnClickListener { onItemClick(meal) } // Вызываем обработчик при клике
        }
    }
}