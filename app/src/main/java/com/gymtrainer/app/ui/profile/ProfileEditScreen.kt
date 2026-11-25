package com.gymtrainer.app.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.gymtrainer.app.domain.BodyArea
import com.gymtrainer.app.domain.EquipmentProfile
import com.gymtrainer.app.domain.Experience
import com.gymtrainer.app.domain.Goal
import com.gymtrainer.app.domain.Limitation
import com.gymtrainer.app.domain.TrainingPlace
import com.gymtrainer.app.ui.components.AppCard
import com.gymtrainer.app.ui.components.AppTextField
import com.gymtrainer.app.ui.components.DetailTopBar
import com.gymtrainer.app.ui.components.FilterPill
import com.gymtrainer.app.ui.components.Gutter
import com.gymtrainer.app.ui.components.PrimaryButton
import com.gymtrainer.app.ui.gymViewModel
import com.gymtrainer.app.ui.theme.AppTheme

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ProfileEditScreen(onBack: () -> Unit, onSaved: () -> Unit) {
    val vm = gymViewModel { ProfileViewModel(it) }
    var saving by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).imePadding()) {
        DetailTopBar("Training profile", onBack)
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = Gutter),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text("Changes rebuild your gym plan and personal home plan.", style = MaterialTheme.typography.bodyMedium, color = AppTheme.colors.textMuted)
            Group("Name") { AppTextField(vm.name, { vm.name = it.take(24) }, "Your name") }
            Group("Goal") { Goal.entries.forEach { g -> FilterPill(g.label, vm.goal == g) { vm.goal = g } } }
            Group("Fitness level") { Experience.entries.forEach { e -> FilterPill(e.label, vm.experience == e) { vm.experience = e } } }
            Group("Where you train") {
                TrainingPlace.entries.forEach { p -> FilterPill(p.label, vm.place == p) { vm.place = p } }
            }
            Group("Focus areas") { BodyArea.entries.forEach { a -> FilterPill(a.label, a in vm.focus) { vm.toggleFocus(a) } } }
            Group("Days per week") { (2..6).forEach { d -> FilterPill("$d days", vm.daysPerWeek == d) { vm.daysPerWeek = d } } }
            Group("Time per workout") { listOf(15, 20, 30, 45, 60, 75).forEach { m -> FilterPill("$m min", vm.sessionMinutes == m) { vm.sessionMinutes = m } } }
            Group("Equipment") { EquipmentProfile.entries.forEach { e -> FilterPill(e.label, vm.equipment == e) { vm.equipment = e } } }
            Group("Pain or injuries") {
                FilterPill("None", vm.limitations.isEmpty()) { vm.toggleLimitation(null) }
                Limitation.entries.forEach { l -> FilterPill(l.label, l in vm.limitations) { vm.toggleLimitation(l) } }
            }
            AppTextField(vm.notes, { vm.notes = it }, "Other notes (optional)", singleLine = false, minLines = 2)
            Spacer(Modifier.height(8.dp))
        }
        PrimaryButton(
            if (saving) "Saving…" else "Save & update my plans",
            onClick = { saving = true; vm.saveAndRegenerate(onSaved) }, enabled = vm.loaded && !saving,
            modifier = Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = Gutter, vertical = 12.dp)
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Group(title: String, content: @Composable () -> Unit) {
    AppCard {
        Text(title, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)
        Spacer(Modifier.height(10.dp))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) { content() }
    }
}
