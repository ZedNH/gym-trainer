package com.gymtrainer.app.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.gymtrainer.app.domain.HomePrograms
import com.gymtrainer.app.domain.ProgramKind
import com.gymtrainer.app.ui.components.AppCard
import com.gymtrainer.app.ui.components.CircleIconButton
import com.gymtrainer.app.ui.components.DetailTopBar
import com.gymtrainer.app.ui.components.ExerciseImage
import com.gymtrainer.app.ui.components.Gutter
import com.gymtrainer.app.ui.components.LevelBadge
import com.gymtrainer.app.ui.components.PillButton
import com.gymtrainer.app.ui.components.PrimaryButton
import com.gymtrainer.app.ui.components.ProgressRing
import com.gymtrainer.app.ui.components.Tag
import com.gymtrainer.app.ui.components.Visuals
import com.gymtrainer.app.ui.rememberExerciseMap
import com.gymtrainer.app.ui.rememberRepository
import com.gymtrainer.app.ui.rememberSettings
import com.gymtrainer.app.ui.theme.AppTheme
import com.gymtrainer.app.ui.train.Fact
import kotlinx.coroutines.launch

@Composable
fun HomeWorkoutPreviewScreen(workoutKey: String, onBack: () -> Unit, onStart: () -> Unit, onExercise: (String) -> Unit) {
    val workout = remember(workoutKey) { HomePrograms.resolve(workoutKey) }
    val exercises = rememberExerciseMap()
    val settings = rememberSettings()
    val repo = rememberRepository()
    val weight by produceState(70.0) { value = repo.getProfile()?.weightKg ?: 70.0 }

    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        if (workout == null) {
            DetailTopBar("Workout", onBack)
            Text("This workout could not be found.", Modifier.padding(Gutter), color = AppTheme.colors.textMuted)
            return@Column
        }
        val (icon, tint) = Visuals.category(workout.category)
        val secs = workout.estimatedSeconds(workout.restSeconds ?: settings.homeRestSeconds)
        LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(bottom = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            item {
                Box(Modifier.fillMaxWidth().background(Brush.linearGradient(Visuals.gradient(tint)))) {
                    Icon(icon, contentDescription = null, tint = Color.White.copy(alpha = 0.15f),
                        modifier = Modifier.size(200.dp).align(Alignment.BottomEnd))
                    Column(Modifier.statusBarsPadding().padding(horizontal = Gutter, vertical = 8.dp)) {
                        CircleIconButton(Icons.AutoMirrored.Filled.ArrowBack, "Back", onBack, tint = Color.White, background = Color.White.copy(alpha = 0.2f))
                        Spacer(Modifier.height(28.dp))
                        Text(workout.category.label.uppercase(), style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.85f))
                        Text(workout.title, style = MaterialTheme.typography.headlineMedium, color = Color.White)
                        Text(workout.subtitle, style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha = 0.9f))
                        Spacer(Modifier.height(22.dp))
                    }
                }
            }
            item {
                Row(Modifier.padding(horizontal = Gutter), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Fact("${workout.estimatedMinutes(workout.restSeconds ?: settings.homeRestSeconds)}", "minutes", Modifier.weight(1f))
                    Fact("${workout.items.size}", "exercises", Modifier.weight(1f))
                    Fact("~${workout.calories(weight, secs)}", "kcal", Modifier.weight(1f))
                }
            }
            item {
                Row(Modifier.fillMaxWidth().padding(horizontal = Gutter, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("Exercises", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onBackground, modifier = Modifier.weight(1f))
                    LevelBadge(workout.level)
                }
            }
            itemsIndexed(workout.items) { i, item ->
                val ex = exercises[item.exerciseId]
                AppCard(Modifier.padding(horizontal = Gutter), onClick = { onExercise(item.exerciseId) }, padding = PaddingValues(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        ExerciseImage(ex, Modifier.width(84.dp).aspectRatio(1.4f), animate = false, thumbnail = true, corner = 12.dp)
                        Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                            Text(ex?.name ?: item.exerciseId, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 2, overflow = TextOverflow.Ellipsis)
                            Text(item.label(), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                        }
                        Text("${i + 1}", style = MaterialTheme.typography.labelMedium, color = AppTheme.colors.textFaint)
                    }
                }
            }
        }
        PrimaryButton("Start workout", onStart, icon = Icons.Filled.PlayArrow,
            modifier = Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = Gutter, vertical = 12.dp))
    }
}

