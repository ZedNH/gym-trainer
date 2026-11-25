package com.gymtrainer.app.ui.onboarding

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.gymtrainer.app.domain.BodyArea
import com.gymtrainer.app.domain.Experience
import com.gymtrainer.app.domain.Goal
import com.gymtrainer.app.domain.Limitation
import com.gymtrainer.app.domain.TrainingPlace
import com.gymtrainer.app.domain.Units
import com.gymtrainer.app.ui.Format
import com.gymtrainer.app.ui.components.AppCard
import com.gymtrainer.app.ui.components.AppTextField
import com.gymtrainer.app.ui.components.ChoiceCard
import com.gymtrainer.app.ui.components.CircleIconButton
import com.gymtrainer.app.ui.components.FilterPill
import com.gymtrainer.app.ui.components.Gutter
import com.gymtrainer.app.ui.components.IconBadge
import com.gymtrainer.app.ui.components.NumberPicker
import com.gymtrainer.app.ui.components.PrimaryButton
import com.gymtrainer.app.ui.components.ProgressRing
import com.gymtrainer.app.ui.components.SegmentedTabs
import com.gymtrainer.app.ui.components.Visuals
import com.gymtrainer.app.ui.gymViewModel
import com.gymtrainer.app.ui.theme.AppTheme
import com.gymtrainer.app.ui.theme.Brand
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

private const val LAST = 9

@Composable
fun OnboardingScreen(onDone: () -> Unit) {
    val vm = gymViewModel { OnboardingViewModel(it) }
    var step by rememberSaveable { mutableIntStateOf(0) }
    var building by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    BackHandler(enabled = step > 0 && !building) { step-- }

    if (building) {
        BuildingPlan(vm.name) {
            scope.launch {
                vm.save()
                withContext(Dispatchers.Main) { onDone() }
            }
        }
        return
    }

    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).statusBarsPadding().navigationBarsPadding().imePadding()) {
        if (step > 0) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                CircleIconButton(Icons.AutoMirrored.Filled.ArrowBack, "Back", { step-- })
                Row(Modifier.weight(1f).padding(horizontal = 14.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    (1..LAST).forEach { i ->
                        Box(Modifier.weight(1f).height(5.dp).clip(RoundedCornerShape(50))
                            .background(if (i <= step) MaterialTheme.colorScheme.primary else AppTheme.colors.border))
                    }
                }
                Text("$step/$LAST", style = MaterialTheme.typography.labelMedium, color = AppTheme.colors.textMuted)
            }
        }

        AnimatedContent(
            targetState = step,
            transitionSpec = {
                val dir = if (targetState > initialState) 1 else -1
                (slideInHorizontally { it * dir / 4 } + fadeIn()) togetherWith (slideOutHorizontally { -it * dir / 4 } + fadeOut())
            },
            modifier = Modifier.weight(1f),
            label = "onboarding"
        ) { s ->
            Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = Gutter)) {
                when (s) {
                    0 -> Welcome()
                    1 -> StepName(vm)
                    2 -> StepGoal(vm)
                    3 -> StepPlace(vm)
                    4 -> StepLevel(vm)
                    5 -> StepFocus(vm)
                    6 -> StepBody(vm)
                    7 -> StepSchedule(vm)
                    8 -> StepEquipment(vm)
                    9 -> StepLimits(vm)
                }
                Spacer(Modifier.height(24.dp))
            }
        }

        val canGo = step != 1 || vm.name.isNotBlank()
        PrimaryButton(
            text = when (step) { 0 -> "Get started"; LAST -> "Create my plan"; else -> "Continue" },
            onClick = { if (step < LAST) step++ else building = true },
            enabled = canGo,
            modifier = Modifier.fillMaxWidth().padding(horizontal = Gutter, vertical = 12.dp)
        )
    }
}

// ---------------- Steps ----------------

