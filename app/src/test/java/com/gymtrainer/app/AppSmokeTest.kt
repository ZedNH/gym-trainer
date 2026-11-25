package com.gymtrainer.app

import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.swipe
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/** Drives the real app end to end: onboarding, dock tabs, nutrition, videos, home and gym workouts. */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [34], qualifiers = "w411dp-h891dp-xxhdpi")
class AppSmokeTest {

    private val compose = createAndroidComposeRule<MainActivity>()

    @get:Rule
    val chain: RuleChain = RuleChain.outerRule(UiFlow.setupRule()).around(compose)

    @Test
    fun endToEnd() {
        val ui = UiFlow(compose)
        ui.onboard()

        // First-run guide explains the app and goes away for good.
        ui.waitFor("How Gym Trainer works")
        ui.scrollTo("How Gym Trainer works", "Got it"); ui.click("Got it"); ui.settle()
        assert(!ui.exists("How Gym Trainer works"))

        // Swipe between tabs: Today -> Workouts -> Nutrition, then back.
        compose.onRoot().performTouchInput { swipe(Offset(right * 0.9f, bottom * 0.12f), Offset(right * 0.1f, bottom * 0.12f), 250) }
        ui.waitFor("Plans, programs and single workouts", substring = true); ui.settle()
        compose.onRoot().performTouchInput { swipe(Offset(right * 0.9f, bottom * 0.12f), Offset(right * 0.1f, bottom * 0.12f), 250) }
        ui.waitFor("Meals, water and calories", substring = true); ui.settle()
        compose.onRoot().performTouchInput { swipe(Offset(right * 0.1f, bottom * 0.12f), Offset(right * 0.9f, bottom * 0.12f), 250) }
        ui.waitFor("Plans, programs and single workouts", substring = true); ui.settle()
        ui.clickDesc("Today"); ui.settle(); ui.swipeDownUntil("Today's workout")

        // Dock tabs.
        ui.clickDesc("Workouts"); ui.waitFor("Plans"); ui.settle()
        ui.click("Gym"); ui.waitFor("Programs"); ui.settle()
        ui.clickDesc("Nutrition"); ui.waitFor("kcal left"); ui.settle()
        ui.clickDesc("Progress"); ui.waitFor("kcal burned"); ui.settle()
        ui.clickDesc("Today"); ui.waitFor("This week"); ui.settle()

        // Profile via the avatar.
        ui.clickDesc("Open profile"); ui.waitFor("Workout settings"); ui.settle()
        ui.clickDesc("Back"); ui.waitFor("This week"); ui.settle()

        // Nutrition: log a food and water.
        ui.clickDesc("Nutrition"); ui.waitFor("Breakfast"); ui.settle()
        ui.click("Add")
        ui.waitFor("Add to", substring = true)
        ui.click("Rice, white (cooked)")
        ui.waitFor("Save to Breakfast")
        ui.click("Save to Breakfast")
        ui.waitFor("1 × 1 cup", substring = true)
        ui.click("+250 ml")
        ui.waitFor("0.25 /", substring = true)

        // Quick actions sheet from the dock.
        ui.clickDesc("Quick actions"); ui.waitFor("Quick actions"); ui.settle()
        ui.click("Exercises"); ui.waitFor("Exercise library"); ui.settle()

        // Library → exercise → in-app video tutorials.
        compose.onAllNodes(hasSetTextAction()).onFirst().performTextInput("pushup")
        ui.settle()
        ui.click("Push-Up")
        ui.waitFor("Video tutorials")
        ui.click("Video tutorials")
        ui.waitFor("Verified tutorials")
        ui.waitFor("Paste link")
        ui.clickDesc("Back"); ui.settle()
        ui.clickDesc("Back"); ui.settle()
        ui.clickDesc("Back"); ui.settle()

        // Guided home workout from Recommended.
        ui.clickDesc("Today"); ui.waitFor("This week"); ui.settle()
        ui.scrollTo("This week", "Full Body Beginner")
        ui.click("Full Body Beginner")
        ui.waitFor("Start workout")
        ui.completeGuidedWorkout()
        ui.click("Finish")
        ui.waitFor("Full Body Beginner"); ui.settle()

        // Gym: log a set from today's plan.
        ui.clickDesc("Workouts"); ui.waitFor("Plans"); ui.settle()
        ui.click("Gym"); ui.waitFor("YOUR GYM PLAN"); ui.settle()
        ui.click("Start session")
        ui.waitFor("Start workout"); ui.click("Start workout")
        ui.waitFor("EXERCISE 1 OF", substring = true)
        val fields = compose.onAllNodes(hasSetTextAction())
        fields[0].performTextInput("40")
        fields[1].performTextInput("10")
        compose.onAllNodesWithContentDescription("Log set").onFirst().performClick()
        ui.waitFor("How did that set feel?")
        ui.click("Appropriate")
        ui.waitFor("1 set logged", substring = true)
        ui.click("Finish")
        ui.waitFor("Finish workout?")
        ui.click("Save & finish")
        ui.waitFor("Session complete!")
        ui.click("Done")

        // Progress shows achievements and the record.
        ui.waitFor("Workouts"); ui.settle()
        ui.clickDesc("Progress"); ui.waitFor("kcal burned"); ui.settle()
        ui.scrollTo("kcal burned", "Body measurements")
        ui.click("Body measurements")
        ui.waitFor("Waist")
        ui.clickDesc("Back"); ui.waitFor("Body measurements")
    }

    @Test
    fun achievementsUnlockAfterWorkout() {
        val ui = UiFlow(compose)
        ui.onboard()
        ui.scrollTo("This week", "Full Body Beginner")
        ui.click("Full Body Beginner")
        ui.waitFor("Start workout")
        ui.completeGuidedWorkout()
        ui.click("Finish"); ui.settle()
        ui.clickDesc("Progress"); ui.waitFor("kcal burned"); ui.settle()
        ui.swipeUntil("Unlocked ✓")
        ui.waitFor("First step")
    }
}
