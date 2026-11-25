package com.gymtrainer.app.ui.train

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.gymtrainer.app.domain.Difficulty
import com.gymtrainer.app.domain.GymCategory
import com.gymtrainer.app.domain.HomePrograms
import com.gymtrainer.app.domain.Volume
import com.gymtrainer.app.domain.VolumeMuscle
import com.gymtrainer.app.domain.VolumeStatus
import com.gymtrainer.app.ui.components.AppCard
import com.gymtrainer.app.ui.components.DetailTopBar
import com.gymtrainer.app.ui.components.Gutter
import com.gymtrainer.app.ui.components.HeroCard
import com.gymtrainer.app.ui.components.IconBadge
import com.gymtrainer.app.ui.components.SegmentedTabs
import com.gymtrainer.app.ui.components.Tag
import com.gymtrainer.app.ui.rememberRepository
import com.gymtrainer.app.ui.theme.AppTheme
import com.gymtrainer.app.ui.theme.Brand

fun volumeColor(s: VolumeStatus): Color = when (s) {
    VolumeStatus.LOW -> Brand.Amber
    VolumeStatus.OK -> Brand.Green
    VolumeStatus.HIGH -> Brand.Red
}

private fun sets(v: Double) = if (v % 1.0 == 0.0) v.toInt().toString() else String.format("%.1f", v)

/** Horizontal bar with the target band shaded. */
@Composable
fun VolumeBar(value: Double, range: IntRange, modifier: Modifier = Modifier, height: Int = 10) {
    val max = range.last * 1.5
    val color = volumeColor(Volume.status(value, range))
    BoxWithConstraints(modifier.fillMaxWidth().height(height.dp).clip(RoundedCornerShape(50)).background(AppTheme.colors.border)) {
        val w = maxWidth
        Box(Modifier.offset(x = w * (range.first / max).toFloat()).width(w * ((range.last - range.first) / max).toFloat())
            .fillMaxHeight().background(Brand.Green.copy(alpha = 0.22f)))
        Box(Modifier.width(w * (value / max).coerceIn(0.0, 1.0).toFloat()).fillMaxHeight().clip(RoundedCornerShape(50)).background(color))
    }
}

@Composable
fun VolumeScreen(onBack: () -> Unit, onCategory: (GymCategory) -> Unit) {
    val repo = rememberRepository()
    var week by rememberSaveable { mutableIntStateOf(0) }
    val level by produceState(Difficulty.BEGINNER) { value = repo.getProfile()?.let { HomePrograms.personalSpec(it).level } ?: Difficulty.BEGINNER }
    val volume by produceState(emptyMap<VolumeMuscle, Double>(), week) { value = repo.weeklyVolume(weekStart(week), weekStart(week - 1)) }
    val range = Volume.target(level)
    val onTarget = VolumeMuscle.entries.count { Volume.status(volume[it] ?: 0.0, range) == VolumeStatus.OK }
    val low = VolumeMuscle.entries.filter { Volume.status(volume[it] ?: 0.0, range) == VolumeStatus.LOW }

    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        DetailTopBar("Weekly sets per muscle", onBack)
        LazyColumn(contentPadding = PaddingValues(bottom = 28.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item {
                SegmentedTabs(listOf(0 to "This week", 1 to "Last week"), week, { week = it }, Modifier.padding(horizontal = Gutter))
            }
            item {
                HeroCard(listOf(Brand.Purple, Brand.Blue), Modifier.padding(horizontal = Gutter).fillMaxWidth(), Icons.Filled.BarChart) {
                    Column {
                        Text("${level.label.uppercase()} TARGET · ${range.first}–${range.last} SETS / MUSCLE", style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.85f))
                        Text("$onTarget of ${VolumeMuscle.entries.size} on target", style = MaterialTheme.typography.headlineSmall, color = Color.White)
                        Text(
                            if (low.isEmpty()) "Every muscle got enough work. Nice balance."
                            else "Needs more: " + low.joinToString(", ") { it.label },
                            style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.9f)
                        )
                    }
                }
            }
            item {
                Text("A hard set = a working set taken close to failure. Main muscle counts 1, helper muscles ½. Tap a muscle for workouts.",
                    style = MaterialTheme.typography.bodySmall, color = AppTheme.colors.textMuted, modifier = Modifier.padding(horizontal = Gutter))
            }
            items(VolumeMuscle.entries, key = { it.name }) { m ->
                val v = volume[m] ?: 0.0
                val st = Volume.status(v, range)
                AppCard(Modifier.padding(horizontal = Gutter), onClick = { onCategory(m.category) }) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(m.label, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
                        Text("${sets(v)} ${if (v == 1.0) "set" else "sets"}",style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)
                        Spacer(Modifier.width(8.dp))
                        Tag(st.label, volumeColor(st))
                    }
                    Spacer(Modifier.height(10.dp))
                    VolumeBar(v, range)
                }
            }
        }
    }
}

/** Compact weekly-volume card for the Gym tab. */
@Composable
fun VolumeSummaryCard(state: TrainState, modifier: Modifier = Modifier, onOpen: () -> Unit) {
    val range = state.volumeRange
    val total = state.volume.values.sum()
    AppCard(modifier, onClick = onOpen) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconBadge(Icons.Filled.BarChart, Brand.Purple, 40.dp)
            Column(Modifier.weight(1f).padding(start = 12.dp)) {
                Text("This week's sets per muscle", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)
                Text(
                    if (total == 0.0) "Log a gym session to see your weekly balance."
                    else if (state.lowMuscles.isEmpty()) "Every muscle is on target."
                    else "Needs more: " + state.lowMuscles.take(4).joinToString(", ") { it.label } + if (state.lowMuscles.size > 4) "…" else "",
                    style = MaterialTheme.typography.bodySmall, color = AppTheme.colors.textMuted, maxLines = 2
                )
            }
        }
        if (total > 0) {
            Spacer(Modifier.height(12.dp))
            listOf(
                VolumeMuscle.CHEST, VolumeMuscle.BACK, VolumeMuscle.SHOULDERS, VolumeMuscle.QUADS, VolumeMuscle.HAMSTRINGS, VolumeMuscle.GLUTES
            ).forEach { m ->
                Row(Modifier.padding(vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(m.label, style = MaterialTheme.typography.labelMedium, color = AppTheme.colors.textMuted, modifier = Modifier.width(92.dp))
                    VolumeBar(state.volume[m] ?: 0.0, range, Modifier.weight(1f), height = 8)
                    Text(sets(state.volume[m] ?: 0.0), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.width(36.dp).padding(start = 8.dp))
                }
            }
        }
    }
}
