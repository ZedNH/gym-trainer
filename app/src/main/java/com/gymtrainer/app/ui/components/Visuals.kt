package com.gymtrainer.app.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessibilityNew
import androidx.compose.material.icons.filled.Apartment
import androidx.compose.material.icons.filled.Autorenew
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CalendarViewWeek
import androidx.compose.material.icons.filled.DirectionsRun
import androidx.compose.material.icons.filled.DirectionsWalk
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.FrontHand
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.SignalCellularAlt
import androidx.compose.material.icons.filled.SignalCellularAlt1Bar
import androidx.compose.material.icons.filled.SignalCellularAlt2Bar
import androidx.compose.material.icons.filled.SportsGymnastics
import androidx.compose.material.icons.filled.Store
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.ViewAgenda
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.gymtrainer.app.domain.BodyArea
import com.gymtrainer.app.domain.EquipmentProfile
import com.gymtrainer.app.domain.Experience
import com.gymtrainer.app.domain.Goal
import com.gymtrainer.app.domain.GymCategory
import com.gymtrainer.app.domain.GymProgram
import com.gymtrainer.app.domain.PlanGoal
import com.gymtrainer.app.domain.TrainingPlace
import com.gymtrainer.app.domain.WorkoutCategory
import com.gymtrainer.app.ui.theme.Brand

object Visuals {
    fun category(c: WorkoutCategory): Pair<ImageVector, Color> = when (c) {
        WorkoutCategory.FULL_BODY -> Icons.Filled.AccessibilityNew to Brand.Blue
        WorkoutCategory.ABS -> Icons.Filled.ViewAgenda to Brand.Orange
        WorkoutCategory.CHEST -> Icons.Filled.Shield to Brand.Purple
        WorkoutCategory.ARM -> Icons.Filled.FrontHand to Brand.Pink
        WorkoutCategory.LEG -> Icons.Filled.DirectionsWalk to Brand.Teal
        WorkoutCategory.SHOULDER_BACK -> Icons.Filled.SportsGymnastics to Brand.Green
        WorkoutCategory.HIIT -> Icons.Filled.Bolt to Brand.Red
        WorkoutCategory.CARDIO -> Icons.Filled.DirectionsRun to Brand.Orange
        WorkoutCategory.MOBILITY -> Icons.Filled.SelfImprovement to Brand.Teal
        WorkoutCategory.DUMBBELL -> Icons.Filled.FitnessCenter to Brand.Purple
        WorkoutCategory.QUICK -> Icons.Filled.Timer to Brand.Blue
        WorkoutCategory.KETTLEBELL_BANDS -> Icons.Filled.SportsGymnastics to Brand.Orange
        WorkoutCategory.LOW_IMPACT -> Icons.Filled.DirectionsWalk to Brand.Green
    }

    fun gymCategory(c: GymCategory): Pair<ImageVector, Color> = when (c) {
        GymCategory.CHEST -> Icons.Filled.Shield to Brand.Purple
        GymCategory.BACK -> Icons.Filled.SportsGymnastics to Brand.Green
        GymCategory.SHOULDERS -> Icons.Filled.AccessibilityNew to Brand.Blue
        GymCategory.ARMS -> Icons.Filled.FrontHand to Brand.Pink
        GymCategory.LEGS -> Icons.Filled.DirectionsWalk to Brand.Teal
        GymCategory.GLUTES -> Icons.Filled.LocalFireDepartment to Brand.Orange
        GymCategory.CORE -> Icons.Filled.ViewAgenda to Brand.Amber
        GymCategory.FULL_BODY -> Icons.Filled.FitnessCenter to Brand.Red
        GymCategory.SPLIT -> Icons.Filled.CalendarViewWeek to Brand.BlueLight
    }

