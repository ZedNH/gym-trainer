package com.gymtrainer.app.ui.nutrition

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LocalDrink
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.gymtrainer.app.data.Food
import com.gymtrainer.app.data.FoodDb
import com.gymtrainer.app.data.FoodLogEntity
import com.gymtrainer.app.data.Meal
import com.gymtrainer.app.data.NutritionCalc
import com.gymtrainer.app.data.NutritionRepository
import com.gymtrainer.app.data.UserProfileEntity
import com.gymtrainer.app.ui.Format
import com.gymtrainer.app.ui.components.AppCard
import com.gymtrainer.app.ui.components.AppTextField
import com.gymtrainer.app.ui.components.CircleIconButton
import com.gymtrainer.app.ui.components.DetailTopBar
import com.gymtrainer.app.ui.components.FilterPill
import com.gymtrainer.app.ui.components.Gutter
import com.gymtrainer.app.ui.components.IconBadge
import com.gymtrainer.app.ui.components.LinearMeter
import com.gymtrainer.app.ui.components.PillButton
import com.gymtrainer.app.ui.components.PrimaryButton
import com.gymtrainer.app.ui.components.ProgressRing
import com.gymtrainer.app.ui.components.SegmentedTabs
import com.gymtrainer.app.ui.components.TabHeader
import com.gymtrainer.app.ui.rememberNutrition
import com.gymtrainer.app.ui.rememberRepository
import com.gymtrainer.app.ui.rememberSettings
import com.gymtrainer.app.ui.theme.AppTheme
import com.gymtrainer.app.ui.theme.Brand
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

private fun dayLabel(day: Long): String {
    val today = NutritionRepository.today()
    return when (day) {
        today -> "Today"
        today - 1 -> "Yesterday"
        today + 1 -> "Tomorrow"
        else -> SimpleDateFormat("EEE, MMM d", Locale.getDefault()).format(Date(day * 86_400_000L))
    }
}

@Composable
fun NutritionScreen(contentPadding: PaddingValues, onAddFood: (Meal, Long) -> Unit) {
    val repo = rememberRepository()
    val nutrition = rememberNutrition()
    val settings = rememberSettings()
    val scope = rememberCoroutineScope()
    var day by rememberSaveable { mutableLongStateOf(NutritionRepository.today()) }
    var sex by remember { mutableStateOf(settings.sexForCalories) }
    var showEstimate by remember { mutableStateOf(false) }
    val profile by produceState<UserProfileEntity?>(null) { value = repo.getProfile() }
    val foods by nutrition.observeFoods(day).collectAsState(initial = emptyList())
    val water by nutrition.observeWater(day).collectAsState(initial = 0)
    val targets = profile?.let { NutritionCalc.targets(it, sex) }

    val eaten = foods.sumOf { it.kcal }
    val protein = foods.sumOf { it.protein }
    val carbs = foods.sumOf { it.carbs }
    val fat = foods.sumOf { it.fat }

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = contentPadding.calculateBottomPadding() + 28.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            TabHeader("Nutrition") {
                CircleIconButton(Icons.AutoMirrored.Filled.ArrowBack, "Previous day", { day -= 1 })
                Text(dayLabel(day), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.padding(horizontal = 10.dp))
                CircleIconButton(Icons.AutoMirrored.Filled.ArrowForward, "Next day", { if (day < NutritionRepository.today()) day += 1 })
            }
        }

        // Calories
        item {
            AppCard(Modifier.padding(horizontal = Gutter)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val goal = targets?.kcal ?: 2000
                    val left = goal - eaten.roundToInt()
                    ProgressRing((eaten / goal).toFloat(), size = 128.dp, stroke = 12.dp,
                        color = if (left < 0) AppTheme.colors.danger else MaterialTheme.colorScheme.primary) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("${kotlin.math.abs(left)}", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.onSurface)
                            Text(if (left >= 0) "kcal left" else "kcal over", style = MaterialTheme.typography.bodySmall, color = AppTheme.colors.textMuted)
                        }
                    }
                    Column(Modifier.padding(start = 18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        KcalLine("Goal", "$goal")
                        KcalLine("Eaten", "${eaten.roundToInt()}")
                        Text("Adjust estimate", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.clip(RoundedCornerShape(8.dp)).clickable { showEstimate = true }.padding(vertical = 4.dp))
                    }
                }
                Spacer(Modifier.height(16.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    Macro("Protein", protein, targets?.protein ?: 120, Brand.Blue, Modifier.weight(1f))
                    Macro("Carbs", carbs, targets?.carbs ?: 220, Brand.Amber, Modifier.weight(1f))
                    Macro("Fat", fat, targets?.fat ?: 60, Brand.Pink, Modifier.weight(1f))
                }
            }
        }

        // Water
        item {
            val goalMl = targets?.waterMl ?: 2500
            AppCard(Modifier.padding(horizontal = Gutter)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconBadge(Icons.Filled.WaterDrop, Brand.Teal, 44.dp)
                    Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                        Text("Water", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                        Text("${Format.litres(water)} / ${Format.litres(goalMl)} L", style = MaterialTheme.typography.bodySmall, color = AppTheme.colors.textMuted)
                    }
                    CircleIconButton(Icons.Filled.Remove, "Undo water", { scope.launch { nutrition.undoWater(day) } })
                    Spacer(Modifier.width(8.dp))
                    PillButton("+250 ml", { scope.launch { nutrition.addWater(day, 250) } }, color = Brand.Teal)
                }
                Spacer(Modifier.height(12.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    val glasses = (goalMl / 250).coerceIn(4, 12)
                    repeat(glasses) { i ->
                        Icon(Icons.Filled.LocalDrink, contentDescription = null, modifier = Modifier.size(24.dp),
                            tint = if ((i + 1) * 250 <= water) Brand.Teal else AppTheme.colors.border)
                    }
                }
            }
        }

        // Meals
        Meal.entries.forEach { meal ->
            item(key = meal.name) {
                val items = foods.filter { it.meal == meal.name }
                AppCard(Modifier.padding(horizontal = Gutter)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(meal.label, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                            Text(if (items.isEmpty()) "Nothing logged" else "${items.sumOf { it.kcal }.roundToInt()} kcal",
                                style = MaterialTheme.typography.bodySmall, color = AppTheme.colors.textMuted)
                        }
                        PillButton("Add", { onAddFood(meal, day) }, icon = Icons.Filled.Add)
                    }
                    items.forEach { f -> FoodRow(f) { scope.launch { nutrition.delete(f.id) } } }
                }
            }
        }
        item {
            Text("Targets are estimates from your profile (Mifflin-St Jeor). They aren't medical advice — adjust with a professional if needed.",
                style = MaterialTheme.typography.bodySmall, color = AppTheme.colors.textFaint, modifier = Modifier.padding(horizontal = Gutter))
        }
    }

    if (showEstimate) {
        AlertDialog(
            onDismissRequest = { showEstimate = false },
            containerColor = AppTheme.colors.card,
            title = { Text("Calorie estimate") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Calculate my target using:", style = MaterialTheme.typography.bodyMedium)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("MALE" to "Male", "FEMALE" to "Female", "UNSPECIFIED" to "Average").forEach { (v, l) ->
                            FilterPill(l, sex == v) { sex = v; settings.sexForCalories = v }
                        }
                    }
                    targets?.let {
                        Text("${it.kcal} kcal · ${it.protein} g protein · ${it.carbs} g carbs · ${it.fat} g fat",
                            style = MaterialTheme.typography.bodySmall, color = AppTheme.colors.textMuted)
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showEstimate = false }) { Text("Done") } }
        )
    }
}

