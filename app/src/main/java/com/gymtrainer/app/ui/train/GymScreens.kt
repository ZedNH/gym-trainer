package com.gymtrainer.app.ui.train

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Autorenew
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gymtrainer.app.data.TEMPLATE_PLAN_ID
import com.gymtrainer.app.domain.Difficulty
import com.gymtrainer.app.domain.GymCategory
import com.gymtrainer.app.domain.GymWorkout
import com.gymtrainer.app.domain.GymWorkouts
import com.gymtrainer.app.ui.components.AppCard
import com.gymtrainer.app.ui.components.DetailTopBar
import com.gymtrainer.app.ui.components.EmptyState
import com.gymtrainer.app.ui.components.FilterPill
import com.gymtrainer.app.ui.components.Gutter
import com.gymtrainer.app.ui.components.HeroCard
import com.gymtrainer.app.ui.components.IconBadge
import com.gymtrainer.app.ui.components.LevelBadge
import com.gymtrainer.app.ui.components.PillButton
import com.gymtrainer.app.ui.components.SecondaryButton
import com.gymtrainer.app.ui.components.SectionHeader
import com.gymtrainer.app.ui.components.Tag
import com.gymtrainer.app.ui.components.Visuals
import com.gymtrainer.app.ui.components.levelColor
import com.gymtrainer.app.ui.gymViewModel
import com.gymtrainer.app.ui.rememberRepository
import com.gymtrainer.app.ui.theme.AppTheme

// ---------------- Gym category (muscle focus) ----------------

@Composable
fun GymCategoryScreen(categoryName: String, onBack: () -> Unit, onOpen: (Long) -> Unit) {
    val cat = remember(categoryName) { runCatching { GymCategory.valueOf(categoryName) }.getOrDefault(GymCategory.FULL_BODY) }
    val (icon, tint) = Visuals.gymCategory(cat)
    val repo = rememberRepository()
    val ids by produceState(emptyMap<String, Long>()) { value = repo.days(TEMPLATE_PLAN_ID).associate { it.name to it.id } }
    val names by produceState(emptyMap<String, String>()) { value = repo.exerciseMap().mapValues { it.value.name } }
    var levelName by rememberSaveable { mutableStateOf<String?>(null) }
    val all = GymWorkouts.byCategory(cat)
    val workouts = all.filter { levelName == null || it.level.name == levelName }

    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        DetailTopBar(cat.label, onBack)
        LazyColumn(contentPadding = PaddingValues(bottom = 28.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item {
                HeroCard(Visuals.gradient(tint), Modifier.padding(horizontal = Gutter).fillMaxWidth(), icon) {
                    Column {
                        Text("GYM · ${cat.label.uppercase()}", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.8f))
                        Text(cat.label, style = MaterialTheme.typography.headlineMedium, color = Color.White)
                        Text(cat.blurb, style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha = 0.85f))
                        Spacer(Modifier.height(18.dp))
                        Text("${all.size} workouts · " + all.map { it.level }.distinct().joinToString(" · ") { it.label },
                            style = MaterialTheme.typography.labelMedium, color = Color.White)
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
                GymWorkoutCard(w, names, Modifier.padding(horizontal = Gutter)) { ids[w.title]?.let(onOpen) }
            }
        }
    }
}

@Composable
private fun GymWorkoutCard(w: GymWorkout, names: Map<String, String>, modifier: Modifier, onClick: () -> Unit) {
    AppCard(modifier, onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconBadge(Icons.Filled.FitnessCenter, levelColor(w.level), 48.dp)
            Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                Text(w.title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(w.subtitle, style = MaterialTheme.typography.bodySmall, color = AppTheme.colors.textMuted,
                    maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            LevelBadge(w.level)
        }
        Spacer(Modifier.height(12.dp))
        Text(w.items.joinToString(" · ") { names[it.id] ?: it.id.replace('_', ' ') }, style = MaterialTheme.typography.bodySmall,
            color = AppTheme.colors.textMuted, maxLines = 2, overflow = TextOverflow.Ellipsis)
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Tag("~${w.minutes} min", MaterialTheme.colorScheme.primary)
            Tag("${w.items.size} exercises", AppTheme.colors.textMuted)
            Tag("${w.sets} sets", AppTheme.colors.textMuted)
        }
    }
}

// ---------------- My gym plan ----------------

