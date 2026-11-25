package com.gymtrainer.app.ui.train

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gymtrainer.app.data.ExerciseEntity
import com.gymtrainer.app.data.GymRepository
import com.gymtrainer.app.ui.components.ExerciseImage
import com.gymtrainer.app.ui.components.SectionCard
import com.gymtrainer.app.ui.rememberExerciseMap
import com.gymtrainer.app.ui.rememberRepository
import com.gymtrainer.app.ui.theme.Accent
import kotlinx.coroutines.launch

private data class EditItem(val exercise: ExerciseEntity, val sets: Int, val repMin: Int, val repMax: Int)

@Composable
fun RoutineEditorScreen(onDone: () -> Unit) {
    val repo = rememberRepository()
    val library = rememberExerciseMap()
    val scope = rememberCoroutineScope()
    var name by remember { mutableStateOf("") }
    val items = remember { mutableStateListOf<EditItem>() }
    var picking by remember { mutableStateOf(false) }
    var saving by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).navigationBarsPadding().imePadding()) {
        com.gymtrainer.app.ui.components.DetailTopBar("New routine", onDone)
        LazyColumn(
            Modifier.weight(1f),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                OutlinedTextField(
                    value = name, onValueChange = { name = it }, singleLine = true,
                    label = { Text("Routine name (e.g. Chest & Triceps)") }, modifier = Modifier.fillMaxWidth()
                )
            }
            itemsIndexed(items) { i, it ->
                SectionCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        ExerciseImage(it.exercise, Modifier.width(64.dp).aspectRatio(1.5f), animate = false, thumbnail = true, corner = 8.dp)
                        Column(Modifier.weight(1f).padding(horizontal = 10.dp)) {
                            Text(it.exercise.name, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onBackground)
                            Text("${it.exercise.primaryMuscles} • ${it.exercise.equipment}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        IconButton(onClick = { items.removeAt(i) }) { Icon(Icons.Filled.Close, contentDescription = "Remove", tint = MaterialTheme.colorScheme.onSurfaceVariant) }
                    }
                    Spacer(Modifier.height(6.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Stepper("Sets", it.sets) { v -> items[i] = it.copy(sets = v.coerceIn(1, 10)) }
                        Stepper(if (it.exercise.timed) "Seconds" else "Reps", it.repMax) { v ->
                            val max = v.coerceIn(1, 300)
                            items[i] = it.copy(repMax = max, repMin = if (it.exercise.timed) max else (max - 4).coerceAtLeast(1))
                        }
                    }
                }
            }
            item {
                OutlinedButton(onClick = { picking = true }, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Filled.Add, contentDescription = null); Text("  Add exercise")
                }
            }
        }
        Button(
            onClick = {
                saving = true
                scope.launch {
                    repo.saveCustomRoutine(name.trim(), items.map {
                        GymRepository.RoutineItemDraft(it.exercise.id, it.sets, it.repMin, it.repMax, it.exercise.restSeconds)
                    })
                    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) { onDone() }
                }
            },
            enabled = items.isNotEmpty() && !saving,
            modifier = Modifier.fillMaxWidth().padding(16.dp).height(52.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Accent, contentColor = MaterialTheme.colorScheme.onPrimary)
        ) { Text("Save routine", fontWeight = FontWeight.Bold) }
    }

    if (picking) {
        ExercisePicker(
            library = library.values,
            onPick = { ex ->
                val (mn, mx) = if (ex.timed) ex.defaultSeconds.coerceAtLeast(20) to ex.defaultSeconds.coerceAtLeast(20) else ex.repMin to ex.repMax
                items += EditItem(ex, 3, mn, mx)
                picking = false
            },
            onDismiss = { picking = false }
        )
    }
}

@Composable
private fun Stepper(label: String, value: Int, onChange: (Int) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text("$label ", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        IconButton(onClick = { onChange(value - 1) }) { Icon(Icons.Filled.Remove, contentDescription = "Less", tint = Accent) }
        Text("$value", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
        IconButton(onClick = { onChange(value + 1) }) { Icon(Icons.Filled.Add, contentDescription = "More", tint = Accent) }
    }
}

@Composable
fun ExercisePicker(library: Collection<ExerciseEntity>, onPick: (ExerciseEntity) -> Unit, onDismiss: () -> Unit) {
    var query by remember { mutableStateOf("") }
    val results = remember(library, query) {
        library.filter { query.isBlank() || it.name.contains(query, true) || it.primaryMuscles.contains(query, true) || it.equipment.contains(query, true) }
            .sortedWith(compareByDescending<ExerciseEntity> { it.featured }.thenBy { it.name })
            .take(80)
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { TextButton(onClick = onDismiss) { Text("Close") } },
        title = { Text("Add exercise") },
        text = {
            Column {
                OutlinedTextField(
                    value = query, onValueChange = { query = it }, singleLine = true,
                    leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                    placeholder = { Text("Search name, muscle, equipment") }, modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                LazyColumn(Modifier.heightIn(max = 420.dp)) {
                    items(results, key = { it.id }) { ex ->
                        Row(
                            Modifier.fillMaxWidth().clickable { onPick(ex) }.padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            ExerciseImage(ex, Modifier.width(56.dp).aspectRatio(1.5f), animate = false, thumbnail = true, corner = 8.dp)
                            Column(Modifier.padding(start = 10.dp)) {
                                Text(ex.name, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onBackground, fontSize = 14.sp)
                                Text("${ex.primaryMuscles} • ${ex.equipment}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
        }
    )
}
