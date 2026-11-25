package com.gymtrainer.app.data

import com.gymtrainer.app.domain.Goal
import kotlinx.coroutines.flow.Flow
import java.util.Calendar
import kotlin.math.roundToInt

/** Food with nutrients per 100 g and one household serving. Values are typical averages (approximate). */
data class Food(
    val name: String,
    val category: String,
    val kcal: Double, val protein: Double, val carbs: Double, val fat: Double,
    val servingLabel: String, val servingGrams: Double
)

enum class Meal(val label: String) { BREAKFAST("Breakfast"), LUNCH("Lunch"), DINNER("Dinner"), SNACK("Snacks") }

data class NutritionTargets(val kcal: Int, val protein: Int, val carbs: Int, val fat: Int, val waterMl: Int)

object FoodDb {
    private fun f(name: String, cat: String, kcal: Double, p: Double, c: Double, fat: Double, serving: String, g: Double) =
        Food(name, cat, kcal, p, c, fat, serving, g)

    val foods: List<Food> = listOf(
        // Grains & staples
        f("Rice, white (cooked)", "Grains", 130.0, 2.7, 28.2, 0.3, "1 cup", 158.0),
        f("Rice, brown (cooked)", "Grains", 112.0, 2.3, 23.5, 0.8, "1 cup", 195.0),
        f("Roti / chapati (whole wheat)", "Grains", 297.0, 9.6, 49.0, 7.5, "1 roti", 40.0),
        f("Paratha", "Grains", 326.0, 6.4, 45.0, 13.0, "1 paratha", 80.0),
        f("Bread, white", "Grains", 265.0, 9.0, 49.0, 3.2, "1 slice", 25.0),
        f("Bread, whole wheat", "Grains", 247.0, 13.0, 41.0, 3.4, "1 slice", 32.0),
        f("Oats (dry)", "Grains", 389.0, 16.9, 66.3, 6.9, "½ cup", 40.0),
        f("Pasta (cooked)", "Grains", 158.0, 5.8, 31.0, 0.9, "1 cup", 140.0),
        f("Flattened rice / chira (dry)", "Grains", 346.0, 6.6, 77.0, 1.2, "1 cup", 50.0),
        f("Muri / puffed rice", "Grains", 402.0, 6.3, 89.8, 0.5, "1 cup", 15.0),
        f("Potato (boiled)", "Grains", 87.0, 1.9, 20.1, 0.1, "1 medium", 150.0),
        f("Sweet potato (boiled)", "Grains", 76.0, 1.4, 17.7, 0.1, "1 medium", 150.0),
        // Protein
        f("Egg, whole (boiled)", "Protein", 155.0, 12.6, 1.1, 10.6, "1 egg", 50.0),
        f("Egg white", "Protein", 52.0, 10.9, 0.7, 0.2, "1 white", 33.0),
        f("Chicken breast (cooked)", "Protein", 165.0, 31.0, 0.0, 3.6, "1 palm", 100.0),
        f("Chicken curry", "Protein", 150.0, 14.0, 4.0, 9.0, "1 bowl", 200.0),
        f("Beef, lean (cooked)", "Protein", 217.0, 26.0, 0.0, 12.0, "1 palm", 100.0),
        f("Beef curry", "Protein", 190.0, 16.0, 4.0, 12.0, "1 bowl", 200.0),
        f("Mutton (cooked)", "Protein", 258.0, 25.6, 0.0, 16.5, "1 palm", 100.0),
        f("Fish, rohu / carp (cooked)", "Protein", 130.0, 20.0, 0.0, 5.0, "1 piece", 100.0),
        f("Hilsa / ilish (cooked)", "Protein", 273.0, 22.0, 0.0, 19.5, "1 piece", 100.0),
        f("Salmon (cooked)", "Protein", 206.0, 22.0, 0.0, 12.0, "1 fillet", 125.0),
        f("Tuna (canned in water)", "Protein", 116.0, 25.5, 0.0, 0.8, "1 can", 120.0),
        f("Shrimp / prawn (cooked)", "Protein", 99.0, 24.0, 0.2, 0.3, "1 serving", 100.0),
        f("Lentils / dal (cooked)", "Protein", 116.0, 9.0, 20.1, 0.4, "1 bowl", 200.0),
        f("Chickpeas (cooked)", "Protein", 164.0, 8.9, 27.4, 2.6, "1 cup", 160.0),
        f("Kidney beans (cooked)", "Protein", 127.0, 8.7, 22.8, 0.5, "1 cup", 177.0),
        f("Tofu (firm)", "Protein", 144.0, 17.3, 2.8, 8.7, "½ block", 120.0),
        f("Paneer", "Protein", 265.0, 18.3, 1.2, 20.8, "1 serving", 80.0),
        f("Whey protein powder", "Protein", 400.0, 80.0, 8.0, 6.0, "1 scoop", 30.0),
        // Dairy
        f("Milk, whole", "Dairy", 61.0, 3.2, 4.8, 3.3, "1 glass", 250.0),
        f("Milk, skimmed", "Dairy", 34.0, 3.4, 5.0, 0.1, "1 glass", 250.0),
        f("Yogurt / doi, plain", "Dairy", 61.0, 3.5, 4.7, 3.3, "1 cup", 245.0),
        f("Greek yogurt, plain", "Dairy", 97.0, 9.0, 3.9, 5.0, "1 cup", 200.0),
        f("Cheese, cheddar", "Dairy", 403.0, 25.0, 1.3, 33.0, "1 slice", 20.0),
        f("Butter", "Dairy", 717.0, 0.9, 0.1, 81.0, "1 tbsp", 14.0),
        f("Ghee", "Dairy", 900.0, 0.0, 0.0, 100.0, "1 tsp", 5.0),
        // Fruit
        f("Banana", "Fruit", 89.0, 1.1, 22.8, 0.3, "1 medium", 118.0),
        f("Apple", "Fruit", 52.0, 0.3, 13.8, 0.2, "1 medium", 182.0),
        f("Orange", "Fruit", 47.0, 0.9, 11.8, 0.1, "1 medium", 131.0),
        f("Mango", "Fruit", 60.0, 0.8, 15.0, 0.4, "1 cup", 165.0),
        f("Papaya", "Fruit", 43.0, 0.5, 10.8, 0.3, "1 cup", 145.0),
        f("Guava", "Fruit", 68.0, 2.6, 14.3, 1.0, "1 fruit", 55.0),
        f("Watermelon", "Fruit", 30.0, 0.6, 7.6, 0.2, "1 cup", 152.0),
        f("Dates", "Fruit", 282.0, 2.5, 75.0, 0.4, "3 dates", 24.0),
        f("Grapes", "Fruit", 69.0, 0.7, 18.1, 0.2, "1 cup", 151.0),
        // Vegetables
        f("Mixed vegetables (cooked)", "Vegetables", 65.0, 2.6, 13.0, 0.3, "1 cup", 180.0),
        f("Spinach / shak (cooked)", "Vegetables", 23.0, 3.0, 3.8, 0.3, "1 cup", 180.0),
        f("Broccoli (cooked)", "Vegetables", 35.0, 2.4, 7.2, 0.4, "1 cup", 156.0),
        f("Cucumber", "Vegetables", 15.0, 0.7, 3.6, 0.1, "1 cup", 120.0),
        f("Tomato", "Vegetables", 18.0, 0.9, 3.9, 0.2, "1 medium", 123.0),
        f("Carrot", "Vegetables", 41.0, 0.9, 9.6, 0.2, "1 medium", 61.0),
        f("Salad (green, no dressing)", "Vegetables", 17.0, 1.2, 3.3, 0.2, "1 bowl", 100.0),
        f("Vegetable curry (with oil)", "Vegetables", 110.0, 2.5, 10.0, 7.0, "1 bowl", 200.0),
        // Nuts, fats, snacks
        f("Peanuts", "Snacks", 567.0, 25.8, 16.1, 49.2, "1 handful", 28.0),
        f("Almonds", "Snacks", 579.0, 21.2, 21.6, 49.9, "1 handful", 28.0),
        f("Peanut butter", "Snacks", 588.0, 25.0, 20.0, 50.0, "1 tbsp", 16.0),
        f("Cooking oil", "Snacks", 884.0, 0.0, 0.0, 100.0, "1 tbsp", 14.0),
        f("Biscuits / cookies", "Snacks", 480.0, 6.0, 68.0, 20.0, "2 biscuits", 20.0),
        f("Samosa", "Snacks", 262.0, 3.5, 24.0, 17.0, "1 samosa", 100.0),
        f("Singara", "Snacks", 280.0, 4.0, 30.0, 16.0, "1 singara", 70.0),
        f("Potato chips", "Snacks", 536.0, 7.0, 53.0, 35.0, "1 small pack", 28.0),
        f("Dark chocolate (70%)", "Snacks", 598.0, 7.8, 46.0, 43.0, "2 squares", 20.0),
        f("Popcorn (air-popped)", "Snacks", 387.0, 13.0, 78.0, 4.5, "3 cups", 24.0),
        // Drinks
        f("Tea with milk & sugar", "Drinks", 45.0, 1.0, 8.0, 1.0, "1 cup", 150.0),
        f("Coffee, black", "Drinks", 2.0, 0.3, 0.0, 0.0, "1 cup", 240.0),
        f("Orange juice", "Drinks", 45.0, 0.7, 10.4, 0.2, "1 glass", 250.0),
        f("Soft drink / cola", "Drinks", 42.0, 0.0, 10.6, 0.0, "1 can", 330.0),
        f("Lassi (sweet)", "Drinks", 98.0, 3.0, 15.0, 3.0, "1 glass", 250.0),
        // Meals
        f("Khichuri", "Meals", 150.0, 5.0, 22.0, 4.5, "1 plate", 250.0),
        f("Chicken biryani", "Meals", 190.0, 9.0, 23.0, 7.0, "1 plate", 300.0),
        f("Fried rice", "Meals", 174.0, 4.0, 26.0, 6.0, "1 plate", 250.0),
        f("Noodles (instant, cooked)", "Meals", 138.0, 3.0, 19.0, 5.5, "1 pack", 250.0),
        f("Pizza slice", "Meals", 266.0, 11.0, 33.0, 10.0, "1 slice", 107.0),
        f("Burger (beef)", "Meals", 254.0, 13.0, 28.0, 10.0, "1 burger", 150.0)
    )

