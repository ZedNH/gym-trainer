package com.gymtrainer.app.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Whatshot
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.foundation.clickable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.gymtrainer.app.domain.HomeWorkout
import com.gymtrainer.app.domain.TrainingPlace
import com.gymtrainer.app.ui.components.AppCard
import com.gymtrainer.app.ui.components.Gutter
import com.gymtrainer.app.ui.components.HeroCard
import com.gymtrainer.app.ui.components.IconBadge
import com.gymtrainer.app.ui.components.LevelBadge
import com.gymtrainer.app.ui.components.LinearMeter
import com.gymtrainer.app.ui.components.ListRow
import com.gymtrainer.app.ui.components.PillButton
import com.gymtrainer.app.ui.components.ProgressRing
import com.gymtrainer.app.ui.components.SectionHeader
import com.gymtrainer.app.ui.components.Visuals
import com.gymtrainer.app.ui.gymViewModel
import com.gymtrainer.app.ui.navigation.Routes
import com.gymtrainer.app.ui.navigation.goToTab
import com.gymtrainer.app.ui.rememberSettings
import com.gymtrainer.app.ui.theme.AppTheme
import com.gymtrainer.app.ui.theme.Brand
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HomeScreen(contentPadding: PaddingValues, nav: NavController) {
    val vm = gymViewModel { HomeViewModel(it) }
    val state by vm.state.collectAsStateWithLifecycle()
    val settings = rememberSettings()
    val rest = settings.homeRestSeconds

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = contentPadding.calculateBottomPadding() + 28.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item { Header(state) { nav.navigate(Routes.PROFILE) } }

        // Week strip
        item {
            AppCard(Modifier.padding(horizontal = Gutter)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text("This week", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
                    Text("${state.workoutsThisWeek}/${settings.weeklyGoal} workouts", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                }
                Spacer(Modifier.height(12.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    state.week.forEach { d -> DayChip(d) }
                }
                Spacer(Modifier.height(14.dp))
                LinearMeter(state.workoutsThisWeek / settings.weeklyGoal.toFloat())
                Spacer(Modifier.height(12.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    MiniStat(Icons.Filled.LocalFireDepartment, Brand.Orange, "${state.kcalThisWeek}", "kcal")
                    MiniStat(Icons.Filled.Timer, Brand.Blue, "${state.minutesThisWeek}", "min")
                    MiniStat(Icons.Filled.Whatshot, Brand.Red, "${state.streak}", "day streak")
                }
            }
        }

        // Nutrition snapshot
        item { NutritionSnapshot { nav.goToTab(Routes.NUTRITION) } }

        // Today's plan
        item { SectionHeader("Today's plan", Modifier.padding(top = 6.dp)) }
        if (state.place != TrainingPlace.GYM && state.personal != null) {
            item {
                PersonalPlanCard(state, rest,
                    onOpen = { state.personal?.let { nav.navigate(Routes.program(it.id)) } },
                    onStart = { state.personalNext?.let { nav.navigate(Routes.homePreview(it.key)) } })
            }
        }
        if (state.place != TrainingPlace.HOME) {
            item {
                if (state.gymDayId != null) {
                    AppCard(Modifier.padding(horizontal = Gutter), onClick = { state.gymDayId?.let { nav.navigate(Routes.routine(it)) } }) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconBadge(Icons.Filled.FitnessCenter, Brand.Blue, 50.dp)
                            Column(Modifier.weight(1f).padding(horizontal = 14.dp)) {
                                Text("GYM SESSION", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                                Text(state.gymDayName.orEmpty(), style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurface)
                                Text("${state.gymExerciseCount} exercises · ${state.gymDayFocus.orEmpty()}", style = MaterialTheme.typography.bodySmall,
                                    color = AppTheme.colors.textMuted, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                            PillButton("Start", { state.gymDayId?.let { nav.navigate(Routes.routine(it)) } }, icon = Icons.Filled.PlayArrow)
                        }
                    }
                }
            }
        }

        // Challenges
        item { SectionHeader("Challenges", Modifier.padding(top = 6.dp), "See all") { nav.goToTab(Routes.TRAIN) } }
        item {
            LazyRow(contentPadding = PaddingValues(horizontal = Gutter), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                items(state.challenges, key = { it.id }) { c ->
                    AppCard(Modifier.width(230.dp), onClick = { nav.navigate(Routes.program(c.id)) }) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            ProgressRing(c.done / c.total.toFloat(), size = 52.dp, stroke = 6.dp) {
                                Text("${c.done}", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)
                            }
                            Column(Modifier.padding(start = 12.dp)) {
                                Text(c.title, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface, maxLines = 2)
                                Text(if (c.nextDay == null) "Completed" else if (c.done == 0) "30 days · Start now" else "Day ${c.nextDay} of 30",
                                    style = MaterialTheme.typography.bodySmall, color = AppTheme.colors.textMuted)
                            }
                        }
                    }
                }
            }
        }

        // Recommended
        item { SectionHeader("Recommended for you", Modifier.padding(top = 6.dp)) }
        items(state.recommended, key = { "rec_" + it.key }) { w ->
            WorkoutRow(w, rest, Modifier.padding(horizontal = Gutter)) { nav.navigate(Routes.homePreview(w.key)) }
        }
    }
}

@Composable
private fun Header(state: HomeState, onProfile: () -> Unit) {
    val date = SimpleDateFormat("EEEE, MMM d", Locale.getDefault()).format(Date())
    Row(
        Modifier.fillMaxWidth().statusBarsPadding().padding(start = Gutter, end = Gutter, top = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(date.uppercase(Locale.getDefault()), style = MaterialTheme.typography.labelSmall, color = AppTheme.colors.textMuted)
            Text("Hi, ${state.name.ifBlank { "there" }}", style = MaterialTheme.typography.headlineLarge, color = MaterialTheme.colorScheme.onBackground)
        }
        Box(
            Modifier.size(46.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer)
                .clickable(onClick = onProfile)
                .semantics { contentDescription = "Open profile" },
            contentAlignment = Alignment.Center
        ) {
            Text(state.name.take(1).uppercase().ifBlank { "?" }, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
private fun NutritionSnapshot(onOpen: () -> Unit) {
    val repo = com.gymtrainer.app.ui.rememberRepository()
    val nutrition = com.gymtrainer.app.ui.rememberNutrition()
    val settings = rememberSettings()
    val day = com.gymtrainer.app.data.NutritionRepository.today()
    val profile by androidx.compose.runtime.produceState<com.gymtrainer.app.data.UserProfileEntity?>(null) { value = repo.getProfile() }
    val foods by nutrition.observeFoods(day).collectAsState(initial = emptyList())
    val water by nutrition.observeWater(day).collectAsState(initial = 0)
    val t = profile?.let { com.gymtrainer.app.data.NutritionCalc.targets(it, settings.sexForCalories) } ?: return
    val kcal = foods.sumOf { it.kcal }.toInt()
    AppCard(Modifier.padding(horizontal = Gutter), onClick = onOpen) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.LocalFireDepartment, contentDescription = null, tint = Brand.Orange, modifier = Modifier.size(16.dp))
                    Text("  $kcal / ${t.kcal} kcal", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)
                }
                Spacer(Modifier.height(6.dp))
                LinearMeter(kcal / t.kcal.toFloat(), color = Brand.Orange, height = 6.dp)
            }
            Spacer(Modifier.width(18.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.WaterDrop, contentDescription = null, tint = Brand.Teal, modifier = Modifier.size(16.dp))
                    Text("  ${com.gymtrainer.app.ui.Format.litres(water)} / ${com.gymtrainer.app.ui.Format.litres(t.waterMl)} L", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)
                }
                Spacer(Modifier.height(6.dp))
                LinearMeter(water / t.waterMl.toFloat(), color = Brand.Teal, height = 6.dp)
            }
        }
    }
}

