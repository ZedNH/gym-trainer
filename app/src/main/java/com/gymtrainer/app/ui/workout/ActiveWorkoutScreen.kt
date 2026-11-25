package com.gymtrainer.app.ui.workout

import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gymtrainer.app.data.ExerciseEntity
import com.gymtrainer.app.domain.Effort
import com.gymtrainer.app.ui.Format
import com.gymtrainer.app.ui.components.AppCard
import com.gymtrainer.app.ui.components.ChoiceCard
import com.gymtrainer.app.ui.components.CircleIconButton
import com.gymtrainer.app.ui.components.ExerciseImage
import com.gymtrainer.app.ui.components.Gutter
import com.gymtrainer.app.ui.components.MotionPrefs
import com.gymtrainer.app.ui.components.PillButton
import com.gymtrainer.app.ui.components.PrimaryButton
import com.gymtrainer.app.ui.components.ProgressRing
import com.gymtrainer.app.ui.components.SecondaryButton
import com.gymtrainer.app.ui.components.StatBlock
import com.gymtrainer.app.ui.components.Tag
import com.gymtrainer.app.ui.gymViewModel
import com.gymtrainer.app.ui.theme.AppTheme
import com.gymtrainer.app.ui.theme.Brand
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun ActiveWorkoutScreen(dayId: Long, onFinish: () -> Unit, onVideo: (String) -> Unit = {}) {
    val vm = gymViewModel { WorkoutViewModel(it) }
    val state by vm.state.collectAsStateWithLifecycle()
    val rest by vm.restRemaining.collectAsStateWithLifecycle()
    val restRunning by vm.restRunning.collectAsStateWithLifecycle()
    LaunchedEffect(dayId) { vm.start(dayId) }

    val startedAt = remember { System.currentTimeMillis() }
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    if (MotionPrefs.animateDemos) LaunchedEffect(Unit) { while (true) { delay(1000); now = System.currentTimeMillis() } }

    if (state.finished && state.summary != null) {
        WorkoutSummaryView(state.summary!!, state.units, onFinish)
        return
    }
    if (state.loading) {
        Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background), contentAlignment = Alignment.Center) { ProgressRing(0.3f, size = 48.dp) }
        return
    }
    if (state.exercises.isEmpty()) {
        Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background), contentAlignment = Alignment.Center) {
            Text("No exercises for this day.", color = AppTheme.colors.textMuted)
        }
        return
    }

    val index = state.currentIndex
    val wex = state.exercises[index]
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var effortFor by remember { mutableStateOf<Int?>(null) }
    var showReplace by remember { mutableStateOf(false) }
    var confirmFinish by remember { mutableStateOf(false) }
    var alternatives by remember { mutableStateOf<List<ExerciseEntity>>(emptyList()) }
    val doneSets = state.exercises.sumOf { e -> e.sets.count { it.done } }

    BackHandler { confirmFinish = true }

    // Voice coach (English or Bangla): announce each exercise, logged sets, and the end of rest.
    val settings = com.gymtrainer.app.ui.rememberSettings()
    val voice = com.gymtrainer.app.ui.rememberCoachVoice()
    var voiceOn by remember { mutableStateOf(settings.voiceCoach) }
    fun coachEx(e: WorkoutExercise) = com.gymtrainer.app.domain.CoachExercise(e.exercise.id, e.exercise.name, e.exercise.pattern, e.exercise.muscleGroup)
    LaunchedEffect(index, wex.exercise.id) {
        delay(600)
        if (voiceOn) voice.say(com.gymtrainer.app.domain.CoachCue.GymExercise(coachEx(wex), wex.sets.size, wex.repMin, wex.repMax))
    }
    var restWasRunning by remember { mutableStateOf(false) }
    LaunchedEffect(rest, restRunning) {
        if (voiceOn && restRunning && rest in 1..3) voice.say(com.gymtrainer.app.domain.CoachCue.Count(rest))
        if (voiceOn && restWasRunning && rest == 0) voice.say(com.gymtrainer.app.domain.CoachCue.RestOver)
        restWasRunning = restRunning && rest > 0
    }

    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).statusBarsPadding().navigationBarsPadding()) {
        // Header
        Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            CircleIconButton(Icons.Filled.Close, "Finish workout", { confirmFinish = true })
            Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                Text(state.dayName, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onBackground, maxLines = 1)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Timer, contentDescription = null, tint = AppTheme.colors.textMuted, modifier = Modifier.size(14.dp))
                    Text(" ${Format.clock(((now - startedAt) / 1000).toInt())} · $doneSets ${if (doneSets == 1) "set" else "sets"} logged", style = MaterialTheme.typography.bodySmall, color = AppTheme.colors.textMuted)
                }
            }
            CircleIconButton(if (voiceOn) Icons.Filled.VolumeUp else Icons.Filled.VolumeOff, "Voice coach",
                { voiceOn = !voiceOn; settings.voiceCoach = voiceOn; if (!voiceOn) voice.stop() })
            Spacer(Modifier.width(8.dp))
            PillButton("Finish", { confirmFinish = true }, color = AppTheme.colors.success)
        }
        // Exercise progress
        Row(Modifier.fillMaxWidth().padding(horizontal = Gutter, vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            state.exercises.forEachIndexed { i, e ->
                val complete = e.sets.isNotEmpty() && e.sets.all { it.done }
                Box(Modifier.weight(1f).height(5.dp).clip(RoundedCornerShape(50)).background(
                    when { complete -> AppTheme.colors.success; i == index -> MaterialTheme.colorScheme.primary; else -> AppTheme.colors.border }
                ).clickable { vm.goTo(i) })
            }
        }

        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = Gutter), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Spacer(Modifier.height(4.dp))
            state.deload?.let { if (index == 0) DeloadBanner(it) }
            AppCard(padding = PaddingValues(0.dp)) {
                ExerciseImage(wex.exercise, Modifier.fillMaxWidth().height(190.dp), corner = 0.dp)
                Column(Modifier.padding(16.dp)) {
                    Text("EXERCISE ${index + 1} OF ${state.exercises.size}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                    Text(wex.exercise.name, style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onSurface)
                    Spacer(Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Tag(if (wex.exercise.timed) "${wex.repMin}–${wex.repMax} sec" else "${wex.repMin}–${wex.repMax} reps")
                        Tag("Rest ${wex.restSeconds}s", AppTheme.colors.textMuted)
                        wex.lastSession?.let { Tag(it, Brand.Purple) }
                    }
                    Spacer(Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        SecondaryButton("Video", { onVideo(wex.exercise.id) }, Modifier.weight(1f).height(46.dp), icon = Icons.Filled.PlayCircle)
                        SecondaryButton("Swap", {
                            scope.launch { alternatives = vm.alternatives(wex.exercise.id); showReplace = true }
                        }, Modifier.weight(1f).height(46.dp), icon = Icons.Filled.SwapHoriz)
                    }
                }
            }

            TargetCard(wex, state.units)
            WarmUpCard(wex, state.units, index)

            // Sets table
            AppCard {
                Row(Modifier.fillMaxWidth().padding(bottom = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("SET", style = MaterialTheme.typography.labelSmall, color = AppTheme.colors.textMuted, modifier = Modifier.width(40.dp))
                    Text(Format.weightUnit(state.units).uppercase(), style = MaterialTheme.typography.labelSmall, color = AppTheme.colors.textMuted,
                        modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
                    Text(if (wex.exercise.timed) "SEC" else "REPS", style = MaterialTheme.typography.labelSmall, color = AppTheme.colors.textMuted,
                        modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
                    Spacer(Modifier.width(52.dp))
                }
                wex.sets.forEachIndexed { si, set ->
                    Row(
                        Modifier.fillMaxWidth().padding(vertical = 4.dp).clip(RoundedCornerShape(12.dp))
                            .background(if (set.done) AppTheme.colors.success.copy(alpha = 0.10f) else Color.Transparent).padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(Modifier.width(40.dp), contentAlignment = Alignment.CenterStart) {
                            Box(Modifier.size(28.dp).clip(CircleShape).background(AppTheme.colors.cardAlt), contentAlignment = Alignment.Center) {
                                Text("${set.setNumber}", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurface)
                            }
                        }
                        NumberBox(set.weight, { vm.setWeight(index, si, it) }, "0", !set.done, KeyboardType.Decimal, Modifier.weight(1f).padding(horizontal = 4.dp))
                        NumberBox(set.reps, { vm.setReps(index, si, it) }, "${wex.repMax}", !set.done, KeyboardType.Number, Modifier.weight(1f).padding(horizontal = 4.dp))
                        Box(Modifier.width(52.dp), contentAlignment = Alignment.Center) {
                            Box(
                                Modifier.size(40.dp).clip(RoundedCornerShape(12.dp))
                                    .background(if (set.done) AppTheme.colors.success else AppTheme.colors.cardAlt)
                                    .clickable(enabled = !set.done && set.reps.isNotBlank()) { effortFor = si },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Filled.Check, contentDescription = if (set.done) "Set done" else "Log set",
                                    tint = if (set.done) Color.White else if (set.reps.isNotBlank()) MaterialTheme.colorScheme.onSurface else AppTheme.colors.textFaint)
                            }
                        }
                    }
                }
                TextButton(onClick = { vm.addSet(index) }) {
                    Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Text(" Add set", style = MaterialTheme.typography.labelLarge)
                }
            }
            Spacer(Modifier.height(8.dp))
        }

        // Rest bar
        if (rest > 0) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = Gutter, vertical = 6.dp).clip(RoundedCornerShape(20.dp))
                    .background(Brush.linearGradient(listOf(Brand.Blue, Brand.Purple))).padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text("REST", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.8f))
                    Text(Format.clock(rest), style = MaterialTheme.typography.headlineSmall, color = Color.White)
                }
                RestChip("−15") { vm.adjustRest(-15) }
                RestChip(if (restRunning) "Pause" else "Go") { vm.toggleRest() }
                RestChip("+15") { vm.adjustRest(15) }
                RestChip("Skip", filled = true) { vm.skipRest() }
            }
        }

        // Navigation
        Row(Modifier.fillMaxWidth().padding(horizontal = Gutter, vertical = 10.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            SecondaryButton("Previous", { vm.goTo(index - 1) }, Modifier.weight(1f), enabled = index > 0)
            if (index < state.exercises.size - 1) PrimaryButton("Next exercise", { vm.goTo(index + 1) }, Modifier.weight(1.4f))
            else PrimaryButton("Finish workout", { vm.finish() }, Modifier.weight(1.4f), color = AppTheme.colors.success)
        }
    }

    effortFor?.let { si ->
        AlertDialog(
            onDismissRequest = { effortFor = null },
            confirmButton = {},
            containerColor = AppTheme.colors.card,
            title = { Text("How did that set feel?", style = MaterialTheme.typography.titleLarge) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Effort.entries.forEach { e ->
                        ChoiceCard(e.label, false, {
                            vm.completeSet(index, si, e); effortFor = null
                            if (voiceOn) voice.say(com.gymtrainer.app.domain.CoachCue.SetLogged(si + 1, wex.sets.size))
                        }, subtitle = e.rir,
                            tint = if (e == Effort.PAIN) AppTheme.colors.danger else MaterialTheme.colorScheme.primary)
                    }
                }
            }
        )
    }

    if (showReplace) {
        AlertDialog(
            onDismissRequest = { showReplace = false },
            confirmButton = { TextButton(onClick = { showReplace = false }) { Text("Cancel") } },
            containerColor = AppTheme.colors.card,
            title = { Text("Swap exercise", style = MaterialTheme.typography.titleLarge) },
            text = {
                Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (alternatives.isEmpty()) Text("No equipment-compatible alternative found.", color = AppTheme.colors.textMuted)
                    alternatives.forEach { alt ->
                        Row(
                            Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(AppTheme.colors.cardAlt)
                                .clickable { vm.replaceExercise(index, alt.id); showReplace = false }.padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            ExerciseImage(alt, Modifier.width(64.dp).height(46.dp), animate = false, thumbnail = true, corner = 8.dp)
                            Column(Modifier.padding(start = 10.dp)) {
                                Text(alt.name, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)
                                Text(alt.equipment, style = MaterialTheme.typography.bodySmall, color = AppTheme.colors.textMuted)
                            }
                        }
                    }
                }
            }
        )
    }

    if (confirmFinish) {
        AlertDialog(
            onDismissRequest = { confirmFinish = false },
            containerColor = AppTheme.colors.card,
            title = { Text("Finish workout?") },
            text = { Text(if (doneSets > 0) "You've logged $doneSets sets. They're saved to your history." else "No sets logged yet — leave without saving?") },
            confirmButton = {
                TextButton(onClick = { confirmFinish = false; if (doneSets > 0) vm.finish() else onFinish() }) { Text(if (doneSets > 0) "Save & finish" else "Leave") }
            },
            dismissButton = { TextButton(onClick = { confirmFinish = false }) { Text("Keep training") } }
        )
    }
}

