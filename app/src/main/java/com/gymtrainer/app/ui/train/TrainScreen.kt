package com.gymtrainer.app.ui.train

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.material.icons.filled.MonitorWeight
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.ListAlt
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
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
import androidx.navigation.NavController
import com.gymtrainer.app.domain.Difficulty
import com.gymtrainer.app.domain.GymCategory
import com.gymtrainer.app.domain.GymPrograms
import com.gymtrainer.app.domain.GymWorkouts
import com.gymtrainer.app.domain.HomePrograms
import com.gymtrainer.app.domain.PlanGoal
import com.gymtrainer.app.domain.PlanSpec
import com.gymtrainer.app.domain.WorkoutCategory
import com.gymtrainer.app.ui.components.AppCard
import com.gymtrainer.app.ui.components.EmptyState
import com.gymtrainer.app.ui.components.FilterPill
import com.gymtrainer.app.ui.components.Gutter
import com.gymtrainer.app.ui.components.HeroCard
import com.gymtrainer.app.ui.components.IconBadge
import com.gymtrainer.app.ui.components.LevelBadge
import com.gymtrainer.app.ui.components.LinearMeter
import com.gymtrainer.app.ui.components.ListRow
import com.gymtrainer.app.ui.components.SectionHeader
import com.gymtrainer.app.ui.components.SegmentedTabs
import com.gymtrainer.app.ui.components.TabHeader
import com.gymtrainer.app.ui.components.Tag
import com.gymtrainer.app.ui.components.Visuals
import com.gymtrainer.app.ui.components.levelColor
import com.gymtrainer.app.ui.gymViewModel
import com.gymtrainer.app.ui.home.programCard
import com.gymtrainer.app.ui.navigation.Routes
import com.gymtrainer.app.ui.rememberRepository
import com.gymtrainer.app.ui.theme.AppTheme
import com.gymtrainer.app.ui.theme.Brand

private enum class Mode { HOME, GYM }

