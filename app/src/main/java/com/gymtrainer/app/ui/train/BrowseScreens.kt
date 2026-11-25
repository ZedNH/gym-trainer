package com.gymtrainer.app.ui.train

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.gymtrainer.app.data.ExerciseEntity
import com.gymtrainer.app.data.WorkoutDayExerciseEntity
import com.gymtrainer.app.domain.Difficulty
import com.gymtrainer.app.domain.GymPrograms
import com.gymtrainer.app.domain.HomePrograms
import com.gymtrainer.app.domain.WorkoutCategory
import com.gymtrainer.app.ui.components.AppCard
import com.gymtrainer.app.ui.components.DetailTopBar
import com.gymtrainer.app.ui.components.EmptyState
import com.gymtrainer.app.ui.components.ExerciseImage
import com.gymtrainer.app.ui.components.FilterPill
import com.gymtrainer.app.ui.components.Gutter
import com.gymtrainer.app.ui.components.HeroCard
import com.gymtrainer.app.ui.components.IconBadge
import com.gymtrainer.app.ui.components.LevelBadge
import com.gymtrainer.app.ui.components.PrimaryButton
import com.gymtrainer.app.ui.components.Visuals
import com.gymtrainer.app.ui.components.levelColor
import com.gymtrainer.app.ui.home.WorkoutRow
import com.gymtrainer.app.ui.rememberRepository
import com.gymtrainer.app.ui.rememberSettings
import com.gymtrainer.app.ui.theme.AppTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// ---------------- Category ----------------

@Composable
fun CategoryScreen(categoryName: String, onBack: () -> Unit, onOpen: (String) -> Unit) {
    val cat = remember(categoryName) { runCatching { WorkoutCategory.valueOf(categoryName) }.getOrDefault(WorkoutCategory.FULL_BODY) }
    val (icon, tint) = Visuals.category(cat)
    val rest = rememberSettings().homeRestSeconds
    var levelName by rememberSaveable { mutableStateOf<String?>(null) }
    val workouts = HomePrograms.byCategory(cat).filter { w -> levelName == null || w.level.name == levelName }

    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        DetailTopBar(cat.label, onBack)
        LazyColumn(
            contentPadding = PaddingValues(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                HeroCard(Visuals.gradient(tint), Modifier.padding(horizontal = Gutter).fillMaxWidth(), icon) {
                    Column {
                        Text(cat.label, style = MaterialTheme.typography.headlineMedium, color = Color.White)
                        Text(cat.blurb, style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha = 0.85f))
                        Spacer(Modifier.height(18.dp))
                        Text("${HomePrograms.byCategory(cat).size} workouts", style = MaterialTheme.typography.labelMedium, color = Color.White)
                    }
                }
            }
            item {
                LazyRow(contentPadding = PaddingValues(horizontal = Gutter), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    item { FilterPill("All levels", levelName == null) { levelName = null } }
                    items(Difficulty.entries) { d -> FilterPill(d.label, levelName == d.name) { levelName = d.name } }
                }
            }
            if (workouts.isEmpty()) {
                item { EmptyState(icon, "Nothing at this level yet", "Try another level.", Modifier.padding(horizontal = Gutter)) }
            }
            items(workouts, key = { it.key }) { w ->
                WorkoutRow(w, rest, Modifier.padding(horizontal = Gutter)) { onOpen(w.key) }
            }
        }
    }
}

// ---------------- Gym program ----------------

