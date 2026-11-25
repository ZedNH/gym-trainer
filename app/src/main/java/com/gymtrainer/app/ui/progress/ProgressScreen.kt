package com.gymtrainer.app.ui.progress

import androidx.compose.material.icons.filled.BarChart as BarChartIcon

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.MonitorWeight
import androidx.compose.material.icons.filled.Straighten
import androidx.compose.runtime.collectAsState
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gymtrainer.app.ui.Format
import com.gymtrainer.app.ui.components.AppCard
import com.gymtrainer.app.ui.components.AppTextField
import com.gymtrainer.app.ui.components.BarChart
import com.gymtrainer.app.ui.components.EmptyState
import com.gymtrainer.app.ui.components.Gutter
import com.gymtrainer.app.ui.components.IconBadge
import com.gymtrainer.app.ui.components.LineChart
import com.gymtrainer.app.ui.components.ListRow
import com.gymtrainer.app.ui.components.PillButton
import com.gymtrainer.app.ui.components.SectionHeader
import com.gymtrainer.app.ui.components.StatBlock
import com.gymtrainer.app.ui.components.TabHeader
import com.gymtrainer.app.ui.gymViewModel
import com.gymtrainer.app.ui.theme.AppTheme
import com.gymtrainer.app.ui.theme.Brand
import com.gymtrainer.app.ui.tools.Calc