@Composable
private fun Question(title: String, subtitle: String) {
    Spacer(Modifier.height(12.dp))
    Text(title, style = MaterialTheme.typography.headlineLarge, color = MaterialTheme.colorScheme.onBackground)
    Spacer(Modifier.height(6.dp))
    Text(subtitle, style = MaterialTheme.typography.bodyLarge, color = AppTheme.colors.textMuted)
    Spacer(Modifier.height(24.dp))
}

@Composable
private fun Welcome() {
    Column(Modifier.statusBarsPadding()) {
        Spacer(Modifier.height(24.dp))
        Box(
            Modifier.fillMaxWidth().aspectRatio(1.15f).clip(RoundedCornerShape(32.dp))
                .background(Brush.linearGradient(listOf(Brand.Blue, Brand.Purple))),
            contentAlignment = Alignment.Center
        ) {
            Box(Modifier.size(320.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.07f)))
            Box(Modifier.size(230.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.08f)))
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(Modifier.size(84.dp).clip(RoundedCornerShape(26.dp)).background(Color.White.copy(alpha = 0.2f)), contentAlignment = Alignment.Center) {
                    Icon(Icons.Filled.FitnessCenter, contentDescription = null, tint = Color.White, modifier = Modifier.size(44.dp))
                }
                Spacer(Modifier.height(14.dp))
                Text("Gym Trainer", style = MaterialTheme.typography.headlineMedium, color = Color.White)
                Text("Home & gym workouts", style = MaterialTheme.typography.bodyLarge, color = Color.White.copy(alpha = 0.85f))
            }
        }
        Spacer(Modifier.height(28.dp))
        Text("Welcome to Gym Trainer", style = MaterialTheme.typography.headlineLarge, color = MaterialTheme.colorScheme.onBackground)
        Spacer(Modifier.height(6.dp))
        Text("Answer a few quick questions and we'll build a plan that fits your body, goal and schedule.",
            style = MaterialTheme.typography.bodyLarge, color = AppTheme.colors.textMuted)
        Spacer(Modifier.height(20.dp))
        Feature(Icons.Filled.CalendarMonth, Brand.Blue, "Personal plan", "Home and gym sessions matched to your level")
        Feature(Icons.Filled.RecordVoiceOver, Brand.Orange, "Guided workouts", "Timers, demos and a voice coach")
        Feature(Icons.Filled.Insights, Brand.Green, "Track progress", "Workouts, weights, records and body stats")
    }
}

@Composable
private fun Feature(icon: ImageVector, tint: Color, title: String, sub: String) {
    Row(Modifier.padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        IconBadge(icon, tint, 44.dp)
        Column(Modifier.padding(start = 14.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onBackground)
            Text(sub, style = MaterialTheme.typography.bodyMedium, color = AppTheme.colors.textMuted)
        }
    }
}

@Composable
private fun StepName(vm: OnboardingViewModel) {
    Question("What's your name?", "We'll use it to personalise your coaching.")
    AppTextField(vm.name, { vm.name = it.take(24) }, placeholder = "Your name")
}

@Composable
private fun StepGoal(vm: OnboardingViewModel) {
    Question("What's your main goal?", "Your plan's rep ranges, rest and workout style follow this.")
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Goal.entries.forEach { g ->
            val (icon, tint) = Visuals.goal(g)
            ChoiceCard(g.label, vm.goal == g, { vm.goal = g }, subtitle = g.desc, icon = icon, tint = tint)
        }
    }
}

@Composable
private fun StepPlace(vm: OnboardingViewModel) {
    Question("Where will you train?", "We'll recommend the right workouts for each day.")
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        TrainingPlace.entries.forEach { p ->
            val (icon, tint) = Visuals.place(p)
            ChoiceCard(p.label, vm.place == p, { vm.onPlaceChanged(p) }, subtitle = p.desc, icon = icon, tint = tint)
        }
    }
}