    fun search(q: String) = if (q.isBlank()) foods else foods.filter { it.name.contains(q, true) || it.category.contains(q, true) }
}

object NutritionCalc {
    /** Mifflin-St Jeor BMR × activity, adjusted for goal. An estimate, not a prescription. */
    fun targets(p: UserProfileEntity, sex: String): NutritionTargets {
        val base = 10 * p.weightKg + 6.25 * p.heightCm - 5 * p.age
        val bmr = when (sex) { "MALE" -> base + 5; "FEMALE" -> base - 161; else -> base - 78 }
        val activity = when {
            p.daysPerWeek <= 2 -> 1.375
            p.daysPerWeek <= 4 -> 1.465
            p.daysPerWeek == 5 -> 1.55
            else -> 1.64
        }
        val tdee = bmr * activity
        val goal = Goal.from(p.goal)
        val kcal = tdee * when (goal) {
            Goal.FAT_LOSS -> 0.8
            Goal.RECOMPOSITION -> 0.92
            Goal.MUSCLE_GAIN, Goal.STRENGTH -> 1.1
            Goal.LEAN_MUSCLE -> 1.05
            Goal.GENERAL_FITNESS -> 1.0
        }
        val proteinPerKg = when (goal) { Goal.FAT_LOSS, Goal.RECOMPOSITION -> 2.0; Goal.GENERAL_FITNESS -> 1.4; else -> 1.8 }
        val protein = p.weightKg * proteinPerKg
        val fat = kcal * 0.25 / 9
        val carbs = ((kcal - protein * 4 - fat * 9) / 4).coerceAtLeast(50.0)
        return NutritionTargets(
            kcal = (kcal / 10).roundToInt() * 10,
            protein = protein.roundToInt(), carbs = carbs.roundToInt(), fat = fat.roundToInt(),
            waterMl = ((p.weightKg * 35) / 250).roundToInt() * 250
        )
    }
}