@Composable
fun ProgressScreen(contentPadding: PaddingValues, onBody: () -> Unit = {}, onVolume: () -> Unit = {}) {
    val vm = gymViewModel { ProgressViewModel(it) }
    val state by vm.state.collectAsStateWithLifecycle()
    val repo = com.gymtrainer.app.ui.rememberRepository()
    val nutrition = com.gymtrainer.app.ui.rememberNutrition()
    val sessions by repo.observeCompletedSessions().collectAsState(initial = emptyList())
    val programDays by repo.observeAllChallengeProgress().collectAsState(initial = emptyList())
    val foodDays by nutrition.observeLoggedDays().collectAsState(initial = 0)
    val measures by repo.observeMeasurements().collectAsState(initial = emptyList())
    val achievements = remember(sessions, programDays, foodDays, measures, state.records) {
        com.gymtrainer.app.domain.Achievements.compute(
            com.gymtrainer.app.domain.AchievementInputs(
                workouts = sessions.size,
                homeWorkouts = sessions.count { it.kind == "HOME" },
                gymWorkouts = sessions.count { it.kind == "GYM" },
                longestStreak = com.gymtrainer.app.domain.Achievements.longestStreak(
                    sessions.map { com.gymtrainer.app.data.NutritionRepository.dayOf(it.startedAt) }),
                totalKcal = sessions.sumOf { it.calories },
                totalMinutes = sessions.sumOf { it.durationSec } / 60,
                records = state.records.size,
                programDays = programDays.size,
                foodDays = foodDays,
                measurements = measures.size
            )
        )
    }
    var showAdd by remember { mutableStateOf(false) }
    var weightInput by remember { mutableStateOf("") }

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = contentPadding.calculateBottomPadding() + 28.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item { TabHeader("Progress", hint = "Your results, records and body changes") }
        item {
            Column(Modifier.padding(horizontal = Gutter), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    StatBlock("${state.totalSessions}", "workouts", Modifier.weight(1f), Icons.Filled.FitnessCenter, Brand.Blue)
                    StatBlock("${state.totalKcal}", "kcal burned", Modifier.weight(1f), Icons.Filled.LocalFireDepartment, Brand.Orange)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    StatBlock("${state.totalMinutes}", "minutes", Modifier.weight(1f), Icons.Filled.Timer, Brand.Purple)
                    StatBlock("${state.records.size}", "records", Modifier.weight(1f), Icons.Filled.EmojiEvents, Brand.Amber)
                }
            }
        }

        // Weight
        item {
            AppCard(Modifier.padding(horizontal = Gutter)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconBadge(Icons.Filled.MonitorWeight, Brand.Teal, 40.dp)
                    Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                        Text("Body weight", style = MaterialTheme.typography.bodySmall, color = AppTheme.colors.textMuted)
                        Text(state.latestWeight, style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onSurface)
                    }
                    PillButton("Log", { showAdd = true })
                }
                if (state.weightSeries.size >= 2) {
                    val delta = state.weightSeries.last() - state.weightSeries.first()
                    Text(
                        (if (delta >= 0) "+" else "") + Format.trim(Math.round(delta * 10) / 10.0) + " ${Format.weightUnit(state.units)} since you started",
                        style = MaterialTheme.typography.bodySmall, color = AppTheme.colors.textMuted, modifier = Modifier.padding(top = 8.dp)
                    )
                }
                LineChart(values = state.weightSeries, lineColor = Brand.Teal)
            }
        }

        item {
            ListRow("Body measurements", "Waist, chest, hips, arms, thighs", onClick = onBody,
                modifier = Modifier.padding(horizontal = Gutter),
                leading = { IconBadge(Icons.Filled.Straighten, Brand.Orange, 46.dp) })
        }
        item {
            ListRow("Weekly sets per muscle", "Is every muscle getting enough work?", onClick = onVolume,
                modifier = Modifier.padding(horizontal = Gutter),
                leading = { IconBadge(Icons.Filled.BarChartIcon, Brand.Purple, 46.dp) })
        }

        // Achievements
        item {
            val unlocked = achievements.count { it.unlocked }
            SectionHeader("Achievements · $unlocked/${achievements.size}")
        }
        items(achievements.sortedByDescending { it.progress }.chunked(2)) { row ->
            Row(Modifier.padding(horizontal = Gutter), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                row.forEach { a -> AchievementTile(a, Modifier.weight(1f)) }
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
        }

        // BMI
        if (state.bmi > 0) {
            item {
                AppCard(Modifier.padding(horizontal = Gutter)) {
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text("BMI", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
                        Text(Format.trim(Math.round(state.bmi * 10) / 10.0), style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onSurface)
                        Text("  ${Calc.bmiCategory(state.bmi)}", style = MaterialTheme.typography.bodyMedium, color = AppTheme.colors.textMuted)
                    }
                    Spacer(Modifier.height(12.dp))
                    BmiBar(state.bmi)
                    // Legend aligned with the segment widths (3.5 : 6.5 : 5 : 5).
                    Row(Modifier.fillMaxWidth().padding(top = 6.dp)) {
                        listOf(3.5f to "Under", 6.5f to "Healthy", 5f to "Over", 5f to "Obese").forEach { (w, label) ->
                            Text(label, style = MaterialTheme.typography.labelSmall, color = AppTheme.colors.textFaint,
                                modifier = Modifier.weight(w), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                        }
                    }
                }
            }
        }

        // Weekly volume
        item {
            AppCard(Modifier.padding(horizontal = Gutter)) {
                Text("Gym sets this week", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                Text("By muscle group", style = MaterialTheme.typography.bodySmall, color = AppTheme.colors.textMuted)
                Spacer(Modifier.height(12.dp))
                BarChart(data = state.weeklySets)
            }
        }

        // Records
        item { SectionHeader("Personal records") }
        if (state.records.isEmpty()) {
            item { EmptyState(Icons.Filled.EmojiEvents, "No records yet", "Log weighted sets in a gym workout and your best lifts appear here.", Modifier.padding(horizontal = Gutter)) }
        } else {
            item {
                AppCard(Modifier.padding(horizontal = Gutter)) {
                    state.records.forEachIndexed { i, pr ->
                        Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(pr.exerciseName, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)
                                Text("Est. 1RM ${Format.weight(pr.estimatedOneRm, state.units)}", style = MaterialTheme.typography.bodySmall, color = AppTheme.colors.textMuted)
                            }
                            Text("${Format.weight(pr.weightKg, state.units)} × ${pr.reps}", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
                        }
                        if (i < state.records.lastIndex) Spacer(Modifier.height(1.dp))
                    }
                }
            }
        }

        // History
        item { SectionHeader("History") }
        if (state.sessions.isEmpty()) {
            item { EmptyState(Icons.Filled.History, "No workouts yet", "Finish your first workout and it will show up here.", Modifier.padding(horizontal = Gutter)) }
        }
        items(state.sessions) { s ->
            ListRow(
                title = s.name,
                meta = "${s.date} · ${s.duration}" + if (s.calories > 0) " · ${s.calories} kcal" else "",
                onClick = {}, modifier = Modifier.padding(horizontal = Gutter),
                leading = { IconBadge(if (s.home) Icons.Filled.Home else Icons.Filled.FitnessCenter, if (s.home) Brand.Teal else Brand.Blue, 46.dp) },
                trailing = {}
            )
        }
    }

    if (showAdd) {
        AlertDialog(
            onDismissRequest = { showAdd = false },
            title = { Text("Log body weight") },
            text = {
                AppTextField(weightInput, { weightInput = it.filter { c -> c.isDigit() || c == '.' } },
                    placeholder = "Weight (${Format.weightUnit(state.units)})", keyboard = KeyboardType.Decimal)
            },
            confirmButton = {
                TextButton(onClick = { weightInput.toDoubleOrNull()?.let { vm.addWeight(it) }; weightInput = ""; showAdd = false }) { Text("Save") }
            },
            dismissButton = { TextButton(onClick = { showAdd = false }) { Text("Cancel") } },
            containerColor = AppTheme.colors.card
        )
    }
}

