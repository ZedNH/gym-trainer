package com.gymtrainer.app.ui.exercises

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.gymtrainer.app.data.ExerciseEntity
import com.gymtrainer.app.domain.Difficulty
import com.gymtrainer.app.ui.components.AppCard
import com.gymtrainer.app.ui.components.CircleIconButton
import com.gymtrainer.app.ui.components.ExerciseImage
import com.gymtrainer.app.ui.components.Gutter
import com.gymtrainer.app.ui.components.LevelBadge
import com.gymtrainer.app.ui.components.SecondaryButton
import com.gymtrainer.app.ui.components.Tag
import com.gymtrainer.app.ui.rememberRepository
import com.gymtrainer.app.ui.theme.AppTheme
import com.gymtrainer.app.ui.theme.Brand

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ExerciseDetailScreen(exerciseId: String, onBack: () -> Unit, onVideos: () -> Unit = {}) {
    val repo = rememberRepository()
    val videos = com.gymtrainer.app.ui.rememberVideos()
    val exercise by produceState<ExerciseEntity?>(initialValue = null, exerciseId) { value = repo.exercise(exerciseId) }
    val verified = remember(exerciseId) { videos.verified(exerciseId) }
    val sets by repo.observeSetsForExercise(exerciseId).collectAsState(initial = emptyList())
    val units by produceState(com.gymtrainer.app.domain.Units.METRIC) {
        value = com.gymtrainer.app.domain.Units.from(repo.getProfile()?.units)
    }

    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).verticalScroll(rememberScrollState())) {
        val ex = exercise
        Box(Modifier.fillMaxWidth().background(if ((ex?.imageCount ?: 0) > 0) Color.White else AppTheme.colors.cardAlt)) {
            ExerciseImage(ex, Modifier.fillMaxWidth().aspectRatio(1.35f).statusBarsPadding(), corner = 0.dp)
            Box(Modifier.statusBarsPadding().padding(12.dp)) {
                CircleIconButton(Icons.AutoMirrored.Filled.ArrowBack, "Back", onBack)
            }
        }
        if (ex == null) return@Column

        Column(Modifier.padding(horizontal = Gutter), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Spacer(Modifier.height(4.dp))
            Text(ex.name, style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.onBackground)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                LevelBadge(Difficulty.from(ex.difficulty))
                Tag(ex.muscleGroup.lowercase().replaceFirstChar { it.uppercase() })
                Tag(ex.equipment, AppTheme.colors.textMuted)
                Tag(ex.category.replaceFirstChar { it.uppercase() }, AppTheme.colors.textMuted)
                if (ex.isCompound) Tag("Compound", Brand.Purple)
            }
            // Video tutorials
            AppCard(onClick = onVideos, padding = androidx.compose.foundation.layout.PaddingValues(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (verified.isNotEmpty()) {
                        com.gymtrainer.app.ui.videos.VideoThumbnail(verified.first().videoId,
                            Modifier.width(118.dp).aspectRatio(16f / 9f).clip(androidx.compose.foundation.shape.RoundedCornerShape(12.dp)), badge = 28.dp)
                    } else {
                        com.gymtrainer.app.ui.components.IconBadge(Icons.Filled.PlayCircle, Brand.Red, 52.dp)
                    }
                    Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                        Text("Video tutorials", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                        Text(if (verified.isNotEmpty()) "${verified.size} verified · watch in the app" else "Find, save and watch in the app",
                            style = MaterialTheme.typography.bodySmall, color = AppTheme.colors.textMuted)
                    }
                    Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = AppTheme.colors.textFaint)
                }
            }

            AppCard {
                Row {
                    Column(Modifier.weight(1f)) {
                        Text("Primary", style = MaterialTheme.typography.labelSmall, color = AppTheme.colors.textMuted)
                        Text(ex.primaryMuscles, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)
                    }
                    if (ex.secondaryMuscles.isNotBlank()) Column(Modifier.weight(1f)) {
                        Text("Secondary", style = MaterialTheme.typography.labelSmall, color = AppTheme.colors.textMuted)
                        Text(ex.secondaryMuscles, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)
                    }
                }
                Spacer(Modifier.height(10.dp))
                Text(
                    if (ex.timed) "Suggested: hold ~${ex.defaultSeconds}s · rest ~${ex.restSeconds}s"
                    else "Suggested: ${ex.repMin}–${ex.repMax} reps · rest ~${ex.restSeconds}s",
                    style = MaterialTheme.typography.bodySmall, color = AppTheme.colors.textMuted
                )
            }

            if (sets.isNotEmpty()) {
                Section("Your history") {
                    // Best estimated 1RM per session (Epley).
                    val perSession = sets.groupBy { it.sessionId }.values.map { s ->
                        s.maxOf { it.weightKg * (1 + it.reps / 30.0) }
                    }.filter { it > 0 }
                    val best = sets.maxByOrNull { it.weightKg * (1 + it.reps / 30.0) }
                    if (best != null && best.weightKg > 0) {
                        Text("Best set: ${com.gymtrainer.app.ui.Format.weight(best.weightKg, units)} × ${best.reps}",
                            style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)
                    }
                    Text("${sets.size} sets in ${sets.map { it.sessionId }.distinct().size} sessions",
                        style = MaterialTheme.typography.bodySmall, color = AppTheme.colors.textMuted)
                    com.gymtrainer.app.ui.components.LineChart(perSession.map {
                        com.gymtrainer.app.ui.Format.weightValue(it, units).toFloat()
                    })
                    Text("Estimated 1-rep max per session", style = MaterialTheme.typography.labelSmall, color = AppTheme.colors.textFaint)
                }
            }
            Section("How to do it") {
                val steps = (listOf(ex.setup) + ex.execution.split(" | ")).filter { it.isNotBlank() }.distinct()
                steps.forEachIndexed { i, step ->
                    Row(Modifier.padding(vertical = 6.dp)) {
                        Box(Modifier.size(26.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer), contentAlignment = Alignment.Center) {
                            Text("${i + 1}", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                        }
                        Text(step, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.padding(start = 12.dp))
                    }
                }
            }
            Section("Breathing") {
                Text(ex.breathing, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
            }
            Section("Common mistakes") {
                ex.commonMistakes.split(" | ").filter { it.isNotBlank() }.forEach { m ->
                    Row(Modifier.padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(8.dp).clip(CircleShape).background(AppTheme.colors.danger))
                        Text(m, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.padding(start = 12.dp))
                    }
                }
            }
            Text("Photos: free-exercise-db (public domain). Videos open in YouTube; nothing is downloaded or re-hosted.",
                style = MaterialTheme.typography.bodySmall, color = AppTheme.colors.textFaint)
            Spacer(Modifier.height(28.dp))
        }
    }
}

@Composable
private fun Section(title: String, content: @Composable () -> Unit) {
    AppCard {
        Text(title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
        Spacer(Modifier.height(8.dp))
        content()
    }
}
