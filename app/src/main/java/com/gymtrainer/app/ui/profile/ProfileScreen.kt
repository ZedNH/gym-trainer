package com.gymtrainer.app.ui.profile

import androidx.compose.material.icons.filled.Translate
import com.gymtrainer.app.data.Updates
import androidx.compose.material.icons.filled.SystemUpdate
import kotlinx.coroutines.launch
import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.HourglassBottom
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.SmartDisplay
import androidx.compose.material.icons.filled.Straighten
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.gymtrainer.app.domain.Units
import com.gymtrainer.app.reminders.Reminders
import com.gymtrainer.app.ui.Format
import com.gymtrainer.app.ui.components.AppCard
import com.gymtrainer.app.ui.components.AppSwitch
import com.gymtrainer.app.ui.components.FilterPill
import com.gymtrainer.app.ui.components.Gutter
import com.gymtrainer.app.ui.components.PillButton
import com.gymtrainer.app.ui.components.RowDivider
import com.gymtrainer.app.ui.components.SectionHeader
import com.gymtrainer.app.ui.components.SettingRow
import com.gymtrainer.app.ui.components.TabHeader
import com.gymtrainer.app.ui.gymViewModel
import com.gymtrainer.app.ui.rememberSettings
import com.gymtrainer.app.ui.theme.AppTheme
import com.gymtrainer.app.ui.theme.Brand
import com.gymtrainer.app.ui.theme.ThemeMode
import com.gymtrainer.app.ui.theme.ThemeState
import com.gymtrainer.app.ui.tools.Calc

/** A pending single-choice setting dialog. */
private data class Choice(val title: String, val options: List<Pair<String, String>>, val selected: String, val onPick: (String) -> Unit)