@Composable
private fun AchievementTile(a: com.gymtrainer.app.domain.Achievement, modifier: Modifier) {
    AppCard(modifier, padding = PaddingValues(14.dp),
        color = if (a.unlocked) Brand.Amber.copy(alpha = 0.10f) else AppTheme.colors.card) {
        IconBadge(Icons.Filled.EmojiEvents, if (a.unlocked) Brand.Amber else AppTheme.colors.textFaint, 38.dp)
        Spacer(Modifier.height(8.dp))
        Text(a.title, style = MaterialTheme.typography.titleSmall,
            color = if (a.unlocked) MaterialTheme.colorScheme.onSurface else AppTheme.colors.textMuted, maxLines = 1)
        Text(a.desc, style = MaterialTheme.typography.bodySmall, color = AppTheme.colors.textMuted, maxLines = 2, minLines = 2)
        Spacer(Modifier.height(8.dp))
        if (a.unlocked) Text("Unlocked ✓", style = MaterialTheme.typography.labelMedium, color = Brand.Amber)
        else {
            com.gymtrainer.app.ui.components.LinearMeter(a.progress, color = Brand.Amber, height = 6.dp)
            Text("${minOf(a.current, a.target)}/${a.target}", style = MaterialTheme.typography.labelSmall, color = AppTheme.colors.textFaint,
                modifier = Modifier.padding(top = 4.dp))
        }
    }
}

/** Horizontal BMI scale (15–35) with a marker at the user's value. */
@Composable
private fun BmiBar(bmi: Double) {
    val segments = listOf(3.5f to Color(0xFF5AC8FA), 6.5f to Brand.Green, 5f to Brand.Amber, 5f to Brand.Red)
    val pos = ((bmi - 15.0) / 20.0).coerceIn(0.0, 1.0).toFloat()
    val ring = AppTheme.colors.card
    Canvas(Modifier.fillMaxWidth().height(22.dp)) {
        var x = 0f
        val h = 8f * density
        val gap = 3f * density
        segments.forEach { (w, c) ->
            val segW = size.width * (w / 20f)
            drawRoundRect(c, Offset(x, size.height / 2 - h / 2), Size(segW - gap, h), androidx.compose.ui.geometry.CornerRadius(h, h))
            x += segW
        }
        val mx = size.width * pos
        drawCircle(ring, radius = 10f * density, center = Offset(mx, size.height / 2))
        drawCircle(Color(0xFF101828), radius = 6f * density, center = Offset(mx, size.height / 2))
    }
}