@Composable
fun TrainScreen(contentPadding: PaddingValues, nav: NavController) {
    val vm = gymViewModel { TrainViewModel(it) }
    val state by vm.state.collectAsStateWithLifecycle()
    val repo = rememberRepository()
    val profile by produceState<com.gymtrainer.app.data.UserProfileEntity?>(null) { value = repo.getProfile() }
    val progress by repo.observeAllChallengeProgress().collectAsState(initial = emptyList())

    var mode by rememberSaveable { mutableStateOf(Mode.HOME) }
    var levelName by rememberSaveable { mutableStateOf<String?>(null) }
    val defaultLevel = profile?.let { HomePrograms.personalSpec(it).level } ?: Difficulty.BEGINNER
    val level = levelName?.let { Difficulty.valueOf(it) } ?: defaultLevel
    var toDelete by remember { mutableStateOf<DayView?>(null) }

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = contentPadding.calculateBottomPadding() + 28.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            TabHeader("Workouts", hint = "Plans, programs and single workouts, at home or in the gym") {
                com.gymtrainer.app.ui.components.CircleIconButton(androidx.compose.material.icons.Icons.Filled.Search, "Exercise library",
                    { nav.navigate(Routes.EXERCISES) })
            }
        }
        item {
            SegmentedTabs(listOf(Mode.HOME to "Home", Mode.GYM to "Gym"), mode, { mode = it }, Modifier.padding(horizontal = Gutter))
        }
        item {
            LazyRow(contentPadding = PaddingValues(horizontal = Gutter), horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically) {
                item { Text("Show level", style = MaterialTheme.typography.labelMedium, color = AppTheme.colors.textMuted, modifier = Modifier.padding(end = 4.dp)) }
                items(Difficulty.entries) { d -> FilterPill(d.label, d == level) { levelName = d.name } }
            }
        }

        if (mode == Mode.HOME) {
            // Personal plan
            profile?.let { p ->
                val prog = HomePrograms.personalProgram(p)
                val card = programCard(prog, progress)
                item {
                    HeroCard(listOf(Brand.Blue, Brand.Purple), Modifier.padding(horizontal = Gutter).fillMaxWidth(), Icons.Filled.CalendarMonth,
                        onClick = { nav.navigate(Routes.program(prog.id)) }) {
                        Column {
                            Text("MADE FOR YOU", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.8f))
                            Text(prog.title, style = MaterialTheme.typography.headlineSmall, color = Color.White)
                            Text(prog.subtitle, style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.85f))
                            Spacer(Modifier.height(12.dp))
                            LinearMeter(card.done / card.total.toFloat(), color = Color.White, track = Color.White.copy(alpha = 0.25f), height = 6.dp)
                            Spacer(Modifier.height(6.dp))
                            Text("${card.done}/${card.total} days complete", style = MaterialTheme.typography.labelMedium, color = Color.White)
                        }
                    }
                }
            }

            // Plans for the selected level
            item { SectionHeader("Plans", Modifier.padding(top = 4.dp)) }
            item {
                val plans = HomePrograms.plans.filter { it.level == level }
                LazyRow(contentPadding = PaddingValues(horizontal = Gutter), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(plans, key = { it.id }) { plan ->
                        val goal = PlanSpec.decode(plan.id)?.goal ?: PlanGoal.GENERAL
                        val (icon, tint) = Visuals.planGoal(goal)
                        val card = programCard(plan, progress)
                        HeroCard(Visuals.gradient(tint), Modifier.width(220.dp).height(170.dp), icon, onClick = { nav.navigate(Routes.program(plan.id)) }) {
                            Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.SpaceBetween) {
                                Column {
                                    Text(plan.title, style = MaterialTheme.typography.titleLarge, color = Color.White)
                                    Text(goal.desc, style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.85f), maxLines = 2)
                                }
                                Column {
                                    Text(plan.subtitle, style = MaterialTheme.typography.labelSmall, color = Color.White)
                                    if (card.done > 0) {
                                        Spacer(Modifier.height(6.dp))
                                        LinearMeter(card.done / card.total.toFloat(), color = Color.White, track = Color.White.copy(alpha = 0.25f), height = 5.dp)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Challenges
            item { SectionHeader("30-day challenges", Modifier.padding(top = 4.dp)) }
            items(HomePrograms.challenges, key = { "ch_" + it.id }) { c ->
                val card = programCard(c, progress)
                ListRow(
                    c.title, if (card.nextDay == null) "Completed 🎉" else "${card.done}/30 days · ${c.subtitle}",
                    onClick = { nav.navigate(Routes.program(c.id)) }, modifier = Modifier.padding(horizontal = Gutter),
                    leading = { IconBadge(Icons.Filled.EmojiEvents, Brand.Amber, 52.dp) }
                )
            }

            // Categories grid
            item { SectionHeader("Browse by category", Modifier.padding(top = 4.dp)) }
            categoryGrid(level) { nav.navigate(Routes.category(it)) }
        } else {
            gymContent(state, level, nav, onBuild = { vm.regenerate() }, onDelete = { toDelete = it })
        }
    }

    toDelete?.let { day ->
        AlertDialog(
            onDismissRequest = { toDelete = null },
            title = { Text("Delete \"${day.name}\"?") },
            text = { Text("The routine is removed. Your logged workouts stay in your history.") },
            confirmButton = { TextButton(onClick = { vm.deleteRoutine(day.id); toDelete = null }) { Text("Delete") } },
            dismissButton = { TextButton(onClick = { toDelete = null }) { Text("Cancel") } }
        )
    }
}

private fun LazyListScope.categoryGrid(level: Difficulty, onOpen: (WorkoutCategory) -> Unit) {
    val cats = WorkoutCategory.entries
    items(cats.chunked(2), key = { "cat_" + it.first().name }) { row ->
        Row(Modifier.fillMaxWidth().padding(horizontal = Gutter), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            row.forEach { cat ->
                val (icon, tint) = Visuals.category(cat)
                val n = HomePrograms.byCategory(cat).size
                val atLevel = HomePrograms.byCategory(cat).count { it.level == level }
                AppCard(Modifier.weight(1f), onClick = { onOpen(cat) }) {
                    IconBadge(icon, tint, 44.dp)
                    Spacer(Modifier.height(12.dp))
                    Text(cat.label, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(if (atLevel > 0) "$n workouts" else "$n workouts · other levels",
                        style = MaterialTheme.typography.bodySmall, color = AppTheme.colors.textMuted, maxLines = 1)
                }
            }
            if (row.size == 1) Spacer(Modifier.weight(1f))
        }
    }
}

private fun LazyListScope.gymContent(
    state: TrainState, level: Difficulty, nav: NavController, onBuild: () -> Unit, onDelete: (DayView) -> Unit
) {
    // 1. Hero: your plan + up next
    item {
        if (state.hasPlan) GymPlanHero(state, Modifier.padding(horizontal = Gutter),
            onOpen = { nav.navigate(Routes.GYM_PLAN) },
            onStart = { state.next?.let { nav.navigate(Routes.routine(it.id)) } })
        else if (state.loaded) NoGymPlanHero(Modifier.padding(horizontal = Gutter), onBuild)
    }
    if (state.loaded) item { VolumeSummaryCard(state, Modifier.padding(horizontal = Gutter)) { nav.navigate(Routes.VOLUME) } }

    // 2. Programs carousel for the selected level
    item { SectionHeader("Programs", Modifier.padding(top = 4.dp)) }
    item {
        LazyRow(contentPadding = PaddingValues(horizontal = Gutter), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            items(GymPrograms.forLevel(level), key = { "gp_" + it.id }) { p ->
                val (icon, tint) = Visuals.gymProgram(p)
                HeroCard(Visuals.gradient(tint), Modifier.width(220.dp).height(170.dp), icon, onClick = { nav.navigate(Routes.gymProgram(p.id)) }) {
                    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.SpaceBetween) {
                        Column {
                            Text(p.title, style = MaterialTheme.typography.titleLarge, color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Spacer(Modifier.height(4.dp))
                            Text(p.description, style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.85f),
                                maxLines = 3, overflow = TextOverflow.Ellipsis)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("${p.daysPerWeek} days · ${p.weeks} weeks", style = MaterialTheme.typography.labelSmall, color = Color.White,
                                modifier = Modifier.weight(1f))
                            if (state.planName == p.title) OnHeroTag("Active")
                        }
                    }
                }
            }
        }
    }

    // 3. Browse by muscle
    item { SectionHeader("Browse by muscle", Modifier.padding(top = 4.dp)) }
    items(GymCategory.entries.chunked(2), key = { "gc_" + it.first().name }) { row ->
        if (row.size == 1) {
            // Odd one out spans the full width as a banner row.
            val cat = row.first()
            val (icon, tint) = Visuals.gymCategory(cat)
            AppCard(Modifier.padding(horizontal = Gutter), onClick = { nav.navigate(Routes.gymCategory(cat)) }) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconBadge(icon, tint, 44.dp)
                    Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                        Text(cat.label, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)
                        Text("${GymWorkouts.byCategory(cat).size} workouts · ${cat.blurb}", style = MaterialTheme.typography.bodySmall,
                            color = AppTheme.colors.textMuted, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
            return@items
        }
        Row(Modifier.fillMaxWidth().padding(horizontal = Gutter), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            row.forEach { cat ->
                val (icon, tint) = Visuals.gymCategory(cat)
                val n = GymWorkouts.byCategory(cat).size
                AppCard(Modifier.weight(1f), onClick = { nav.navigate(Routes.gymCategory(cat)) }) {
                    IconBadge(icon, tint, 44.dp)
                    Spacer(Modifier.height(12.dp))
                    Text(cat.label, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text("$n workouts", style = MaterialTheme.typography.bodySmall, color = AppTheme.colors.textMuted, maxLines = 1)
                }
            }
            if (row.size == 1) Spacer(Modifier.weight(1f))
        }
    }

    // 4. My routines
    item { SectionHeader("My routines", Modifier.padding(top = 4.dp), "+ Create") { nav.navigate(Routes.ROUTINE_NEW) } }
    item {
        LazyRow(contentPadding = PaddingValues(horizontal = Gutter), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            item(key = "new") {
                AppCard(Modifier.width(150.dp).height(132.dp), onClick = { nav.navigate(Routes.ROUTINE_NEW) }) {
                    IconBadge(Icons.Filled.Add, MaterialTheme.colorScheme.primary, 40.dp)
                    Spacer(Modifier.weight(1f))
                    Text("Create routine", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)
                    Text("880+ exercises", style = MaterialTheme.typography.bodySmall, color = AppTheme.colors.textMuted)
                }
            }
            items(state.custom, key = { "c_${it.id}" }) { d ->
                AppCard(Modifier.width(180.dp).height(132.dp), onClick = { nav.navigate(Routes.routine(d.id)) }) {
                    Row(verticalAlignment = Alignment.Top) {
                        IconBadge(Icons.Filled.FitnessCenter, Brand.Teal, 40.dp)
                        Spacer(Modifier.weight(1f))
                        Icon(Icons.Filled.Delete, contentDescription = "Delete ${d.name}", tint = AppTheme.colors.textFaint,
                            modifier = Modifier.size(20.dp).clickable { onDelete(d) })
                    }
                    Spacer(Modifier.weight(1f))
                    Text(d.name, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text("${d.exercises.size} exercises · ${d.sets} sets", style = MaterialTheme.typography.bodySmall,
                        color = AppTheme.colors.textMuted, maxLines = 1)
                }
            }
        }
    }

    // 5. Tools
    item { SectionHeader("Gym tools", Modifier.padding(top = 4.dp)) }
    item {
        Row(Modifier.fillMaxWidth().padding(horizontal = Gutter), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            ToolTile("1RM", "Max lift", Icons.Filled.Calculate, Brand.Orange, Modifier.weight(1f)) { nav.navigate(Routes.TOOLS) }
            ToolTile("Plates", "Bar loader", Icons.Filled.FitnessCenter, Brand.Blue, Modifier.weight(1f)) { nav.navigate(Routes.TOOLS) }
            ToolTile("BMI", "Body index", Icons.Filled.MonitorWeight, Brand.Green, Modifier.weight(1f)) { nav.navigate(Routes.TOOLS) }
        }
    }
}

/** Gradient shared by every "your gym plan" surface. */
internal val GymHeroColors = listOf(Brand.Orange, Brand.Red)

@Composable
internal fun GymPlanHero(state: TrainState, modifier: Modifier = Modifier, onOpen: (() -> Unit)?, onStart: () -> Unit) {
    val next = state.next
    HeroCard(GymHeroColors, modifier.fillMaxWidth(), Icons.Filled.FitnessCenter, onClick = onOpen) {
        Column {
            Text("YOUR GYM PLAN", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.8f))
            Text(state.planName, style = MaterialTheme.typography.headlineSmall, color = Color.White, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text("${state.days.size} days a week" + (if (state.weeks > 0) " · ${state.weeks} weeks" else "") +
                " · ${state.sessionsDone} done", style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.85f))
            Spacer(Modifier.height(14.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                DayDots(state.days.size, state.nextIndex)
                Spacer(Modifier.weight(1f))
                if (state.deload.active) OnHeroTag("Deload week")
            }
            if (next != null) {
                Spacer(Modifier.height(14.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("UP NEXT · DAY ${state.nextIndex + 1}", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.8f))
                        Text(next.name, style = MaterialTheme.typography.titleMedium, color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text("${next.exercises.size} exercises · ${next.sets} sets", style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.85f))
                    }
                    HeroAction("Start session", Brand.Red, onStart)
                }
            }
        }
    }
}

@Composable
private fun DayDots(count: Int, next: Int) {
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        repeat(count) { i ->
            val isNext = i == next
            Box(
                Modifier.size(28.dp).clip(CircleShape).background(if (isNext) Color.White else Color.White.copy(alpha = 0.22f)),
                contentAlignment = Alignment.Center
            ) {
                Text("${i + 1}", style = MaterialTheme.typography.labelMedium, color = if (isNext) Brand.Red else Color.White)
            }
        }
    }
}

@Composable
internal fun NoGymPlanHero(modifier: Modifier = Modifier, onBuild: () -> Unit) {
    HeroCard(listOf(Color(0xFF2A3350), Brand.Blue), modifier.fillMaxWidth(), Icons.Filled.FitnessCenter) {
        Column {
            Text("START LIFTING", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.8f))
            Text("Get your gym plan", style = MaterialTheme.typography.headlineSmall, color = Color.White)
            Text("We build it from your goal, level and schedule — or pick a program below.",
                style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.85f))
            Spacer(Modifier.height(14.dp))
            HeroAction("Build my plan", Brand.Blue, onBuild)
        }
    }
}

/** White pill on a gradient hero. */
@Composable
internal fun HeroAction(text: String, tint: Color, onClick: () -> Unit) {
    Row(
        Modifier.height(40.dp).clip(RoundedCornerShape(50)).background(Color.White).clickable(onClick = onClick).padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Filled.PlayArrow, contentDescription = null, tint = tint, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(4.dp))
        Text(text, style = MaterialTheme.typography.labelLarge, color = tint)
    }
}

@Composable
private fun OnHeroTag(text: String) {
    Text(text, style = MaterialTheme.typography.labelSmall, color = Color.White,
        modifier = Modifier.clip(RoundedCornerShape(50)).background(Color.White.copy(alpha = 0.25f)).padding(horizontal = 10.dp, vertical = 4.dp))
}

@Composable
private fun ToolTile(title: String, meta: String, icon: androidx.compose.ui.graphics.vector.ImageVector, tint: Color, modifier: Modifier, onClick: () -> Unit) {
    AppCard(modifier, onClick = onClick, padding = PaddingValues(14.dp)) {
        IconBadge(icon, tint, 36.dp)
        Spacer(Modifier.height(10.dp))
        Text(title, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)
        Text(meta, style = MaterialTheme.typography.bodySmall, color = AppTheme.colors.textMuted, maxLines = 1)
    }
}
