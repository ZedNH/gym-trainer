package com.gymtrainer.app.ui.profile

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gymtrainer.app.data.GymRepository
import com.gymtrainer.app.domain.BodyArea
import com.gymtrainer.app.domain.EquipmentProfile
import com.gymtrainer.app.domain.Experience
import com.gymtrainer.app.domain.Goal
import com.gymtrainer.app.domain.Limitation
import com.gymtrainer.app.domain.TrainingPlace
import com.gymtrainer.app.domain.Units
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ProfileViewModel(private val repo: GymRepository) : ViewModel() {
    var loaded by mutableStateOf(false)
        private set
    var name by mutableStateOf("")
    var goal by mutableStateOf(Goal.MUSCLE_GAIN)
    var experience by mutableStateOf(Experience.BEGINNER)
    var place by mutableStateOf(TrainingPlace.BOTH)
    var focus by mutableStateOf(setOf(BodyArea.FULL_BODY))
    var daysPerWeek by mutableIntStateOf(3)
    var sessionMinutes by mutableIntStateOf(30)
    var equipment by mutableStateOf(EquipmentProfile.FULL_GYM)
    var units by mutableStateOf(Units.METRIC)
    var limitations by mutableStateOf(setOf<Limitation>())
    var notes by mutableStateOf("")
    var age by mutableIntStateOf(0)
    var heightCm by mutableDoubleStateOf(0.0)
    var weightKg by mutableDoubleStateOf(0.0)

    init { reload() }

    fun reload() {
        viewModelScope.launch {
            repo.getProfile()?.let { p ->
                name = p.name
                goal = Goal.from(p.goal)
                experience = Experience.from(p.experience)
                place = TrainingPlace.from(p.trainingPlace)
                focus = p.focusAreas.split(",").mapNotNull { n -> BodyArea.entries.firstOrNull { it.name == n.trim() } }.toSet()
                    .ifEmpty { setOf(BodyArea.FULL_BODY) }
                daysPerWeek = p.daysPerWeek
                sessionMinutes = p.sessionMinutes
                equipment = EquipmentProfile.from(p.equipment)
                units = Units.from(p.units)
                limitations = Limitation.parse(p.limitations)
                val tagPhrases = Limitation.entries.map { "${it.label.lowercase()} (${it.keyword})" }.toSet()
                notes = p.limitations.split(", ").filter { it.isNotBlank() && it !in tagPhrases }.joinToString(", ")
                age = p.age
                heightCm = p.heightCm
                weightKg = repo.measurements("WEIGHT").lastOrNull()?.value ?: p.weightKg
            }
            loaded = true
        }
    }

    fun toggleFocus(a: BodyArea) {
        focus = when {
            a == BodyArea.FULL_BODY -> setOf(BodyArea.FULL_BODY)
            a in focus -> (focus - a).ifEmpty { setOf(BodyArea.FULL_BODY) }
            else -> focus - BodyArea.FULL_BODY + a
        }
    }

    fun toggleLimitation(l: Limitation?) {
        limitations = when {
            l == null -> emptySet()
            l in limitations -> limitations - l
            else -> limitations + l
        }
    }

    fun saveAndRegenerate(onDone: () -> Unit) {
        viewModelScope.launch {
            val current = repo.getProfile() ?: return@launch
            val limitText = (limitations.map { "${it.label.lowercase()} (${it.keyword})" } + notes.trim())
                .filter { it.isNotBlank() }.joinToString(", ")
            val updated = current.copy(
                name = name.trim().ifBlank { current.name },
                goal = goal.name,
                experience = experience.name,
                daysPerWeek = daysPerWeek,
                sessionMinutes = sessionMinutes,
                equipment = equipment.name,
                units = units.name,
                limitations = limitText,
                trainingPlace = place.name,
                focusAreas = focus.joinToString(",") { it.name }
            )
            repo.saveProfile(updated)
            repo.generateAndSavePlan(updated)
            withContext(Dispatchers.Main) { onDone() }
        }
    }

    fun saveUnits(u: Units) {
        units = u
        viewModelScope.launch {
            val current = repo.getProfile() ?: return@launch
            repo.saveProfile(current.copy(units = u.name))
        }
    }
}
