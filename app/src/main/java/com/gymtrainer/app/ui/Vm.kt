package com.gymtrainer.app.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.gymtrainer.app.GymApp
import com.gymtrainer.app.data.GymRepository

@Composable
fun rememberRepository(): GymRepository =
    (LocalContext.current.applicationContext as GymApp).repository

@Composable
fun rememberVideos(): com.gymtrainer.app.data.VideoRepository =
    (LocalContext.current.applicationContext as GymApp).videos

@Composable
fun rememberNutrition(): com.gymtrainer.app.data.NutritionRepository =
    (LocalContext.current.applicationContext as GymApp).nutrition

@Composable
fun rememberSettings(): com.gymtrainer.app.data.Settings {
    val ctx = LocalContext.current
    return androidx.compose.runtime.remember { com.gymtrainer.app.data.Settings(ctx) }
}

/** The read-only exercise library as an id → entity map (empty until loaded). */
@Composable
fun rememberExerciseMap(): Map<String, com.gymtrainer.app.data.ExerciseEntity> {
    val repo = rememberRepository()
    val state = androidx.compose.runtime.produceState(initialValue = emptyMap<String, com.gymtrainer.app.data.ExerciseEntity>(), repo) {
        value = repo.exerciseMap()
    }
    return state.value
}

/** Build a ViewModel wired to the app repository without a DI framework. */
@Composable
inline fun <reified VM : ViewModel> gymViewModel(
    crossinline create: (GymRepository) -> VM
): VM {
    val repo = rememberRepository()
    return viewModel(factory = viewModelFactory { initializer { create(repo) } })
}