@Composable
private fun NumberBox(value: String, onChange: (String) -> Unit, hint: String, enabled: Boolean, keyboard: KeyboardType, modifier: Modifier) {
    val shape = RoundedCornerShape(12.dp)
    Box(
        modifier.height(44.dp).clip(shape)
            .background(if (enabled) AppTheme.colors.cardAlt else Color.Transparent)
            .border(1.dp, if (enabled) AppTheme.colors.border else Color.Transparent, shape),
        contentAlignment = Alignment.Center
    ) {
        if (value.isEmpty()) Text(hint, style = MaterialTheme.typography.titleMedium, color = AppTheme.colors.textFaint)
        BasicTextField(
            value = value, onValueChange = onChange, enabled = enabled, singleLine = true,
            textStyle = MaterialTheme.typography.titleMedium.copy(color = MaterialTheme.colorScheme.onSurface, textAlign = TextAlign.Center),
            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
            keyboardOptions = KeyboardOptions(keyboardType = keyboard),
            modifier = Modifier.fillMaxWidth().padding(horizontal = 6.dp)
        )
    }
}

@Composable
private fun RestChip(text: String, filled: Boolean = false, onClick: () -> Unit) {
    Box(
        Modifier.padding(start = 6.dp).height(36.dp).clip(RoundedCornerShape(50))
            .background(if (filled) Color.White else Color.White.copy(alpha = 0.18f)).clickable(onClick = onClick).padding(horizontal = 12.dp),
        contentAlignment = Alignment.Center
    ) { Text(text, style = MaterialTheme.typography.labelMedium, color = if (filled) Brand.Blue else Color.White) }
}