@Composable
fun ProfileScreen(contentPadding: PaddingValues, onEditProfile: () -> Unit, onTools: () -> Unit, onBack: (() -> Unit)? = null) {
    val vm = gymViewModel { ProfileViewModel(it) }
    val settings = rememberSettings()
    val context = LocalContext.current
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { vm.reload() }

    var voice by remember { mutableStateOf(settings.voiceCoach) }
    var rest by remember { mutableIntStateOf(settings.homeRestSeconds) }
    var countdown by remember { mutableIntStateOf(settings.countdownSeconds) }
    var goal by remember { mutableIntStateOf(settings.weeklyGoal) }
    var reminder by remember { mutableStateOf(settings.reminderOn) }
    var hour by remember { mutableIntStateOf(settings.reminderHour) }
    var choice by remember { mutableStateOf<Choice?>(null) }
    var showAbout by remember { mutableStateOf(false) }
    var coachLang by remember { mutableStateOf(settings.coachLanguage) }
    var checking by remember { mutableStateOf(false) }
    var upToDate by remember { mutableStateOf(false) }
    var found by remember { mutableStateOf<com.gymtrainer.app.data.AppUpdate?>(null) }
    val scope = androidx.compose.runtime.rememberCoroutineScope()

    fun applyReminder(on: Boolean) { reminder = on; settings.reminderOn = on; Reminders.apply(context) }
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { applyReminder(it) }

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item { if (onBack != null) com.gymtrainer.app.ui.components.DetailTopBar("Profile", onBack) else TabHeader("Profile") }

        // Profile card
        item {
            AppCard(Modifier.padding(horizontal = Gutter)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(64.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer), contentAlignment = Alignment.Center) {
                        Text(vm.name.take(1).uppercase().ifBlank { "?" }, style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.primary)
                    }
                    Column(Modifier.weight(1f).padding(horizontal = 14.dp)) {
                        Text(vm.name, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurface)
                        Text("${vm.goal.label} · ${vm.experience.label}", style = MaterialTheme.typography.bodySmall, color = AppTheme.colors.textMuted)
                        Text("${vm.place.label} · ${vm.daysPerWeek}×/week · ${vm.sessionMinutes} min", style = MaterialTheme.typography.bodySmall, color = AppTheme.colors.textMuted)
                    }
                }
                Spacer(Modifier.height(14.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    BodyStat("Height", Format.height(vm.heightCm, vm.units))
                    BodyStat("Weight", Format.weight(vm.weightKg, vm.units))
                    BodyStat("BMI", Calc.bmi(vm.weightKg, vm.heightCm).let { if (it > 0) Format.trim(Math.round(it * 10) / 10.0) else "—" })
                    BodyStat("Age", "${vm.age}")
                }
                Spacer(Modifier.height(14.dp))
                PillButton("Edit training profile", onEditProfile, icon = Icons.Filled.Edit)
            }
        }

        // Workout settings
        item { SectionHeader("Workout settings") }
        item {
            AppCard(Modifier.padding(horizontal = Gutter), padding = PaddingValues(vertical = 4.dp)) {
                SettingRow(Icons.Filled.RecordVoiceOver, Brand.Orange, "Voice coach", trailing = {
                    AppSwitch(voice) { voice = it; settings.voiceCoach = it }
                })
                RowDivider()
                SettingRow(Icons.Filled.Translate, Brand.Teal, "Coach language",
                    com.gymtrainer.app.domain.CoachLanguage.from(coachLang).label, onClick = {
                    choice = Choice("Coach language", com.gymtrainer.app.domain.CoachLanguage.entries.map { it.name to it.label }, coachLang) {
                        coachLang = it; settings.coachLanguage = it
                    }
                })
                RowDivider()
                SettingRow(Icons.Filled.HourglassBottom, Brand.Blue, "Rest between exercises", "${rest}s", onClick = {
                    choice = Choice("Rest between exercises", listOf(10, 20, 30, 45, 60).map { "$it" to "${it}s" }, "$rest") {
                        rest = it.toInt(); settings.homeRestSeconds = rest
                    }
                })
                RowDivider()
                SettingRow(Icons.Filled.Timer, Brand.Purple, "Get-ready countdown", "${countdown}s", onClick = {
                    choice = Choice("Get-ready countdown", listOf(5, 10, 15).map { "$it" to "${it}s" }, "$countdown") {
                        countdown = it.toInt(); settings.countdownSeconds = countdown
                    }
                })
                RowDivider()
                SettingRow(Icons.Filled.Flag, Brand.Green, "Weekly goal", "$goal workouts", onClick = {
                    choice = Choice("Weekly goal", (2..7).map { "$it" to "$it workouts" }, "$goal") {
                        goal = it.toInt(); settings.weeklyGoal = goal
                    }
                })
                RowDivider()
                SettingRow(Icons.Filled.Straighten, Brand.Teal, "Units", if (vm.units == Units.METRIC) "kg · cm" else "lb · ft", onClick = {
                    choice = Choice("Units", listOf(Units.METRIC.name to "Metric (kg, cm)", Units.IMPERIAL.name to "Imperial (lb, ft)"), vm.units.name) {
                        vm.saveUnits(Units.valueOf(it))
                    }
                })
                RowDivider()
                SettingRow(Icons.Filled.DarkMode, Brand.Pink, "Appearance",
                    when (ThemeState.mode) { ThemeMode.LIGHT -> "Light"; ThemeMode.DARK -> "Dark"; ThemeMode.SYSTEM -> "System" }, onClick = {
                        choice = Choice("Appearance", listOf("LIGHT" to "Light", "DARK" to "Dark", "SYSTEM" to "Follow system"), ThemeState.mode.name) {
                            ThemeState.mode = ThemeMode.valueOf(it); settings.themeMode = it
                        }
                    })
            }
        }

        // Reminders
        item { SectionHeader("Reminders") }
        item {
            AppCard(Modifier.padding(horizontal = Gutter), padding = PaddingValues(vertical = 4.dp)) {
                SettingRow(Icons.Filled.NotificationsActive, Brand.Red, "Daily reminder", trailing = {
                    AppSwitch(reminder) { on ->
                        if (on && Build.VERSION.SDK_INT >= 33 &&
                            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
                        ) permission.launch(Manifest.permission.POST_NOTIFICATIONS) else applyReminder(on)
                    }
                })
                if (reminder) {
                    RowDivider()
                    SettingRow(Icons.Filled.Alarm, Brand.Amber, "Reminder time", String.format("%02d:00", hour), onClick = {
                        choice = Choice("Reminder time", listOf(6, 7, 8, 12, 17, 18, 19, 20, 21).map { "$it" to String.format("%02d:00", it) }, "$hour") {
                            hour = it.toInt(); settings.reminderHour = hour; settings.reminderMinute = 0; Reminders.apply(context)
                        }
                    })
                }
            }
        }

        // More
        item { SectionHeader("More") }
        item {
            AppCard(Modifier.padding(horizontal = Gutter), padding = PaddingValues(vertical = 4.dp)) {
                SettingRow(Icons.Filled.Calculate, Brand.Orange, "Gym tools", "1RM · plates · BMI", onClick = onTools)
                RowDivider()
                SettingRow(Icons.Filled.Info, Brand.Blue, "About & credits", onClick = { showAbout = true })
                RowDivider()
                SettingRow(Icons.Filled.SystemUpdate, Brand.Green, "App updates",
                    when { checking -> "Checking…"; upToDate -> "v${Updates.currentVersion(context)} · up to date"; else -> "v${Updates.currentVersion(context)} · check now" },
                    onClick = {
                        if (!checking) { checking = true; upToDate = false
                            scope.launch { val u = Updates.check(context); checking = false; if (u != null) found = u else upToDate = true } }
                    })
            }
        }
        item {
            Text("Your data stays on this device. Guidance here is not medical advice.", style = MaterialTheme.typography.bodySmall,
                color = AppTheme.colors.textFaint, modifier = Modifier.padding(horizontal = Gutter))
        }
    }

    choice?.let { c -> ChoiceDialog(c) { choice = null } }

    found?.let { com.gymtrainer.app.ui.components.UpdateDialog(it) { found = null } }

    if (showAbout) {
        AlertDialog(
            onDismissRequest = { showAbout = false },
            confirmButton = { TextButton(onClick = { showAbout = false }) { Text("Close") } },
            title = { Text("Gym Trainer") },
            text = {
                Text("Exercise photos and instructions: free-exercise-db by yuhonas (public domain, Unlicense).\n\n" +
                    "Typeface: Poppins (SIL Open Font License).\n\n" +
                    "Videos: YouTube's official embedded player, streamed from YouTube. Videos belong to their creators.\n\n" +
                    "Workouts, plans, challenges and app code are original to this app.")
            }
        )
    }
}

@Composable
private fun BodyStat(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
        Text(label, style = MaterialTheme.typography.bodySmall, color = AppTheme.colors.textMuted)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ChoiceDialog(c: Choice, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { TextButton(onClick = onDismiss) { Text("Done") } },
        title = { Text(c.title, style = MaterialTheme.typography.titleLarge) },
        text = {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                c.options.forEach { (value, label) -> FilterPill(label, value == c.selected) { c.onPick(value); onDismiss() } }
            }
        },
        containerColor = AppTheme.colors.card
    )
}
