package com.gymtrainer.app

import androidx.compose.ui.test.SemanticsNodeInteractionsProvider
import androidx.compose.ui.test.hasAnyDescendant
import androidx.compose.ui.test.hasScrollToNodeAction
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.AndroidComposeTestRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.swipeUp
import androidx.compose.ui.test.swipeDown
import androidx.test.core.app.ApplicationProvider
import androidx.room.Room
import com.gymtrainer.app.data.AppDatabase
import com.gymtrainer.app.ui.components.MotionPrefs
import org.junit.rules.TestWatcher
import org.junit.runner.Description

/** Shared helpers for the Robolectric UI tests. */
class UiFlow(private val c: AndroidComposeTestRule<*, *>) {

    companion object {
        /** Synchronous in-memory DB so Room work stays on the main thread; no looping demo animations. */
        fun setupRule() = object : TestWatcher() {
            override fun starting(description: Description) {
                MotionPrefs.animateDemos = false
                MotionPrefs.allowNetwork = false
                com.gymtrainer.app.ui.theme.ThemeState.mode = com.gymtrainer.app.ui.theme.ThemeMode.LIGHT
                val ctx = ApplicationProvider.getApplicationContext<android.content.Context>()
                AppDatabase.setInstanceForTesting(
                    Room.inMemoryDatabaseBuilder(ctx, AppDatabase::class.java)
                        .allowMainThreadQueries().setQueryExecutor { it.run() }.setTransactionExecutor { it.run() }.build()
                )
            }
        }
    }

    fun exists(text: String, substring: Boolean = false) =
        c.onAllNodesWithText(text, substring = substring).fetchSemanticsNodes().isNotEmpty()

    fun existsDesc(desc: String) = c.onAllNodesWithContentDescription(desc).fetchSemanticsNodes().isNotEmpty()

    fun waitFor(text: String, substring: Boolean = false, ms: Long = 30_000) = c.waitUntil(ms) { exists(text, substring) }

    fun click(text: String, substring: Boolean = false) = c.onAllNodesWithText(text, substring = substring).onFirst().performClick()

    fun clickDesc(desc: String) = c.onAllNodesWithContentDescription(desc).onFirst().performClick()

    fun settle() {
        c.mainClock.advanceTimeBy(1500)
        c.waitForIdle()
    }

    /** Scroll the list that currently shows [anchor] until [target] is composed. */
    fun scrollTo(anchor: String, target: String) {
        settle()
        c.onNode(hasScrollToNodeAction() and hasAnyDescendant(hasText(anchor, substring = true)))
            .performScrollToNode(hasText(target, substring = true))
        // The floating dock overlays the bottom of the screen; lift the target clear of it.
        c.onRoot().performTouchInput { swipeUp(startY = bottom * 0.6f, endY = bottom * 0.35f, durationMillis = 300) }
        settle()
    }

    /** Swipe the screen up until [target] is composed (robust to list recomposition). */
    fun swipeUntil(target: String, maxSwipes: Int = 20) {
        settle()
        var n = 0
        while (!exists(target, substring = true) && n++ < maxSwipes) {
            c.onRoot().performTouchInput { swipeUp(startY = bottom * 0.75f, endY = bottom * 0.35f, durationMillis = 300) }
            c.mainClock.advanceTimeBy(600)
            c.waitForIdle()
        }
        settle()
    }

    fun swipeDownUntil(target: String, maxSwipes: Int = 20) {
        settle()
        var n = 0
        while (!exists(target) && n++ < maxSwipes) {
            c.onRoot().performTouchInput { swipeDown(startY = bottom * 0.3f, endY = bottom * 0.8f, durationMillis = 300) }
            c.mainClock.advanceTimeBy(600)
            c.waitForIdle()
        }
        settle()
    }

    fun onboard(name: String = "Sam") {
        waitFor("Welcome to Gym Trainer")
        click("Get started")
        c.onAllNodes(hasSetTextAction()).onFirst().performTextInput(name)
        repeat(8) { click("Continue"); settle() }
        click("Create my plan")
        waitFor("This week")
        settle()
    }

    /** Runs a guided workout from the preview screen to the finish screen. */
    fun completeGuidedWorkout() {
        click("Start workout")
        waitFor("GET READY")
        click("Start now")
        var guard = 0
        while (!exists("Workout complete! 🎉") && guard++ < 120) {
            when {
                exists("Skip") -> click("Skip")
                exists("Done") -> click("Done")
                existsDesc("Skip") -> clickDesc("Skip")
            }
            c.waitForIdle()
        }
        waitFor("Workout complete! 🎉")
    }

    @Suppress("unused")
    private val provider: SemanticsNodeInteractionsProvider = c
}