@Composable
private fun KcalLine(label: String, value: String) {
    Column {
        Text(label, style = MaterialTheme.typography.bodySmall, color = AppTheme.colors.textMuted)
        Text(value, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
private fun Macro(label: String, value: Double, target: Int, color: Color, modifier: Modifier) {
    Column(modifier) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = AppTheme.colors.textMuted)
        Text("${value.roundToInt()} / $target g", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)
        Spacer(Modifier.height(6.dp))
        LinearMeter((value / target).toFloat(), color = color, height = 6.dp)
    }
}

@Composable
private fun FoodRow(f: FoodLogEntity, onDelete: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(top = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(f.name, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text("${f.amountLabel} · P ${f.protein.roundToInt()} · C ${f.carbs.roundToInt()} · F ${f.fat.roundToInt()}",
                style = MaterialTheme.typography.bodySmall, color = AppTheme.colors.textMuted, maxLines = 1)
        }
        Text("${f.kcal.roundToInt()}", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)
        IconButton(onClick = onDelete) { Icon(Icons.Filled.Close, contentDescription = "Remove ${f.name}", tint = AppTheme.colors.textFaint) }
    }
}

// ---------------- Add food ----------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddFoodScreen(mealName: String, day: Long, onDone: () -> Unit) {
    val meal = remember(mealName) { runCatching { Meal.valueOf(mealName) }.getOrDefault(Meal.SNACK) }
    val nutrition = rememberNutrition()
    val scope = rememberCoroutineScope()
    var query by remember { mutableStateOf("") }
    var category by remember { mutableStateOf<String?>(null) }
    var picked by remember { mutableStateOf<Food?>(null) }
    var showCustom by remember { mutableStateOf(false) }
    val categories = remember { FoodDb.foods.map { it.category }.distinct() }
    val results = FoodDb.search(query).filter { category == null || it.category == category }

    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).imePadding()) {
        DetailTopBar("Add to ${meal.label}", onDone) {
            CircleIconButton(Icons.Filled.Edit, "Custom food", { showCustom = true })
        }
        AppTextField(query, { query = it }, "Search foods", Modifier.padding(horizontal = Gutter),
            leading = { Icon(Icons.Filled.Search, contentDescription = null, tint = AppTheme.colors.textMuted) })
        Spacer(Modifier.height(10.dp))
        LazyRow(contentPadding = PaddingValues(horizontal = Gutter), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            item { FilterPill("All", category == null) { category = null } }
            items(categories) { c -> FilterPill(c, category == c) { category = if (category == c) null else c } }
        }
        LazyColumn(contentPadding = PaddingValues(Gutter), verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.weight(1f)) {
            items(results, key = { it.name }) { food ->
                AppCard(onClick = { picked = food }, padding = PaddingValues(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconBadge(Icons.Filled.Restaurant, Brand.Orange, 40.dp)
                        Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                            Text(food.name, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)
                            Text("${food.servingLabel} (${food.servingGrams.roundToInt()} g) · ${(food.kcal * food.servingGrams / 100).roundToInt()} kcal",
                                style = MaterialTheme.typography.bodySmall, color = AppTheme.colors.textMuted)
                        }
                        Icon(Icons.Filled.Add, contentDescription = "Add ${food.name}", tint = MaterialTheme.colorScheme.primary)
                    }
                }
            }
            item {
                TextButton(onClick = { showCustom = true }, modifier = Modifier.fillMaxWidth()) { Text("Can't find it? Add a custom food") }
            }
        }
    }

    picked?.let { food ->
        var byGrams by remember(food) { mutableStateOf(false) }
        var amount by remember(food) { mutableDoubleStateOf(1.0) }
        val grams = if (byGrams) amount else amount * food.servingGrams
        ModalBottomSheet(onDismissRequest = { picked = null }, containerColor = AppTheme.colors.card) {
            Column(Modifier.padding(horizontal = Gutter).navigationBarsPadding()) {
                Text(food.name, style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onSurface)
                Spacer(Modifier.height(12.dp))
                SegmentedTabs(listOf(false to "Servings", true to "Grams"), byGrams, {
                    byGrams = it; amount = if (it) food.servingGrams else 1.0
                })
                Spacer(Modifier.height(16.dp))
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                    CircleIconButton(Icons.Filled.Remove, "Less", { amount = (amount - if (byGrams) 10.0 else 0.5).coerceAtLeast(if (byGrams) 10.0 else 0.5) },
                        background = AppTheme.colors.cardAlt)
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(Format.trim(amount), style = MaterialTheme.typography.displaySmall, color = MaterialTheme.colorScheme.onSurface)
                        Text(if (byGrams) "grams" else "× ${food.servingLabel}", style = MaterialTheme.typography.bodySmall, color = AppTheme.colors.textMuted)
                    }
                    CircleIconButton(Icons.Filled.Add, "More", { amount += if (byGrams) 10.0 else 0.5 }, background = AppTheme.colors.cardAlt)
                }
                Spacer(Modifier.height(16.dp))
                val k = grams / 100
                Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(AppTheme.colors.cardAlt).padding(14.dp)) {
                    Text("${(food.kcal * k).roundToInt()} kcal · P ${(food.protein * k).roundToInt()} g · C ${(food.carbs * k).roundToInt()} g · F ${(food.fat * k).roundToInt()} g",
                        style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)
                }
                Spacer(Modifier.height(16.dp))
                PrimaryButton("Save to ${meal.label}", {
                    val label = if (byGrams) "${grams.roundToInt()} g" else "${Format.trim(amount)} × ${food.servingLabel}"
                    scope.launch { nutrition.logFood(day, meal, food, grams, label); picked = null; onDone() }
                }, Modifier.fillMaxWidth())
                Spacer(Modifier.height(16.dp))
            }
        }
    }

    if (showCustom) {
        var name by remember { mutableStateOf("") }
        var kcal by remember { mutableStateOf("") }
        var p by remember { mutableStateOf("") }
        var c by remember { mutableStateOf("") }
        var f by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showCustom = false },
            containerColor = AppTheme.colors.card,
            title = { Text("Custom food") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    AppTextField(name, { name = it }, "Name")
                    AppTextField(kcal, { kcal = it.filter { ch -> ch.isDigit() || ch == '.' } }, "Calories (kcal)", keyboard = KeyboardType.Decimal)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        AppTextField(p, { p = it.filter { ch -> ch.isDigit() || ch == '.' } }, "Protein", Modifier.weight(1f), keyboard = KeyboardType.Decimal)
                        AppTextField(c, { c = it.filter { ch -> ch.isDigit() || ch == '.' } }, "Carbs", Modifier.weight(1f), keyboard = KeyboardType.Decimal)
                        AppTextField(f, { f = it.filter { ch -> ch.isDigit() || ch == '.' } }, "Fat", Modifier.weight(1f), keyboard = KeyboardType.Decimal)
                    }
                }
            },
            confirmButton = {
                TextButton(enabled = name.isNotBlank() && kcal.toDoubleOrNull() != null, onClick = {
                    scope.launch {
                        nutrition.logCustom(day, meal, name.trim(), kcal.toDouble(), p.toDoubleOrNull() ?: 0.0, c.toDoubleOrNull() ?: 0.0, f.toDoubleOrNull() ?: 0.0)
                        showCustom = false; onDone()
                    }
                }) { Text("Add") }
            },
            dismissButton = { TextButton(onClick = { showCustom = false }) { Text("Cancel") } }
        )
    }
}