class NutritionRepository(private val dao: NutritionDao) {
    fun observeFoods(day: Long): Flow<List<FoodLogEntity>> = dao.observeFoods(day)
    fun observeWater(day: Long): Flow<Int> = dao.observeWater(day)
    fun observeLoggedDays(): Flow<Int> = dao.observeLoggedDays()

    suspend fun logFood(day: Long, meal: Meal, food: Food, grams: Double, label: String) {
        val k = grams / 100.0
        dao.insertFood(
            FoodLogEntity(day = day, meal = meal.name, name = food.name, amountLabel = label,
                kcal = food.kcal * k, protein = food.protein * k, carbs = food.carbs * k, fat = food.fat * k,
                loggedAt = System.currentTimeMillis())
        )
    }

    suspend fun logCustom(day: Long, meal: Meal, name: String, kcal: Double, protein: Double, carbs: Double, fat: Double) =
        dao.insertFood(FoodLogEntity(day = day, meal = meal.name, name = name, amountLabel = "1 serving",
            kcal = kcal, protein = protein, carbs = carbs, fat = fat, loggedAt = System.currentTimeMillis()))

    suspend fun delete(id: Long) = dao.deleteFood(id)
    suspend fun addWater(day: Long, ml: Int) = dao.insertWater(WaterLogEntity(day = day, ml = ml, loggedAt = System.currentTimeMillis()))
    suspend fun undoWater(day: Long) = dao.undoWater(day)

    companion object {
        fun today(): Long = dayOf(System.currentTimeMillis())
        fun dayOf(ms: Long): Long {
            val cal = Calendar.getInstance().apply { timeInMillis = ms }
            return (ms + cal.get(Calendar.ZONE_OFFSET) + cal.get(Calendar.DST_OFFSET)) / 86_400_000L
        }
    }
}
