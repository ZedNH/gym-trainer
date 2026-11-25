package com.gymtrainer.app.ui.onboarding

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.gymtrainer.app.data.GymRepository
import com.gymtrainer.app.data.UserProfileEntity
import com.gymtrainer.app.domain.BodyArea
import com.gymtrainer.app.domain.EquipmentProfile
import com.gymtrainer.app.domain.Experience
import com.gymtrainer.app.domain.Goal
import com.gymtrainer.app.domain.Limitation
import com.gymtrainer.app.domain.TrainingPlace
import com.gymtrainer.app.domain.Units

class OnboardingViewModel(private val repo: GymRepository) : ViewModel() {
    var name by mutableStateOf("")
    var goal by mutableStateOf(Goal.MUSCLE_GAIN)
    var place by mutableStateOf(TrainingPlace.BOTH)
    var experience by mutableStateOf(Experience.BEGINNER)
    var focus by mutableStateOf(setOf(BodyArea.FULL_BODY))
    var units by mutableStateOf(Units.METRIC)
    var age by mutableIntStateOf(25)
    var heightCm by mutableDoubleStateOf(170.0)
    var weightKg by mutableDoubleStateOf(70.0)
    var daysPerWeek by mutableIntStateOf(3)
    var sessionMinutes by mutableIntStateOf(30)
    var equipment by mutableStateOf(EquipmentProfile.FULL_GYM)
    var limitations by mutableStateOf(setOf<Limitation>())
    var notes by mutableStateOf("")

    /** Equipment choices that make sense for where the user trains. */
    fun equipmentOptions(): List<EquipmentProfile> = when (place) {
        TrainingPlace.HOME -> listOf(EquipmentProfile.BODYWEIGHT, EquipmentProfile.DUMBBELLS, EquipmentProfile.HOME_GYM)
        TrainingPlace.GYM -> listOf(EquipmentProfile.FULL_GYM, EquipmentProfile.BASIC_GYM)
        TrainingPlace.BOTH -> listOf(EquipmentProfile.FULL_GYM, EquipmentProfile.BASIC_GYM, EquipmentProfile.HOME_GYM, EquipmentProfile.DUMBBELLS)
    }

    fun onPlaceChanged(p: TrainingPlace) {
        place = p
        if (equipment !in equipmentOptions()) equipment = equipmentOptions().first()
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

    suspend fun save() {
        val limitText = (limitations.map { "${it.label.lowercase()} (${it.keyword})" } + notes.trim())
            .filter { it.isNotBlank() }.joinToString(", ")
        val profile = UserProfileEntity(
            id = 1,
            name = name.trim().ifBlank { "Athlete" },
            age = age,
            heightCm = heightCm,
            weightKg = weightKg,
            targetWeightKg = null,
            units = units.name,
            experience = experience.name,
            goal = goal.name,
            secondaryGoal = null,
            daysPerWeek = daysPerWeek,
            sessionMinutes = sessionMinutes,
            equipment = equipment.name,
            limitations = limitText,
            preferences = "",
            createdAt = System.currentTimeMillis(),
            trainingPlace = place.name,
            focusAreas = focus.joinToString(",") { it.name }
        )
        repo.saveProfile(profile)
        repo.generateAndSavePlan(profile)
        repo.addMeasurement("WEIGHT", weightKg)
    }
}