@Composable
private fun DayChip(d: DayDot) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(d.label.take(1), style = MaterialTheme.typography.labelSmall, color = if (d.today) MaterialTheme.colorScheme.primary else AppTheme.colors.textMuted)
        Spacer(Modifier.height(6.dp))
        Box(
            Modifier.size(36.dp).clip(CircleShape)
                .background(when { d.done -> MaterialTheme.colorScheme.primary; d.today -> MaterialTheme.colorScheme.primaryContainer; else -> AppTheme.colors.cardAlt })
                .then(if (d.today && !d.done) Modifier.border(2.dp, MaterialTheme.colorScheme.primary, CircleShape) else Modifier),
            contentAlignment = Alignment.Center
        ) {
            if (d.done) Icon(Icons.Filled.Check, contentDescription = "Trained", tint = Color.White, modifier = Modifier.size(18.dp))
            else Text("${d.date}", style = MaterialTheme.typography.labelMedium, color = if (d.today) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface)
        }
    }
}

@Composable
private fun MiniStat(icon: ImageVector, tint: Color, value: String, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(4.dp))
        Text(value, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)
        Spacer(Modifier.width(3.dp))
        Text(label, style = MaterialTheme.typography.bodySmall, color = AppTheme.colors.textMuted)
    }
}

@Composable
private fun PersonalPlanCard(state: HomeState, rest: Int, onOpen: () -> Unit, onStart: () -> Unit) {
    val card = state.personal ?: return
    val next = state.personalNext
    HeroCard(listOf(Brand.Blue, Brand.Purple), Modifier.padding(horizontal = Gutter).fillMaxWidth(), icon = Icons.Filled.CalendarMonth, onClick = onOpen) {
        Column {
            Text("HOME · YOUR PERSONAL PLAN", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.8f))
            Spacer(Modifier.height(4.dp))
            Text(next?.title ?: "Plan complete!", style = MaterialTheme.typography.headlineSmall, color = Color.White)
            Text(
                if (next != null) "Day ${card.nextDay} · ${next.items.size} moves · ~${next.estimatedMinutes(next.restSeconds ?: rest)} min" else "Great work — open the plan to restart.",
                style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha = 0.9f), maxLines = 1
            )
            Spacer(Modifier.height(14.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("${card.done}/${card.total} days", style = MaterialTheme.typography.labelMedium, color = Color.White)
                    Spacer(Modifier.height(6.dp))
                    LinearMeter(card.done / card.total.toFloat(), color = Color.White, track = Color.White.copy(alpha = 0.25f), height = 6.dp)
                }
                Spacer(Modifier.width(16.dp))
                if (next != null) {
                    Row(
                        Modifier.height(44.dp).clip(RoundedCornerShape(50)).background(Color.White)
                            .then(Modifier.padding(horizontal = 0.dp)),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        androidx.compose.material3.TextButton(onClick = onStart) {
                            Icon(Icons.Filled.PlayArrow, contentDescription = null, tint = Brand.Blue)
                            Text("Start", style = MaterialTheme.typography.labelLarge, color = Brand.Blue)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun WorkoutRow(w: HomeWorkout, rest: Int, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val (icon, tint) = Visuals.category(w.category)
    ListRow(
        title = w.title,
        meta = "${w.estimatedMinutes(rest)} min · ${w.items.size} exercises",
        onClick = onClick, modifier = modifier,
        leading = { IconBadge(icon, tint, 52.dp) },
        badge = { LevelBadge(w.level) }
    )
}