/** Day grid for any program: 30-day challenges, catalog plans and the personal plan. */
@Composable
fun ProgramScreen(programId: String, onBack: () -> Unit, onOpenDay: (String) -> Unit) {
    val program = remember(programId) { HomePrograms.program(programId) }
    val repo = rememberRepository()
    val scope = rememberCoroutineScope()
    val rest = rememberSettings().homeRestSeconds
    val progress by repo.observeChallenge(programId).collectAsState(initial = emptyList())
    var confirmReset by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        DetailTopBar(program?.title ?: "Program", onBack) {
            if (program != null) CircleIconButton(Icons.Filled.Refresh, "Restart", { confirmReset = true })
        }
        if (program == null) return@Column
        val card = programCard(program, progress)
        val done = progress.map { it.day }.toSet()
        val next = card.nextDay
        val nextWorkout = next?.let { program.workoutFor(it) }

        LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(bottom = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item {
                AppCard(Modifier.padding(horizontal = Gutter)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        ProgressRing(card.done / card.total.toFloat(), size = 84.dp, stroke = 9.dp) {
                            Text("${(card.done * 100 / card.total)}%", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                        }
                        Column(Modifier.padding(start = 16.dp)) {
                            Text("${card.done} of ${card.total} days", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurface)
                            Text(program.subtitle, style = MaterialTheme.typography.bodySmall, color = AppTheme.colors.textMuted)
                            Spacer(Modifier.height(8.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                program.level?.let { LevelBadge(it) }
                                Tag(when (program.kind) { ProgramKind.CHALLENGE -> "Challenge"; ProgramKind.PLAN -> program.goalLabel; ProgramKind.PERSONAL -> "Personalized" })
                            }
                        }
                    }
                }
            }
            if (nextWorkout != null) {
                item {
                    AppCard(Modifier.padding(horizontal = Gutter), onClick = { onOpenDay(nextWorkout.key) }) {
                        Text("UP NEXT · DAY $next", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.height(2.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(nextWorkout.title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                                Text("${nextWorkout.items.size} exercises · ${nextWorkout.estimatedMinutes(nextWorkout.restSeconds ?: rest)} min",
                                    style = MaterialTheme.typography.bodySmall, color = AppTheme.colors.textMuted)
                            }
                            PillButton("Go", { onOpenDay(nextWorkout.key) }, icon = Icons.Filled.PlayArrow)
                        }
                    }
                }
            } else {
                item {
                    AppCard(Modifier.padding(horizontal = Gutter)) {
                        Text("Program complete 🏆", style = MaterialTheme.typography.titleLarge, color = AppTheme.colors.success)
                        Text("Amazing consistency. Restart it or pick a harder level.", style = MaterialTheme.typography.bodyMedium, color = AppTheme.colors.textMuted)
                    }
                }
            }
            itemsIndexed((1..program.totalDays).chunked(7)) { week, days ->
                AppCard(Modifier.padding(horizontal = Gutter)) {
                    Text("Week ${week + 1}", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)
                    Spacer(Modifier.height(10.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        days.forEach { day ->
                            val isDone = day in done
                            val isRest = program.isRestDay(day)
                            val isNext = day == next
                            val unlocked = isDone || isNext || (next != null && day < next) || next == null
                            Box(
                                Modifier.weight(1f).aspectRatio(1f).clip(CircleShape)
                                    .background(when { isDone -> MaterialTheme.colorScheme.primary; isNext -> MaterialTheme.colorScheme.primaryContainer; else -> AppTheme.colors.cardAlt })
                                    .then(if (isNext) Modifier.border(2.dp, MaterialTheme.colorScheme.primary, CircleShape) else Modifier)
                                    .clickable(enabled = unlocked && !isRest) { program.workoutFor(day)?.let { onOpenDay(it.key) } },
                                contentAlignment = Alignment.Center
                            ) {
                                when {
                                    isDone -> Icon(Icons.Filled.Check, contentDescription = "Day $day done", tint = Color.White, modifier = Modifier.size(16.dp))
                                    isRest -> Icon(Icons.Filled.Bedtime, contentDescription = "Rest day", tint = AppTheme.colors.textFaint, modifier = Modifier.size(15.dp))
                                    else -> Text("$day", style = MaterialTheme.typography.labelMedium,
                                        color = if (isNext) MaterialTheme.colorScheme.primary else if (unlocked) MaterialTheme.colorScheme.onSurface else AppTheme.colors.textFaint)
                                }
                            }
                        }
                        repeat(7 - days.size) { Spacer(Modifier.weight(1f)) }
                    }
                }
            }
        }
        if (nextWorkout != null) {
            PrimaryButton("Start day $next", { onOpenDay(nextWorkout.key) }, icon = Icons.Filled.PlayArrow,
                modifier = Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = Gutter, vertical = 12.dp))
        }
    }

    if (confirmReset) {
        AlertDialog(
            onDismissRequest = { confirmReset = false },
            title = { Text("Restart program?") },
            text = { Text("Progress for this program will be cleared. Your workout history is kept.") },
            confirmButton = { TextButton(onClick = { scope.launch { repo.resetChallenge(programId) }; confirmReset = false }) { Text("Restart") } },
            dismissButton = { TextButton(onClick = { confirmReset = false }) { Text("Cancel") } }
        )
    }
}

@Suppress("unused")
private val previewShape = RoundedCornerShape(12.dp)
