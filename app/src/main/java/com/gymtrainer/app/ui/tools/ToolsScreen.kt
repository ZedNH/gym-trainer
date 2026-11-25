package com.gymtrainer.app.ui.tools

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gymtrainer.app.domain.Units
import com.gymtrainer.app.ui.Format
import com.gymtrainer.app.ui.components.Pill
import com.gymtrainer.app.ui.components.SectionCard
import com.gymtrainer.app.ui.rememberRepository
import com.gymtrainer.app.ui.theme.Accent

object Calc {
    /** Epley, Brzycki and Lombardi averaged for a stable estimate. */
    fun oneRepMax(weight: Double, reps: Int): Double {
        if (weight <= 0 || reps <= 0) return 0.0
        if (reps == 1) return weight
        val epley = weight * (1 + reps / 30.0)
        val brzycki = weight * 36.0 / (37.0 - reps.coerceAtMost(36))
        val lombardi = weight * Math.pow(reps.toDouble(), 0.10)
        return (epley + brzycki + lombardi) / 3.0
    }

    /** Greedy plate breakdown per side. */
    fun plates(target: Double, bar: Double, available: List<Double>): Pair<List<Double>, Double> {
        var perSide = (target - bar) / 2.0
        if (perSide <= 0) return emptyList<Double>() to 0.0
        val out = mutableListOf<Double>()
        for (p in available.sortedDescending()) {
            while (perSide + 1e-6 >= p) { out += p; perSide -= p }
        }
        return out to perSide
    }

    fun bmi(weightKg: Double, heightCm: Double): Double =
        if (heightCm <= 0) 0.0 else weightKg / ((heightCm / 100.0) * (heightCm / 100.0))

    fun bmiCategory(bmi: Double): String = when {
        bmi <= 0 -> "—"
        bmi < 18.5 -> "Underweight"
        bmi < 25 -> "Healthy weight"
        bmi < 30 -> "Overweight"
        else -> "Obese"
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ToolsScreen(onBack: () -> Unit) {
    val repo = rememberRepository()
    val profile by produceState<com.gymtrainer.app.data.UserProfileEntity?>(null) { value = repo.getProfile() }
    val units = Units.from(profile?.units)
    val unit = Format.weightUnit(units)
    val imperial = units == Units.IMPERIAL

    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).navigationBarsPadding().verticalScroll(rememberScrollState())) {
        com.gymtrainer.app.ui.components.DetailTopBar("Gym tools", onBack)
        Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {

            // ---- 1RM ----
            var w by remember { mutableStateOf("") }
            var r by remember { mutableStateOf("") }
            SectionCard {
                Text("One-rep max calculator", fontWeight = FontWeight.SemiBold, color = Accent)
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    NumField(w, { w = it }, "Weight ($unit)", Modifier.weight(1f))
                    NumField(r, { r = it.filter { c -> c.isDigit() } }, "Reps", Modifier.weight(1f), decimal = false)
                }
                val orm = Calc.oneRepMax(w.toDoubleOrNull() ?: 0.0, r.toIntOrNull() ?: 0)
                if (orm > 0) {
                    Spacer(Modifier.height(10.dp))
                    Text("Estimated 1RM: ${Format.trim(orm)} $unit", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = MaterialTheme.colorScheme.onBackground)
                    Spacer(Modifier.height(6.dp))
                    listOf(95 to 2, 90 to 4, 85 to 6, 80 to 8, 75 to 10, 70 to 12, 65 to 15).forEach { (pct, reps) ->
                        Row(Modifier.fillMaxWidth().padding(vertical = 2.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("$pct%  (~$reps reps)", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
                            Text("${Format.trim(Math.round(orm * pct / 100.0 * 2) / 2.0)} $unit", color = MaterialTheme.colorScheme.onBackground, fontSize = 13.sp)
                        }
                    }
                }
            }

            // ---- Plates ----
            var target by remember { mutableStateOf("") }
            var bar by remember { mutableStateOf(if (imperial) 45.0 else 20.0) }
            SectionCard {
                Text("Plate loader", fontWeight = FontWeight.SemiBold, color = Accent)
                Spacer(Modifier.height(8.dp))
                NumField(target, { target = it }, "Target weight ($unit)", Modifier.fillMaxWidth())
                Spacer(Modifier.height(8.dp))
                Text("Bar", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                val bars = if (imperial) listOf(45.0, 35.0, 25.0, 15.0) else listOf(20.0, 15.0, 10.0, 7.5)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    bars.forEach { b ->
                        FilterChip(selected = bar == b, onClick = { bar = b }, label = { Text("${Format.trim(b)} $unit") },
                            colors = FilterChipDefaults.filterChipColors(selectedContainerColor = Accent, selectedLabelColor = MaterialTheme.colorScheme.onPrimary))
                    }
                }
                val t = target.toDoubleOrNull()
                if (t != null && t > bar) {
                    val avail = if (imperial) listOf(45.0, 35.0, 25.0, 10.0, 5.0, 2.5) else listOf(25.0, 20.0, 15.0, 10.0, 5.0, 2.5, 1.25)
                    val (plates, leftover) = Calc.plates(t, bar, avail)
                    Spacer(Modifier.height(10.dp))
                    Text("Each side:", fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onBackground)
                    FlowRow { plates.forEach { Pill("${Format.trim(it)}") } }
                    if (leftover > 0.01) Text("Closest load is ${Format.trim(t - leftover * 2)} $unit", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            // ---- BMI ----
            var hText by remember(profile) { mutableStateOf(profile?.heightCm?.let { Format.trim(it) } ?: "") }
            var wText by remember(profile) { mutableStateOf(profile?.weightKg?.let { Format.trim(it) } ?: "") }
            SectionCard {
                Text("BMI calculator", fontWeight = FontWeight.SemiBold, color = Accent)
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    NumField(hText, { hText = it }, "Height (cm)", Modifier.weight(1f))
                    NumField(wText, { wText = it }, "Weight (kg)", Modifier.weight(1f))
                }
                val bmi = Calc.bmi(wText.toDoubleOrNull() ?: 0.0, hText.toDoubleOrNull() ?: 0.0)
                if (bmi > 0) {
                    Spacer(Modifier.height(10.dp))
                    Text("BMI ${Format.trim(Math.round(bmi * 10) / 10.0)} — ${Calc.bmiCategory(bmi)}", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = MaterialTheme.colorScheme.onBackground)
                    Text("BMI is a rough population screen; it doesn't account for muscle mass.", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun NumField(value: String, onChange: (String) -> Unit, label: String, modifier: Modifier, decimal: Boolean = true) {
    OutlinedTextField(
        value = value,
        onValueChange = { v -> onChange(v.filter { it.isDigit() || (decimal && it == '.') }) },
        label = { Text(label) }, singleLine = true, modifier = modifier,
        keyboardOptions = KeyboardOptions(keyboardType = if (decimal) KeyboardType.Decimal else KeyboardType.Number)
    )
}
