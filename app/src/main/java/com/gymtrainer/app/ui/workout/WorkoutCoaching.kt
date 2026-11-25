package com.gymtrainer.app.ui.workout

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.gymtrainer.app.domain.DeloadStatus
import com.gymtrainer.app.domain.TargetAction
import com.gymtrainer.app.domain.Units
import com.gymtrainer.app.domain.WarmUp
import com.gymtrainer.app.ui.Format
import com.gymtrainer.app.ui.components.AppCard
import com.gymtrainer.app.ui.components.IconBadge
import com.gymtrainer.app.ui.theme.AppTheme
import com.gymtrainer.app.ui.theme.Brand

fun actionVisual(a: TargetAction): Pair<ImageVector, Color> = when (a) {
    TargetAction.START -> Icons.Filled.Flag to Brand.Purple
    TargetAction.INCREASE -> Icons.Filled.TrendingUp to Brand.Green
    TargetAction.BUILD_REPS -> Icons.Filled.Repeat to Brand.Blue
    TargetAction.REPEAT -> Icons.Filled.Repeat to Brand.Blue
    TargetAction.DECREASE -> Icons.Filled.TrendingDown to Brand.Amber
    TargetAction.DELOAD -> Icons.Filled.SelfImprovement to Brand.Teal
}

/** "Today's target" card: what to lift, why, and what you did last time. */
@Composable
fun TargetCard(wex: WorkoutExercise, units: Units) {
    val t = wex.target ?: return
    val (icon, tint) = actionVisual(t.action)
    val unit = Format.weightUnit(units)
    AppCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconBadge(icon, tint, 44.dp)
            Column(Modifier.weight(1f).padding(start = 12.dp)) {
                Text("TODAY'S TARGET · ${t.action.label.uppercase()}", style = MaterialTheme.typography.labelSmall, color = tint)
                Text(
                    when {
                        t.weightKg != null -> "${Format.trim(Format.weightValue(t.weightKg, units))} $unit × ${t.reps}"
                        t.action == TargetAction.START -> "${wex.repMin}–${wex.repMax} reps"
                        else -> "${t.reps} reps"
                    },
                    style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(t.reason, style = MaterialTheme.typography.bodySmall, color = AppTheme.colors.textMuted)
        if (wex.lastSets.isNotEmpty()) {
            Spacer(Modifier.height(6.dp))
            Text("Last time: " + wex.lastSets.joinToString(" · ") { if (it.contains('×')) it.replace("×", " $unit × ") else it },
                style = MaterialTheme.typography.labelMedium, color = Brand.Purple)
        }
    }
}

/** Warm-up ladder for the working weight in the first set, with plates per side for barbell lifts. */
@Composable
fun WarmUpCard(wex: WorkoutExercise, units: Units, exIndex: Int) {
    val work = wex.sets.firstOrNull()?.weight?.toDoubleOrNull() ?: return
    val imperial = units == Units.IMPERIAL
    val plan = WarmUp.plan(work, wex.exercise.equipment, wex.exercise.isCompound, imperial)
    if (plan.isEmpty()) return
    val unit = Format.weightUnit(units)
    val done = remember(exIndex, wex.exercise.id) { mutableStateListOf<Int>() }
    var open by remember(exIndex, wex.exercise.id) { mutableStateOf(wex.sets.none { it.done }) }

    AppCard {
        Row(Modifier.fillMaxWidth().clickable { open = !open }, verticalAlignment = Alignment.CenterVertically) {
            IconBadge(Icons.Filled.LocalFireDepartment, Brand.Orange, 36.dp)
            Column(Modifier.weight(1f).padding(start = 12.dp)) {
                Text("Warm-up", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)
                Text("${done.size}/${plan.size} done · not logged", style = MaterialTheme.typography.bodySmall, color = AppTheme.colors.textMuted)
            }
            Icon(if (open) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore, contentDescription = if (open) "Hide warm-up" else "Show warm-up",
                tint = AppTheme.colors.textMuted)
        }
        if (open) {
            Spacer(Modifier.height(8.dp))
            plan.forEachIndexed { i, w ->
                val isDone = i in done
                Row(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).clickable { if (isDone) done.remove(i) else done.add(i) }
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        Modifier.size(26.dp).clip(CircleShape).background(if (isDone) AppTheme.colors.success else AppTheme.colors.cardAlt),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isDone) Icon(Icons.Filled.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                        else Text("${i + 1}", style = MaterialTheme.typography.labelSmall, color = AppTheme.colors.textMuted)
                    }
                    Text(
                        (if (w.isBar) "Empty bar" else "${Format.trim(w.weight)} $unit") + " × ${w.reps}",
                        style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f).padding(start = 12.dp)
                    )
                    if (w.perSide.isNotEmpty()) Text(plateText(w.perSide), style = MaterialTheme.typography.labelMedium, color = AppTheme.colors.textMuted)
                }
            }
            if (wex.exercise.equipment == "Barbell") {
                val side = WarmUp.plates(work, imperial)
                if (side.isNotEmpty()) {
                    Spacer(Modifier.height(4.dp))
                    Text("Working ${Format.trim(work)} $unit: ${plateText(side)} (${Format.trim(WarmUp.bar(imperial))} $unit bar)",
                        style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                }
            }
        }
    }
}

private fun plateText(side: List<Double>) =
    side.joinToString(" + ") { if (it % 1.0 == 0.0) it.toInt().toString() else it.toBigDecimal().stripTrailingZeros().toPlainString() } + " / side"

@Composable
fun DeloadBanner(status: DeloadStatus) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp))
            .background(Brush.linearGradient(listOf(Brand.Teal, Brand.Blue))).padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Filled.SelfImprovement, contentDescription = null, tint = Color.White, modifier = Modifier.size(32.dp))
        Spacer(Modifier.width(12.dp))
        Column {
            Text("DELOAD WEEK · ${status.kind?.label?.uppercase() ?: ""}", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.85f))
            Text("Lighter on purpose", style = MaterialTheme.typography.titleMedium, color = Color.White)
            Text("Fewer sets, ~10% less weight. ${status.reason}", style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.9f))
        }
    }
}

@Composable
fun FatigueNotice() {
    AppCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconBadge(Icons.Filled.SelfImprovement, Brand.Teal, 40.dp)
            Column(Modifier.padding(start = 12.dp)) {
                Text("Deload week scheduled", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)
                Text("Your recent sets felt too heavy. Next week is lighter so you come back stronger.",
                    style = MaterialTheme.typography.bodySmall, color = AppTheme.colors.textMuted)
            }
        }
    }
}
