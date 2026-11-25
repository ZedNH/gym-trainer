package com.gymtrainer.app.ui.exercises

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gymtrainer.app.data.ExerciseEntity
import com.gymtrainer.app.data.GymRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

class ExercisesViewModel(repo: GymRepository) : ViewModel() {
    val exercises: StateFlow<List<ExerciseEntity>> =
        repo.observeExercises().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
}
