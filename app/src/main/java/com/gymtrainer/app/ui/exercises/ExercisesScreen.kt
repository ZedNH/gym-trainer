package com.gymtrainer.app.ui.exercises

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.gymtrainer.app.data.ExerciseEntity
import com.gymtrainer.app.domain.Difficulty
import com.gymtrainer.app.domain.MuscleGroup
import com.gymtrainer.app.ui.components.AppTextField
import com.gymtrainer.app.ui.components.ExerciseImage
import com.gymtrainer.app.ui.components.FilterPill
import com.gymtrainer.app.ui.components.Gutter
import com.gymtrainer.app.ui.components.LevelBadge
import com.gymtrainer.app.ui.components.ListRow
import com.gymtrainer.app.ui.components.PrimaryButton
import com.gymtrainer.app.ui.components.TabHeader
import com.gymtrainer.app.ui.gymViewModel
import com.gymtrainer.app.ui.navigation.Routes
import com.gymtrainer.app.ui.theme.AppTheme

private val categories = listOf(
    "strength" to "Strength", "stretching" to "Stretching", "cardio" to "Cardio",
    "plyometrics" to "Plyometrics", "powerlifting" to "Powerlifting", "olympic weightlifting" to "Olympic"
)
private val equipmentOptions = listOf("Bodyweight", "Dumbbell", "Barbell", "Machine", "Cable", "Kettlebell", "Bands", "Medicine Ball", "Exercise Ball", "Other")

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ExercisesScreen(contentPadding: PaddingValues, nav: NavController, onBack: (() -> Unit)? = null) {
    val vm = gymViewModel { ExercisesViewModel(it) }
    val all by vm.exercises.collectAsStateWithLifecycle()
    var query by rememberSaveable { mutableStateOf("") }
    var group by rememberSaveable { mutableStateOf<String?>(null) }
    var equipment by rememberSaveable { mutableStateOf<String?>(null) }
    var category by rememberSaveable { mutableStateOf<String?>(null) }
    var showFilters by remember { mutableStateOf(false) }
    val activeFilters = listOfNotNull(equipment, category).size

    val filtered = remember(all, query, group, equipment, category) {
        all.filter { ex ->
            (query.isBlank() || ex.name.contains(query, true) || ex.primaryMuscles.contains(query, true)) &&
                (group == null || ex.muscleGroup == group) &&
                (equipment == null || ex.equipment == equipment) &&
                (category == null || ex.category == category)
        }.sortedWith(compareByDescending<ExerciseEntity> { it.featured }.thenBy { it.name })
    }

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            if (onBack != null) com.gymtrainer.app.ui.components.DetailTopBar("Exercise library", onBack)
            else TabHeader("Library", "${all.size} exercises")
        }
        item {
            Row(Modifier.padding(horizontal = Gutter), verticalAlignment = Alignment.CenterVertically) {
                AppTextField(query, { query = it }, "Search exercises", Modifier.weight(1f),
                    leading = { Icon(Icons.Filled.Search, contentDescription = null, tint = AppTheme.colors.textMuted) })
                Spacer(Modifier.width(10.dp))
                Box(
                    Modifier.size(56.dp).clip(RoundedCornerShape(16.dp))
                        .background(if (activeFilters > 0) MaterialTheme.colorScheme.primary else AppTheme.colors.card)
                        .border(1.dp, AppTheme.colors.border, RoundedCornerShape(16.dp))
                        .clickable { showFilters = true },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Filled.Tune, contentDescription = "Filters",
                        tint = if (activeFilters > 0) androidx.compose.ui.graphics.Color.White else MaterialTheme.colorScheme.onSurface)
                    if (activeFilters > 0) {
                        Box(Modifier.align(Alignment.TopEnd).padding(6.dp).size(16.dp).clip(CircleShape).background(androidx.compose.ui.graphics.Color.White),
                            contentAlignment = Alignment.Center) {
                            Text("$activeFilters", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }
        }
        item {
            LazyRow(contentPadding = PaddingValues(horizontal = Gutter), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                item { FilterPill("All", group == null) { group = null } }
                items(MuscleGroup.entries, key = { it.name }) { g -> FilterPill(g.label, group == g.name) { group = if (group == g.name) null else g.name } }
            }
        }
        item {
            Text("${filtered.size} results", style = MaterialTheme.typography.labelMedium, color = AppTheme.colors.textMuted,
                modifier = Modifier.padding(horizontal = Gutter))
        }
        items(filtered, key = { it.id }) { ex ->
            ListRow(
                title = ex.name,
                meta = "${ex.primaryMuscles} · ${ex.equipment}",
                onClick = { nav.navigate(Routes.exerciseDetail(ex.id)) },
                modifier = Modifier.padding(horizontal = Gutter),
                leading = { ExerciseImage(ex, Modifier.width(84.dp).aspectRatio(1.4f), animate = false, thumbnail = true, corner = 12.dp) },
                badge = { LevelBadge(Difficulty.from(ex.difficulty)) }
            )
        }
    }

    if (showFilters) {
        ModalBottomSheet(onDismissRequest = { showFilters = false }, containerColor = AppTheme.colors.card) {
            Column(Modifier.padding(horizontal = Gutter).navigationBarsPadding()) {
                Text("Filters", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onSurface)
                Spacer(Modifier.height(16.dp))
                Text("Type", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)
                Spacer(Modifier.height(8.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterPill("Any", category == null) { category = null }
                    categories.forEach { (v, l) -> FilterPill(l, category == v) { category = if (category == v) null else v } }
                }
                Spacer(Modifier.height(18.dp))
                Text("Equipment", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)
                Spacer(Modifier.height(8.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterPill("Any", equipment == null) { equipment = null }
                    equipmentOptions.forEach { e -> FilterPill(e, equipment == e) { equipment = if (equipment == e) null else e } }
                }
                Spacer(Modifier.height(20.dp))
                PrimaryButton("Show ${filtered.size} results", { showFilters = false }, Modifier.fillMaxWidth())
                Spacer(Modifier.height(16.dp))
            }
        }
    }
}