@Composable
fun GymPlanScreen(onBack: () -> Unit, onStart: (Long) -> Unit, onPrograms: () -> Unit, onVolume: () -> Unit = {}) {
    val vm = gymViewModel { TrainViewModel(it) }
    val state by vm.state.collectAsStateWithLifecycle()
    var rebuilding by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        DetailTopBar("My gym plan", onBack)
        if (!state.loaded) return@Column
        LazyColumn(contentPadding = PaddingValues(bottom = 28.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (!state.hasPlan) {
                item { NoGymPlanHero(Modifier.padding(horizontal = Gutter)) { vm.regenerate() } }
                return@LazyColumn
            }
            item { GymPlanHero(state, Modifier.padding(horizontal = Gutter), onOpen = null, onStart = { state.next?.let { onStart(it.id) } }) }
            item {
                val week = if (state.days.isEmpty()) 1 else state.sessionsDone / state.days.size + 1
                Row(Modifier.padding(horizontal = Gutter), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Fact("${state.sessionsDone}", "sessions done", Modifier.weight(1f))
                    Fact(if (state.weeks > 0) "${week.coerceAtMost(state.weeks)}/${state.weeks}" else "$week", "week", Modifier.weight(1f))
                    Fact("${state.days.size}", "days / week", Modifier.weight(1f))
                }
            }

            item { DeloadCard(state, Modifier.padding(horizontal = Gutter), onStart = vm::startDeload, onSkip = vm::skipDeload) }
            item { VolumeSummaryCard(state, Modifier.padding(horizontal = Gutter), onVolume) }

            item { SectionHeader("Schedule", Modifier.padding(top = 4.dp)) }
            itemsIndexed(state.days, key = { _, d -> "pd_${d.id}" }) { i, d ->
                val isNext = i == state.nextIndex
                AppCard(Modifier.padding(horizontal = Gutter), onClick = { onStart(d.id) }) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("DAY ${i + 1}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                                if (isNext) { Spacer(Modifier.width(8.dp)); Tag("Up next", GymHeroColors.last()) }
                            }
                            Text(d.name, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                            Text("${d.exercises.size} exercises · ${d.sets} sets · ${d.focus}", style = MaterialTheme.typography.bodySmall,
                                color = AppTheme.colors.textMuted, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                        PillButton("Start", { onStart(d.id) }, icon = Icons.Filled.PlayArrow,
                            color = if (isNext) GymHeroColors.last() else MaterialTheme.colorScheme.primary)
                    }
                    Spacer(Modifier.height(10.dp))
                    d.exercises.forEach { ex ->
                        Row(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                            Text(ex.name, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(ex.scheme, style = MaterialTheme.typography.labelMedium, color = AppTheme.colors.textMuted)
                        }
                    }
                }
            }

            if (state.notes.isNotBlank()) {
                item {
                    AppCard(Modifier.padding(horizontal = Gutter)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconBadge(Icons.Filled.Lightbulb, com.gymtrainer.app.ui.theme.Brand.Amber, 36.dp)
                            Spacer(Modifier.width(10.dp))
                            Text("Coach notes", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)
                        }
                        Spacer(Modifier.height(8.dp))
                        Text(state.notes, style = MaterialTheme.typography.bodyMedium, color = AppTheme.colors.textMuted)
                    }
                }
            }

            item {
                Column(Modifier.padding(horizontal = Gutter).padding(top = 4.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    SecondaryButton("Change program", onPrograms, Modifier.fillMaxWidth(), icon = Icons.Filled.SwapHoriz)
                    SecondaryButton(if (rebuilding) "Rebuilding…" else "Rebuild from my profile",
                        { rebuilding = true; vm.regenerate { rebuilding = false } },
                        Modifier.fillMaxWidth(), icon = Icons.Filled.Autorenew, enabled = !rebuilding)
                }
            }
        }
    }
}

@Composable
private fun DeloadCard(state: TrainState, modifier: Modifier, onStart: () -> Unit, onSkip: () -> Unit) {
    val d = state.deload
    AppCard(modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconBadge(Icons.Filled.SelfImprovement, com.gymtrainer.app.ui.theme.Brand.Teal, 40.dp)
            Column(Modifier.weight(1f).padding(start = 12.dp)) {
                Text(if (d.active) "Deload week · ${d.kind?.label ?: ""}" else "Recovery", style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface)
                Text(
                    if (d.active) "${d.sessionsLeft} lighter ${if (d.sessionsLeft == 1) "session" else "sessions"} left. ${d.reason}"
                    else "Next planned deload in ${d.nextInSessions ?: 0} sessions. It starts by itself if your sets keep feeling too heavy.",
                    style = MaterialTheme.typography.bodySmall, color = AppTheme.colors.textMuted
                )
            }
        }
        Spacer(Modifier.height(10.dp))
        if (d.active) SecondaryButton("Skip deload", onSkip, Modifier.fillMaxWidth().height(46.dp))
        else SecondaryButton("Feeling beat up? Deload now", onStart, Modifier.fillMaxWidth().height(46.dp), icon = Icons.Filled.SelfImprovement)
    }
}