    /** Colour + icon for a multi-week gym program, from what it emphasises. */
    fun gymProgram(p: GymProgram): Pair<ImageVector, Color> {
        val t = p.title.lowercase()
        return when {
            "strength" in t || "5×5" in t || "power" in t -> Icons.Filled.EmojiEvents to Brand.Amber
            "glute" in t -> Icons.Filled.LocalFireDepartment to Brand.Pink
            "lean" in t || "tone" in t -> Icons.Filled.Bolt to Brand.Purple
            "machine" in t -> Icons.Filled.Apartment to Brand.Teal
            "dumbbell" in t -> Icons.Filled.FitnessCenter to Brand.Orange
            "push" in t || "ppl" in t || "split" in t -> Icons.Filled.CalendarViewWeek to Brand.Blue
            "upper" in t -> Icons.Filled.SwapHoriz to Brand.Green
            else -> Icons.Filled.AccessibilityNew to Brand.Blue
        }
    }

    fun gradient(c: Color): List<Color> = listOf(c, c.copy(red = (c.red * 0.75f), green = (c.green * 0.75f), blue = (c.blue * 0.9f)))

    fun area(a: BodyArea): Pair<ImageVector, Color> = when (a) {
        BodyArea.FULL_BODY -> category(WorkoutCategory.FULL_BODY)
        BodyArea.ABS -> category(WorkoutCategory.ABS)
        BodyArea.CHEST -> category(WorkoutCategory.CHEST)
        BodyArea.ARM -> category(WorkoutCategory.ARM)
        BodyArea.LEG -> category(WorkoutCategory.LEG)
        BodyArea.SHOULDER_BACK -> category(WorkoutCategory.SHOULDER_BACK)
    }

    fun goal(g: Goal): Pair<ImageVector, Color> = when (g) {
        Goal.MUSCLE_GAIN -> Icons.Filled.FitnessCenter to Brand.Blue
        Goal.LEAN_MUSCLE -> Icons.Filled.Bolt to Brand.Purple
        Goal.FAT_LOSS -> Icons.Filled.LocalFireDepartment to Brand.Orange
        Goal.RECOMPOSITION -> Icons.Filled.Autorenew to Brand.Teal
        Goal.STRENGTH -> Icons.Filled.EmojiEvents to Brand.Amber
        Goal.GENERAL_FITNESS -> Icons.Filled.Favorite to Brand.Pink
    }

    fun planGoal(g: PlanGoal): Pair<ImageVector, Color> = when (g) {
        PlanGoal.LOSE_WEIGHT -> Icons.Filled.LocalFireDepartment to Brand.Orange
        PlanGoal.BUILD_MUSCLE -> Icons.Filled.FitnessCenter to Brand.Blue
        PlanGoal.TONE -> Icons.Filled.Bolt to Brand.Purple
        PlanGoal.FLEXIBILITY -> Icons.Filled.SelfImprovement to Brand.Teal
        PlanGoal.GENERAL -> Icons.Filled.Favorite to Brand.Pink
    }

    fun place(p: TrainingPlace): Pair<ImageVector, Color> = when (p) {
        TrainingPlace.HOME -> Icons.Filled.Home to Brand.Teal
        TrainingPlace.GYM -> Icons.Filled.FitnessCenter to Brand.Blue
        TrainingPlace.BOTH -> Icons.Filled.SwapHoriz to Brand.Purple
    }

    fun experience(e: Experience): Pair<ImageVector, Color> = when (e) {
        Experience.BEGINNER -> Icons.Filled.SignalCellularAlt1Bar to Brand.Green
        Experience.INTERMEDIATE -> Icons.Filled.SignalCellularAlt2Bar to Brand.Amber
        Experience.ADVANCED -> Icons.Filled.SignalCellularAlt to Brand.Red
        Experience.RETURNING -> Icons.Filled.History to Brand.Blue
    }

    fun equipment(e: EquipmentProfile): Pair<ImageVector, Color> = when (e) {
        EquipmentProfile.FULL_GYM -> Icons.Filled.Apartment to Brand.Blue
        EquipmentProfile.BASIC_GYM -> Icons.Filled.Store to Brand.Purple
        EquipmentProfile.HOME_GYM -> Icons.Filled.Home to Brand.Teal
        EquipmentProfile.DUMBBELLS -> Icons.Filled.FitnessCenter to Brand.Orange
        EquipmentProfile.BODYWEIGHT -> Icons.Filled.AccessibilityNew to Brand.Green
    }
}