@Composable
fun GymProgramScreen(programId: String, onBack: () -> Unit, onAdopted: () -> Unit) {
    val program = remember(programId) { GymPrograms.byId(programId) }
    val repo = rememberRepository()
    val scope = rememberCoroutineScope()
    val names by produceState(emptyMap<String, String>()) { value = repo.exerciseMap().mapValues { it.value.name } }
    val active by produceState<String?>(null) { value = repo.activePlan()?.name }
    var saving by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        DetailTopBar("Program", onBack)
        if (program == null) return@Column
        LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(bottom = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item {
                HeroCard(Visuals.gradient(levelColor(program.level)), Modifier.padding(horizontal = Gutter).fillMaxWidth(), Icons.Filled.CalendarMonth) {
                    Column {
                        Text(program.level.label.uppercase(), style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.85f))
                        Text(program.title, style = MaterialTheme.typography.headlineMedium, color = Color.White)
                        Text("${program.subtitle} · ${program.equipment}", style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha = 0.9f))
                    }
                }
            }
            item {
                Text(program.description, style = MaterialTheme.typography.bodyLarge, color = AppTheme.colors.textMuted,
                    modifier = Modifier.padding(horizontal = Gutter))
            }
            itemsIndexed(program.days) { i, day ->
                AppCard(Modifier.padding(horizontal = Gutter)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("DAY ${i + 1}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(8.dp))
                        Text(day.name, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
                    }
                    Text(day.focus, style = MaterialTheme.typography.bodySmall, color = AppTheme.colors.textMuted)
                    Spacer(Modifier.height(8.dp))
                    day.items.forEach { item ->
                        Row(Modifier.fillMaxWidth().padding(vertical = 5.dp)) {
                            Text(names[item.id] ?: item.id.replace('_', ' '), style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(if (item.timed) "${item.sets} × ${item.repMin}s" else if (item.repMin == item.repMax) "${item.sets} × ${item.repMin}" else "${item.sets} × ${item.repMin}–${item.repMax}",
                                style = MaterialTheme.typography.labelMedium, color = AppTheme.colors.textMuted)
                        }
                    }
                }
            }
        }
        val isActive = active == program.title
        PrimaryButton(
            if (isActive) "Active program" else if (saving) "Setting up…" else "Use this program",
            onClick = {
                saving = true
                scope.launch { repo.adoptGymProgram(program); withContext(Dispatchers.Main) { onAdopted() } }
            },
            enabled = !isActive && !saving, icon = if (isActive) Icons.Filled.Check else null,
            modifier = Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = Gutter, vertical = 12.dp)
        )
    }
}

// ---------------- Gym routine preview ----------------

private data class RoutineLoaded(val name: String, val focus: String, val rows: List<Pair<WorkoutDayExerciseEntity, ExerciseEntity?>>)

@Composable
fun RoutineScreen(dayId: Long, onBack: () -> Unit, onStart: () -> Unit, onExercise: (String) -> Unit) {
    val repo = rememberRepository()
    val loaded by produceState<RoutineLoaded?>(null, dayId) {
        val day = repo.day(dayId)
        value = if (day == null) RoutineLoaded("Workout", "", emptyList())
        else RoutineLoaded(day.name, day.focus, repo.dayExercises(dayId).map { it to repo.exercise(it.exerciseId) })
    }

    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        DetailTopBar(loaded?.name ?: "", onBack)
        val l = loaded ?: return@Column
        val totalSets = l.rows.sumOf { it.first.sets }
        val minutes = (l.rows.sumOf { (de, _) -> de.sets * (40 + de.restSeconds) } / 60.0).toInt().coerceAtLeast(5)
        LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(bottom = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            item {
                Column(Modifier.padding(horizontal = Gutter)) {
                    Text(l.focus, style = MaterialTheme.typography.bodyLarge, color = AppTheme.colors.textMuted)
                    Spacer(Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Fact("${l.rows.size}", "exercises", Modifier.weight(1f))
                        Fact("$totalSets", "sets", Modifier.weight(1f))
                        Fact("~$minutes", "minutes", Modifier.weight(1f))
                    }
                }
            }
            items(l.rows, key = { it.first.id }) { (de, ex) ->
                AppCard(Modifier.padding(horizontal = Gutter), onClick = { onExercise(de.exerciseId) }, padding = PaddingValues(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        ExerciseImage(ex, Modifier.width(84.dp).aspectRatio(1.4f), animate = false, thumbnail = true, corner = 12.dp)
                        Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                            Text(ex?.name ?: de.exerciseId, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface, maxLines = 2)
                            Text(
                                (if (ex?.timed == true) "${de.sets} × ${de.repMin}s" else "${de.sets} × ${de.repMin}–${de.repMax} reps") + " · rest ${de.restSeconds}s",
                                style = MaterialTheme.typography.bodySmall, color = AppTheme.colors.textMuted
                            )
                        }
                    }
                }
            }
        }
        PrimaryButton("Start workout", onStart, enabled = l.rows.isNotEmpty(), icon = Icons.Filled.PlayArrow,
            modifier = Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = Gutter, vertical = 12.dp))
    }
}

@Composable
fun Fact(value: String, label: String, modifier: Modifier = Modifier) {
    AppCard(modifier, padding = PaddingValues(vertical = 12.dp, horizontal = 12.dp)) {
        Text(value, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurface)
        Text(label, style = MaterialTheme.typography.bodySmall, color = AppTheme.colors.textMuted)
    }
}
