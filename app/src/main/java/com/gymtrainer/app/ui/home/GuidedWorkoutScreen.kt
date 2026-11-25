package com.gymtrainer.app.ui.home

import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import android.speech.tts.TextToSpeech
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gymtrainer.app.ui.Format
import com.gymtrainer.app.ui.components.AppCard
import com.gymtrainer.app.ui.components.CircleIconButton
import com.gymtrainer.app.ui.components.ExerciseImage
import com.gymtrainer.app.ui.components.Gutter
import com.gymtrainer.app.ui.components.PrimaryButton
import com.gymtrainer.app.ui.components.ProgressRing
import com.gymtrainer.app.ui.components.StatBlock
import com.gymtrainer.app.ui.gymViewModel
import com.gymtrainer.app.ui.rememberSettings
import com.gymtrainer.app.ui.theme.AppTheme
import com.gymtrainer.app.ui.theme.Brand
import java.util.Locale

@Composable
fun GuidedWorkoutScreen(workoutKey: String, onClose: () -> Unit, onVideo: (String) -> Unit = {}) {
    val settings = rememberSettings()
    val vm = gymViewModel { GuidedViewModel(it, workoutKey, settings.homeRestSeconds, settings.countdownSeconds) }
    val s by vm.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var voiceOn by remember { mutableStateOf(settings.voiceCoach) }
    var confirmQuit by remember { mutableStateOf(false) }

    val view = LocalView.current
    DisposableEffect(Unit) {
        view.keepScreenOn = true
        onDispose { view.keepScreenOn = false }
    }

    val voice = com.gymtrainer.app.ui.rememberCoachVoice()
    LaunchedEffect(vm) {
        vm.speech.collect { cue -> if (voiceOn) voice.say(cue) }
    }

    BackHandler(enabled = s.phase != Phase.DONE) { confirmQuit = true }

    val w = s.workout
    if (w == null) {
        Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background), contentAlignment = Alignment.Center) {
            ProgressRing(0.3f, size = 48.dp)
        }
        return
    }
    if (s.phase == Phase.DONE) {
        DoneView(w.title, w.items.size, s.elapsed, s.calories, onClose)
        return
    }

    val item = w.items[s.index]
    val ex = s.exercises[item.exerciseId]
    val resting = s.phase == Phase.REST
    val bg = if (resting) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.background
    val onBg = if (resting) Color.White else MaterialTheme.colorScheme.onBackground

    Column(Modifier.fillMaxSize().background(bg).statusBarsPadding().navigationBarsPadding()) {
        // Top bar
        Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            CircleIconButton(Icons.Filled.Close, "Quit", { confirmQuit = true }, tint = onBg,
                background = if (resting) Color.White.copy(alpha = 0.18f) else AppTheme.colors.card)
            Row(Modifier.weight(1f).padding(horizontal = 12.dp), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                w.items.indices.forEach { i ->
                    val done = i < s.index || (i == s.index && resting)
                    val track = if (resting) Color.White.copy(alpha = 0.3f) else AppTheme.colors.border
                    val fill = if (resting) Color.White else MaterialTheme.colorScheme.primary
                    Box(Modifier.weight(1f).height(4.dp).clip(RoundedCornerShape(2.dp)).background(if (done) fill else if (i == s.index) fill.copy(alpha = 0.5f) else track))
                }
            }
            // Coach language: tap to switch English <-> বাংলা.
            val bangla = voice.requested == com.gymtrainer.app.domain.CoachLanguage.BANGLA
            Box(
                Modifier.padding(end = 8.dp).height(40.dp).clip(RoundedCornerShape(50))
                    .background(if (resting) Color.White.copy(alpha = 0.18f) else AppTheme.colors.card)
                    .clickable {
                        val next = if (bangla) com.gymtrainer.app.domain.CoachLanguage.ENGLISH else com.gymtrainer.app.domain.CoachLanguage.BANGLA
                        voice.setLanguage(next); settings.coachLanguage = next.name
                    }
                    .semantics { contentDescription = "Coach language" }
                    .padding(horizontal = 14.dp),
                contentAlignment = Alignment.Center
            ) { Text(if (bangla) "বাং" else "EN", style = MaterialTheme.typography.labelLarge, color = onBg) }
            CircleIconButton(if (voiceOn) Icons.Filled.VolumeUp else Icons.Filled.VolumeOff, "Voice coach",
                { voiceOn = !voiceOn; settings.voiceCoach = voiceOn; if (!voiceOn) voice.stop() }, tint = onBg,
                background = if (resting) Color.White.copy(alpha = 0.18f) else AppTheme.colors.card)
        }
        if (voice.banglaMissing && voiceOn) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = Gutter, vertical = 4.dp).clip(RoundedCornerShape(14.dp))
                    .background(Brand.Amber.copy(alpha = 0.16f))
                    .clickable { runCatching { context.startActivity(com.gymtrainer.app.ui.CoachVoice.installIntent()) } }
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("বাংলা ভয়েস এই ফোনে নেই, তাই আপাতত ইংরেজিতে বলছে। Tap to install the Bangla voice.",
                    style = MaterialTheme.typography.bodySmall, color = onBg)
            }
        }

        when (s.phase) {
            Phase.READY -> ReadyView(s, Modifier.weight(1f), vm::next)
            Phase.REST -> RestView(s, Modifier.weight(1f), onAdd = { vm.addRest(20) }, onSkip = vm::next)
            Phase.WORK -> {
                Column(Modifier.weight(1f).padding(horizontal = Gutter), horizontalAlignment = Alignment.CenterHorizontally) {
                    Spacer(Modifier.height(8.dp))
                    ExerciseImage(ex, Modifier.fillMaxWidth().aspectRatio(1.35f), corner = 28.dp)
                    Spacer(Modifier.height(18.dp))
                    Text("${s.index + 1} of ${w.items.size}", style = MaterialTheme.typography.labelMedium, color = AppTheme.colors.textMuted)
                    Text(ex?.name ?: item.exerciseId, style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onBackground, maxLines = 2)
                    Text(
                        "▶ How-to video", style = MaterialTheme.typography.labelMedium, color = Brand.Red,
                        modifier = Modifier.clip(RoundedCornerShape(50)).background(Brand.Red.copy(alpha = 0.1f))
                            .clickable { if (!s.paused) vm.togglePause(); onVideo(item.exerciseId) }
                            .padding(horizontal = 12.dp, vertical = 5.dp)
                    )
                    Spacer(Modifier.height(14.dp))
                    if (item.isTimed) {
                        ProgressRing(s.remaining / s.phaseTotal.toFloat(), size = 128.dp, stroke = 10.dp) {
                            Text(Format.clock(s.remaining.coerceAtLeast(0)), style = MaterialTheme.typography.headlineLarge, color = MaterialTheme.colorScheme.onBackground)
                        }
                    } else {
                        Text("x${item.reps}", style = MaterialTheme.typography.displayLarge, color = MaterialTheme.colorScheme.primary)
                        Text("reps", style = MaterialTheme.typography.bodyMedium, color = AppTheme.colors.textMuted)
                    }
                    ex?.execution?.split(" | ")?.firstOrNull { it.isNotBlank() }?.let { cue ->
                        Spacer(Modifier.height(14.dp))
                        Text(cue, style = MaterialTheme.typography.bodyMedium, color = AppTheme.colors.textMuted,
                            textAlign = TextAlign.Center, maxLines = 3)
                    }
                    Spacer(Modifier.weight(1f))
                    w.items.getOrNull(s.index + 1)?.let { nx ->
                        Text("Next: ${s.exercises[nx.exerciseId]?.name ?: nx.exerciseId}", style = MaterialTheme.typography.bodySmall, color = AppTheme.colors.textMuted)
                    }
                }
                Row(Modifier.fillMaxWidth().padding(horizontal = Gutter, vertical = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                    RoundControl(Icons.Filled.SkipPrevious, "Previous", enabled = s.index > 0, onClick = vm::previous)
                    Spacer(Modifier.width(14.dp))
                    if (item.isTimed) {
                        PrimaryButton(if (s.paused) "Resume" else "Pause", vm::togglePause, Modifier.weight(1f),
                            icon = if (s.paused) Icons.Filled.PlayArrow else Icons.Filled.Pause)
                    } else {
                        PrimaryButton("Done", vm::completeExercise, Modifier.weight(1f), icon = Icons.Filled.Check, color = AppTheme.colors.success)
                    }
                    Spacer(Modifier.width(14.dp))
                    RoundControl(Icons.Filled.SkipNext, "Skip", onClick = vm::next)
                }
            }
            Phase.DONE -> Unit
        }
    }

    if (confirmQuit) {
        val wasPaused = s.paused
        LaunchedEffect(Unit) { if (!wasPaused) vm.togglePause() }
        AlertDialog(
            onDismissRequest = { confirmQuit = false; if (!wasPaused) vm.togglePause() },
            title = { Text("Quit workout?") },
            text = { Text("Your progress in this session won't be saved.") },
            confirmButton = { TextButton(onClick = { confirmQuit = false; onClose() }) { Text("Quit") } },
            dismissButton = { TextButton(onClick = { confirmQuit = false; if (!wasPaused) vm.togglePause() }) { Text("Keep going") } }
        )
    }
}

