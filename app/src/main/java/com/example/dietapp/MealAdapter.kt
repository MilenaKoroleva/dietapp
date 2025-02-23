package com.example.dietapp

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide


class MealAdapter(
    private var meals: List<MealEntity>,
    private val onClick: (MealEntity) -> Unit
) : RecyclerView.Adapter<MealAdapter.MealViewHolder>() {

    class MealViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val mealImage: ImageView = itemView.findViewById(R.id.meal_image)
        val mealName: TextView = itemView.findViewById(R.id.meal_name)
        val mealCalories: TextView = itemView.findViewById(R.id.meal_calories)
        val mealRecipe: TextView = itemView.findViewById(R.id.meal_recipe)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MealViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.meal_item, parent, false)
        return MealViewHolder(view)
    }

    override fun onBindViewHolder(holder: MealViewHolder, position: Int) {
        val meal = meals[position]
        Glide.with(holder.itemView.context)
            .load(meal.imageUrl)
            .into(holder.mealImage)
        holder.mealName.text = meal.name
        holder.mealCalories.text = "${meal.calories} ккал"
        holder.mealRecipe.text = meal.recipe
        holder.itemView.setOnClickListener { onClick(meal) }
    }

    override fun getItemCount(): Int = meals.size

    fun updateMeals(newMeals: List<MealEntity>) {
        meals = newMeals
        notifyDataSetChanged()
    }
}