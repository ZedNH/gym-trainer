package com.gymtrainer.app.ui.body

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Accessibility
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.MonitorWeight
import androidx.compose.material.icons.filled.Straighten
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.gymtrainer.app.domain.Units
import com.gymtrainer.app.ui.Format
import com.gymtrainer.app.ui.components.AppCard
import com.gymtrainer.app.ui.components.AppTextField
import com.gymtrainer.app.ui.components.DetailTopBar
import com.gymtrainer.app.ui.components.Gutter
import com.gymtrainer.app.ui.components.IconBadge
import com.gymtrainer.app.ui.components.LineChart
import com.gymtrainer.app.ui.components.PillButton
import com.gymtrainer.app.ui.rememberRepository
import com.gymtrainer.app.ui.theme.AppTheme
import com.gymtrainer.app.ui.theme.Brand
import kotlinx.coroutines.launch

private data class Metric(val type: String, val label: String, val isWeight: Boolean, val icon: ImageVector, val tint: Color)

private val metrics = listOf(
    Metric("WEIGHT", "Body weight", true, Icons.Filled.MonitorWeight, Brand.Teal),
    Metric("WAIST", "Waist", false, Icons.Filled.Straighten, Brand.Orange),
    Metric("CHEST", "Chest", false, Icons.Filled.Accessibility, Brand.Blue),
    Metric("HIPS", "Hips", false, Icons.Filled.Accessibility, Brand.Pink),
    Metric("ARMS", "Arms", false, Icons.Filled.FitnessCenter, Brand.Purple),
    Metric("THIGHS", "Thighs", false, Icons.Filled.Straighten, Brand.Green)
)

private const val CM_PER_IN = 2.54

@Composable
fun BodyScreen(onBack: () -> Unit) {
    val repo = rememberRepository()
    val scope = rememberCoroutineScope()
    val all by repo.observeMeasurements().collectAsState(initial = emptyList())
    val units by produceState(Units.METRIC) { value = Units.from(repo.getProfile()?.units) }
    var editing by remember { mutableStateOf<Metric?>(null) }
    val imperial = units == Units.IMPERIAL

    fun display(m: Metric, v: Double) = if (m.isWeight) Format.weightValue(v, units) else if (imperial) v / CM_PER_IN else v
    fun unit(m: Metric) = if (m.isWeight) Format.weightUnit(units) else if (imperial) "in" else "cm"

    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        DetailTopBar("Body measurements", onBack)
        LazyColumn(contentPadding = PaddingValues(start = Gutter, end = Gutter, bottom = 28.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item {
                Text("Measure at the same time of day (e.g. mornings) for reliable trends. Look at the trend, not single readings.",
                    style = MaterialTheme.typography.bodySmall, color = AppTheme.colors.textMuted)
            }
            items(metrics, key = { it.type }) { m ->
                val values = all.filter { it.type == m.type }.sortedBy { it.date }
                AppCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconBadge(m.icon, m.tint, 42.dp)
                        Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                            Text(m.label, style = MaterialTheme.typography.bodySmall, color = AppTheme.colors.textMuted)
                            Text(values.lastOrNull()?.let { "${Format.trim(Math.round(display(m, it.value) * 10) / 10.0)} ${unit(m)}" } ?: "—",
                                style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onSurface)
                            if (values.size >= 2) {
                                val d = display(m, values.last().value) - display(m, values.first().value)
                                Text((if (d >= 0) "+" else "") + Format.trim(Math.round(d * 10) / 10.0) + " ${unit(m)} since start",
                                    style = MaterialTheme.typography.bodySmall, color = AppTheme.colors.textMuted)
                            }
                        }
                        PillButton("Log", { editing = m }, color = m.tint)
                    }
                    if (values.size >= 2) LineChart(values.map { display(m, it.value).toFloat() }, lineColor = m.tint)
                }
            }
        }
    }

    editing?.let { m ->
        var input by remember(m) { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { editing = null },
            containerColor = AppTheme.colors.card,
            title = { Text("Log ${m.label.lowercase()}") },
            text = { AppTextField(input, { input = it.filter { c -> c.isDigit() || c == '.' } }, "Value (${unit(m)})", keyboard = KeyboardType.Decimal) },
            confirmButton = {
                TextButton(enabled = input.toDoubleOrNull() != null, onClick = {
                    val v = input.toDouble()
                    val stored = if (m.isWeight) Format.toKg(v, units) else if (imperial) v * CM_PER_IN else v
                    scope.launch { repo.addMeasurement(m.type, stored) }
                    editing = null
                }) { Text("Save") }
            },
            dismissButton = { TextButton(onClick = { editing = null }) { Text("Cancel") } }
        )
    }
}