@Composable
private fun RoundControl(icon: ImageVector, description: String, enabled: Boolean = true, onClick: () -> Unit) {
    Box(
        Modifier.size(56.dp).clip(CircleShape).background(AppTheme.colors.card).clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = description, tint = if (enabled) MaterialTheme.colorScheme.onSurface else AppTheme.colors.textFaint, modifier = Modifier.size(28.dp))
    }
}

@Composable
private fun ReadyView(s: GuidedState, modifier: Modifier, onStart: () -> Unit) {
    val w = s.workout ?: return
    val ex = s.exercises[w.items[s.index].exerciseId]
    Column(modifier.fillMaxWidth().padding(horizontal = Gutter), horizontalAlignment = Alignment.CenterHorizontally) {
        Spacer(Modifier.height(8.dp))
        ExerciseImage(ex, Modifier.fillMaxWidth().aspectRatio(1.35f), corner = 28.dp)
        Spacer(Modifier.height(20.dp))
        Text("GET READY", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
        Text(ex?.name ?: "", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onBackground, textAlign = TextAlign.Center)
        Spacer(Modifier.height(18.dp))
        ProgressRing(s.remaining / s.phaseTotal.toFloat(), size = 120.dp, stroke = 10.dp) {
            Text("${s.remaining}", style = MaterialTheme.typography.displayMedium, color = MaterialTheme.colorScheme.onBackground)
        }
        Spacer(Modifier.weight(1f))
        PrimaryButton("Start now", onStart, Modifier.fillMaxWidth().padding(bottom = 16.dp), icon = Icons.Filled.PlayArrow)
    }
}

@Composable
private fun RestView(s: GuidedState, modifier: Modifier, onAdd: () -> Unit, onSkip: () -> Unit) {
    val w = s.workout ?: return
    val nextItem = w.items.getOrNull(s.index + 1) ?: return
    val nextEx = s.exercises[nextItem.exerciseId]
    Column(modifier.fillMaxWidth().padding(horizontal = Gutter), horizontalAlignment = Alignment.CenterHorizontally) {
        Spacer(Modifier.weight(1f))
        ProgressRing(
            s.remaining / s.phaseTotal.toFloat(), size = 220.dp, stroke = 12.dp,
            color = Color.White, track = Color.White.copy(alpha = 0.2f)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("REST", style = MaterialTheme.typography.titleMedium, color = Color.White.copy(alpha = 0.85f))
                Text(Format.clock(s.remaining.coerceAtLeast(0)), style = MaterialTheme.typography.displayLarge, color = Color.White)
            }
        }
        Spacer(Modifier.height(24.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            WhitePill("+20s", filled = false, onClick = onAdd)
            WhitePill("Skip", filled = true, onClick = onSkip)
        }
        Spacer(Modifier.weight(1f))
        AppCard(Modifier.padding(bottom = 16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                ExerciseImage(nextEx, Modifier.width(110.dp).aspectRatio(1.4f), corner = 14.dp, animate = false)
                Column(Modifier.weight(1f).padding(start = 14.dp)) {
                    Text("NEXT ${s.index + 2}/${w.items.size}", style = MaterialTheme.typography.labelSmall, color = AppTheme.colors.textMuted)
                    Text(nextEx?.name ?: nextItem.exerciseId, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface, maxLines = 2)
                    Text(nextItem.label(), style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
                }
            }
        }
    }
}

@Composable
private fun WhitePill(text: String, filled: Boolean, onClick: () -> Unit) {
    Box(
        Modifier.height(48.dp).clip(RoundedCornerShape(50))
            .background(if (filled) Color.White else Color.White.copy(alpha = 0.18f))
            .clickable(onClick = onClick).padding(horizontal = 28.dp),
        contentAlignment = Alignment.Center
    ) { Text(text, style = MaterialTheme.typography.labelLarge, color = if (filled) Brand.Blue else Color.White) }
}

@Composable
private fun DoneView(title: String, exercises: Int, seconds: Int, kcal: Int, onClose: () -> Unit) {
    Column(
        Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).statusBarsPadding().navigationBarsPadding().padding(Gutter),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(36.dp))
        Box(Modifier.size(120.dp).clip(CircleShape).background(Brush.linearGradient(listOf(Brand.Amber, Brand.Orange))), contentAlignment = Alignment.Center) {
            Icon(Icons.Filled.EmojiEvents, contentDescription = null, tint = Color.White, modifier = Modifier.size(64.dp))
        }
        Spacer(Modifier.height(20.dp))
        Text("Workout complete! 🎉", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.onBackground, textAlign = TextAlign.Center)
        Text(title, style = MaterialTheme.typography.bodyLarge, color = AppTheme.colors.textMuted, textAlign = TextAlign.Center)
        Spacer(Modifier.height(24.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StatBlock("$exercises", "exercises", Modifier.weight(1f))
            StatBlock(Format.duration(seconds), "time", Modifier.weight(1f))
            StatBlock("$kcal", "kcal", Modifier.weight(1f), tint = Brand.Orange)
        }
        Spacer(Modifier.height(16.dp))
        Text("Saved to your progress. Consistency beats intensity — see you next session!", style = MaterialTheme.typography.bodyMedium,
            color = AppTheme.colors.textMuted, textAlign = TextAlign.Center)
        Spacer(Modifier.weight(1f))
        PrimaryButton("Finish", onClose, Modifier.fillMaxWidth())
    }
}

@Suppress("unused")
private val unusedPad = PaddingValues(0.dp)