@Composable
private fun WorkoutSummaryView(summary: WorkoutSummary, units: com.gymtrainer.app.domain.Units, onFinish: () -> Unit) {
    Column(
        Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).statusBarsPadding().navigationBarsPadding().padding(Gutter),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(36.dp))
        Box(Modifier.size(120.dp).clip(CircleShape).background(Brush.linearGradient(listOf(Brand.Green, Brand.Teal))), contentAlignment = Alignment.Center) {
            Icon(Icons.Filled.EmojiEvents, contentDescription = null, tint = Color.White, modifier = Modifier.size(64.dp))
        }
        Spacer(Modifier.height(20.dp))
        Text("Session complete!", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.onBackground)
        Text("Every set was saved to your history.", style = MaterialTheme.typography.bodyLarge, color = AppTheme.colors.textMuted)
        Spacer(Modifier.height(24.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StatBlock("${summary.totalSets}", "sets", Modifier.weight(1f))
            StatBlock("${summary.exercisesDone}", "exercises", Modifier.weight(1f))
        }
        Spacer(Modifier.height(10.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StatBlock("${Format.trim(Format.weightValue(summary.totalVolume, units))} ${Format.weightUnit(units)}", "volume", Modifier.weight(1f), tint = Brand.Purple)
            StatBlock(Format.duration(summary.durationSec), "duration", Modifier.weight(1f), tint = Brand.Orange)
        }
        if (summary.deloadStarted) { Spacer(Modifier.height(10.dp)); FatigueNotice() }
        Spacer(Modifier.weight(1f))
        PrimaryButton("Done", onFinish, Modifier.fillMaxWidth())
    }
}
