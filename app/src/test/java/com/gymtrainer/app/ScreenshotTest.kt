package com.gymtrainer.app

import android.graphics.Bitmap
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.gymtrainer.app.ui.theme.ThemeMode
import com.gymtrainer.app.ui.theme.ThemeState
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File
import java.io.FileOutputStream

/** Renders key screens to PNG (app/build/screens) for design review. */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w393dp-h852dp-xxhdpi")
class ScreenshotTest {

    private val compose = createAndroidComposeRule<MainActivity>()

    @get:Rule
    val chain: RuleChain = RuleChain.outerRule(UiFlow.setupRule()).around(compose)

    private val outDir = File("build/screens").apply { deleteRecursively(); mkdirs() }

    private fun shot(name: String) {
        compose.mainClock.advanceTimeBy(1500)
        compose.waitForIdle()
        val view = compose.activity.window.decorView
        val bmp = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
        compose.runOnUiThread { view.draw(android.graphics.Canvas(bmp)) }
        FileOutputStream(File(outDir, "$name.png")).use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }

    @Test
    fun captureScreens() {
        val ui = UiFlow(compose)
        ui.waitFor("Welcome to Gym Trainer"); shot("01_welcome")
        ui.click("Get started"); ui.settle()
        compose.onAllNodes(hasSetTextAction()).onFirst().performTextInput("Sam"); shot("02_name")
        ui.click("Continue"); shot("03_goal")
        ui.click("Fat loss")
        ui.click("Continue"); shot("04_place")
        ui.click("Continue"); shot("05_level")
        ui.click("Continue"); ui.click("Abs"); ui.click("Legs & Butt"); shot("06_focus")
        ui.click("Continue"); shot("07_body")
        ui.click("Continue"); shot("08_schedule")
        ui.click("Continue"); shot("09_equipment")
        ui.click("Continue"); ui.click("Knees"); shot("10_limits")
        ui.click("Create my plan")
        compose.mainClock.advanceTimeBy(700); compose.waitForIdle(); shot("11_building")
        ui.waitFor("This week"); shot("20_today")
        ui.swipeUntil("Recommended for you"); ui.swipeUntil("Full Body Beginner"); shot("21_today_scrolled")

        ui.clickDesc("Workouts"); ui.waitFor("Plans"); shot("30_workouts_home")
        ui.swipeUntil("Browse by category"); ui.swipeUntil("Quick"); shot("31_workouts_home_categories")
        ui.swipeDownUntil("Gym"); ui.click("Gym"); ui.waitFor("YOUR GYM PLAN"); shot("32_workouts_gym")
        ui.swipeUntil("Browse by muscle"); ui.swipeUntil("Push · Pull · Legs"); shot("35_gym_muscles")
        ui.click("Chest"); ui.waitFor("Chest Builder"); shot("36_gym_category")
        ui.click("Chest Builder"); ui.waitFor("Start workout"); shot("37_gym_workout")
        repeat(2) { compose.onAllNodesWithContentDescription("Back").onFirst().performClick(); ui.settle() }
        ui.swipeUntil("Gym tools"); shot("38_gym_routines_tools")
        ui.swipeDownUntil("YOUR GYM PLAN"); ui.click("YOUR GYM PLAN"); ui.waitFor("Schedule"); shot("39_gym_plan")
        ui.swipeUntil("Rebuild from my profile"); shot("39b_gym_plan_bottom")
        compose.onAllNodesWithContentDescription("Back").onFirst().performClick(); ui.settle()
        ui.swipeDownUntil("Home"); ui.click("Home"); ui.waitFor("Plans"); ui.settle()
        ui.click("Your Personal Plan"); ui.waitFor("UP NEXT", substring = true); shot("33_personal_plan")
        compose.onAllNodesWithContentDescription("Back").onFirst().performClick(); ui.waitFor("Plans"); ui.settle()
        ui.swipeUntil("Dumbbell"); ui.click("HIIT"); ui.waitFor("HIIT Starter"); shot("34_category")
        ui.click("HIIT Starter"); ui.waitFor("Start workout"); shot("40_preview")
        ui.click("Start workout"); ui.waitFor("GET READY"); shot("41_player_ready")
        ui.click("Start now"); shot("42_player_work")
        ui.clickDesc("Skip"); ui.waitFor("REST"); shot("43_player_rest")
        compose.onAllNodesWithContentDescription("Quit").onFirst().performClick(); ui.waitFor("Quit workout?"); ui.click("Quit"); ui.settle()
        compose.onAllNodesWithContentDescription("Back").onFirst().performClick(); ui.settle()

        ui.clickDesc("Workouts"); ui.swipeDownUntil("Gym"); ui.click("Gym"); ui.waitFor("YOUR GYM PLAN"); ui.settle()
        ui.click("Start session"); ui.waitFor("Start workout"); shot("50_routine")
        ui.click("Start workout"); ui.waitFor("EXERCISE 1 OF", substring = true)
        val fields = compose.onAllNodes(hasSetTextAction())
        fields[0].performTextInput("40"); fields[1].performTextInput("10")
        compose.onAllNodesWithContentDescription("Log set").onFirst().performClick()
        ui.waitFor("How did that set feel?"); shot("51_effort")
        ui.click("Appropriate"); shot("52_logger")
        ui.click("Finish"); ui.click("Save & finish"); ui.waitFor("Session complete!"); shot("53_gym_done")
        ui.click("Done"); ui.settle()

        // Second session: the logger now shows a target from history and a warm-up ladder.
        ui.clickDesc("Workouts"); ui.waitFor("YOUR GYM PLAN"); ui.settle(); shot("54_gym_volume_card")
        ui.click("Start session"); ui.waitFor("Start workout"); ui.click("Start workout")
        ui.waitFor("TODAY'S TARGET", substring = true); ui.settle(); shot("54b_target")
        ui.swipeUntil("Warm-up"); shot("54c_warmup")
        ui.clickDesc("Finish workout"); ui.waitFor("Finish workout?"); ui.click("Leave"); ui.settle()
        ui.clickDesc("Workouts"); ui.waitFor("YOUR GYM PLAN"); ui.settle()
        ui.click("YOUR GYM PLAN"); ui.waitFor("Schedule"); ui.swipeUntil("Recovery"); shot("54d_plan_recovery")
        ui.click("This week's sets per muscle"); ui.waitFor("Needs more", substring = true); shot("54e_volume")
        repeat(2) { compose.onAllNodesWithContentDescription("Back").onFirst().performClick(); ui.settle() }

        // Nutrition
        ui.clickDesc("Nutrition"); ui.waitFor("kcal left"); shot("55_nutrition_empty")
        ui.click("Add"); ui.waitFor("Add to", substring = true); shot("56_add_food")
        ui.click("Rice, white (cooked)"); ui.waitFor("Save to Breakfast"); shot("57_food_amount")
        ui.click("Save to Breakfast"); ui.settle()
        ui.click("+250 ml"); ui.click("+250 ml"); ui.settle(); shot("58_nutrition_logged")

        // Quick actions → library → detail → videos
        ui.clickDesc("Quick actions"); ui.waitFor("Quick actions"); shot("59_quick_actions")
        ui.click("Exercises"); ui.waitFor("Exercise library"); shot("60_library")
        compose.onAllNodes(hasSetTextAction()).onFirst().performTextInput("squat"); ui.settle()
        ui.click("Barbell Squat"); ui.waitFor("How to do it"); shot("61_exercise_detail")
        ui.click("Video tutorials"); ui.waitFor("Verified tutorials"); shot("62_videos")
        repeat(3) { compose.onAllNodesWithContentDescription("Back").onFirst().performClick(); ui.settle() }

        ui.clickDesc("Progress"); ui.waitFor("kcal burned"); shot("70_progress")
        ui.swipeUntil("Achievements"); ui.swipeUntil("Unlocked"); shot("71_achievements")
        ui.swipeDownUntil("Body measurements"); ui.click("Body measurements"); ui.waitFor("Waist"); shot("72_body")
        compose.onAllNodesWithContentDescription("Back").onFirst().performClick(); ui.settle()

        ui.clickDesc("Today"); ui.waitFor("This week"); ui.swipeDownUntil("This week")
        ui.clickDesc("Open profile"); ui.waitFor("Workout settings"); shot("80_profile")
        ui.click("Edit training profile"); ui.waitFor("Training profile"); shot("81_profile_edit")
        repeat(2) { compose.onAllNodesWithContentDescription("Back").onFirst().performClick(); ui.settle() }

        ThemeState.mode = ThemeMode.DARK
        ui.waitFor("This week"); shot("90_today_dark")
        ui.clickDesc("Nutrition"); ui.waitFor("kcal left"); shot("91_nutrition_dark")
        ThemeState.mode = ThemeMode.LIGHT
    }
}