@Composable
private fun StepLevel(vm: OnboardingViewModel) {
    Question("What's your fitness level?", "Be honest — you can always level up later.")
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Experience.entries.forEach { e ->
            val (icon, tint) = Visuals.experience(e)
            ChoiceCard(e.label, vm.experience == e, { vm.experience = e }, subtitle = e.desc, icon = icon, tint = tint)
        }
    }
}

@Composable
private fun StepFocus(vm: OnboardingViewModel) {
    Question("Which areas do you want to focus on?", "Pick as many as you like.")
    val areas = BodyArea.entries
    areas.chunked(2).forEach { row ->
        Row(Modifier.fillMaxWidth().padding(bottom = 10.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            row.forEach { a ->
                val (icon, tint) = Visuals.area(a)
                val sel = a in vm.focus
                AreaTile(a.label, icon, tint, sel, Modifier.weight(1f)) { vm.toggleFocus(a) }
            }
            if (row.size == 1) Spacer(Modifier.weight(1f))
        }
    }
}

@Composable
private fun AreaTile(label: String, icon: ImageVector, tint: Color, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val shape = RoundedCornerShape(20.dp)
    Column(
        modifier.height(118.dp).clip(shape)
            .background(if (selected) tint.copy(alpha = 0.12f) else AppTheme.colors.card)
            .border(if (selected) 2.dp else 1.dp, if (selected) tint else AppTheme.colors.border, shape)
            .clickable(onClick = onClick).padding(14.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            IconBadge(icon, tint, 40.dp)
            if (selected) Box(Modifier.size(22.dp).clip(CircleShape).background(tint), contentAlignment = Alignment.Center) {
                Icon(Icons.Filled.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(15.dp))
            }
        }
        Text(label, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
private fun StepBody(vm: OnboardingViewModel) {
    Question("Tell us about your body", "Used for calorie estimates and BMI. You can change these later.")
    SegmentedTabs(listOf(Units.METRIC to "kg / cm", Units.IMPERIAL to "lb / ft"), vm.units, { vm.units = it })
    Spacer(Modifier.height(14.dp))
    NumberPicker("Age", "${vm.age}", "years", vm.age.toFloat(), 13f..90f,
        { vm.age = (vm.age - 1).coerceAtLeast(13) }, { vm.age = (vm.age + 1).coerceAtMost(90) }, { vm.age = it.roundToInt() })
    Spacer(Modifier.height(12.dp))
    val imperial = vm.units == Units.IMPERIAL
    NumberPicker("Height", if (imperial) Format.height(vm.heightCm, vm.units) else "${vm.heightCm.roundToInt()}", if (imperial) "" else "cm",
        vm.heightCm.toFloat(), 120f..220f,
        { vm.heightCm = (vm.heightCm - 1).coerceAtLeast(120.0) }, { vm.heightCm = (vm.heightCm + 1).coerceAtMost(220.0) },
        { vm.heightCm = it.roundToInt().toDouble() })
    Spacer(Modifier.height(12.dp))
    val step = if (imperial) 0.4536 else 0.5
    NumberPicker("Weight", Format.trim(Math.round(Format.weightValue(vm.weightKg, vm.units) * 2) / 2.0), Format.weightUnit(vm.units),
        vm.weightKg.toFloat(), 35f..200f,
        { vm.weightKg = (vm.weightKg - step).coerceAtLeast(35.0) }, { vm.weightKg = (vm.weightKg + step).coerceAtMost(200.0) },
        { vm.weightKg = Math.round(it * 2) / 2.0 })
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun StepSchedule(vm: OnboardingViewModel) {
    Question("How often can you train?", "Pick a schedule you can keep. Consistency beats intensity.")
    Text("Days per week", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onBackground)
    Spacer(Modifier.height(10.dp))
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        (2..6).forEach { d ->
            val sel = vm.daysPerWeek == d
            Box(
                Modifier.weight(1f).aspectRatio(1f).clip(RoundedCornerShape(18.dp))
                    .background(if (sel) MaterialTheme.colorScheme.primary else AppTheme.colors.card)
                    .border(1.dp, if (sel) MaterialTheme.colorScheme.primary else AppTheme.colors.border, RoundedCornerShape(18.dp))
                    .clickable { vm.daysPerWeek = d },
                contentAlignment = Alignment.Center
            ) {
                Text("$d", style = MaterialTheme.typography.headlineSmall, color = if (sel) Color.White else MaterialTheme.colorScheme.onSurface)
            }
        }
    }
    Text(
        when (vm.daysPerWeek) { 2 -> "Great for busy weeks"; 3 -> "Recommended for most people"; 4 -> "Solid progress"; 5 -> "Committed"; else -> "Athlete mode — recover well" },
        style = MaterialTheme.typography.bodySmall, color = AppTheme.colors.textMuted, modifier = Modifier.padding(top = 8.dp)
    )
    Spacer(Modifier.height(24.dp))
    Text("Time per workout", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onBackground)
    Spacer(Modifier.height(10.dp))
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf(15, 20, 30, 45, 60, 75).forEach { m -> FilterPill("$m min", vm.sessionMinutes == m) { vm.sessionMinutes = m } }
    }
}

@Composable
private fun StepEquipment(vm: OnboardingViewModel) {
    Question("What equipment do you have?", "Exercises will only use what's available.")
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        vm.equipmentOptions().forEach { e ->
            val (icon, tint) = Visuals.equipment(e)
            ChoiceCard(e.label, vm.equipment == e, { vm.equipment = e }, subtitle = e.desc, icon = icon, tint = tint)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun StepLimits(vm: OnboardingViewModel) {
    Question("Any pain or injuries?", "We'll leave out exercises that load these areas. This isn't medical advice — see a professional for pain.")
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        FilterPill("None", vm.limitations.isEmpty()) { vm.toggleLimitation(null) }
        Limitation.entries.forEach { l -> FilterPill(l.label, l in vm.limitations) { vm.toggleLimitation(l) } }
    }
    Spacer(Modifier.height(18.dp))
    AppTextField(vm.notes, { vm.notes = it }, placeholder = "Anything else we should know? (optional)", singleLine = false, minLines = 3)
}

// ---------------- Building screen ----------------

@Composable
private fun BuildingPlan(name: String, onReady: () -> Unit) {
    var progress by remember { mutableFloatStateOf(0f) }
    val steps = listOf("Analysing your profile", "Choosing exercises for your level", "Scheduling your week", "Setting up progress tracking")
    LaunchedEffect(Unit) {
        while (progress < 1f) { delay(40); progress = (progress + 0.025f).coerceAtMost(1f) }
        onReady()
    }
    Column(
        Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).statusBarsPadding().padding(Gutter),
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center
    ) {
        ProgressRing(progress, size = 150.dp, stroke = 12.dp) {
            Text("${(progress * 100).roundToInt()}%", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.onBackground)
        }
        Spacer(Modifier.height(28.dp))
        Text("Building your plan${if (name.isNotBlank()) ", ${name.trim()}" else ""}", style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onBackground, textAlign = TextAlign.Center)
        Spacer(Modifier.height(20.dp))
        AppCard {
            steps.forEachIndexed { i, s ->
                val done = progress >= (i + 1) / steps.size.toFloat()
                Row(Modifier.padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(22.dp).clip(CircleShape).background(if (done) AppTheme.colors.success else AppTheme.colors.cardAlt), contentAlignment = Alignment.Center) {
                        if (done) Icon(Icons.Filled.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                    }
                    Spacer(Modifier.width(12.dp))
                    Text(s, style = MaterialTheme.typography.bodyMedium, color = if (done) MaterialTheme.colorScheme.onSurface else AppTheme.colors.textMuted)
                }
            }
        }
    }
}
