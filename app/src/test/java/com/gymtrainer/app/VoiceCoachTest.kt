package com.gymtrainer.app

import com.gymtrainer.app.domain.BanglaNames
import com.gymtrainer.app.domain.CoachCue
import com.gymtrainer.app.domain.CoachExercise
import com.gymtrainer.app.domain.CoachLanguage
import com.gymtrainer.app.domain.CoachLines
import com.gymtrainer.app.domain.Tips
import com.gymtrainer.app.ui.videos.playerUrl
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class VoiceCoachTest {

    private val squat = CoachExercise("Bodyweight_Squat", "Bodyweight Squat", "Squat")
    private val pushup = CoachExercise("Pushups", "Push-Up", "Horizontal Push")
    private val plank = CoachExercise("Plank", "Plank", "Stability")

    private fun hasBangla(s: String) = s.any { it in 'ঀ'..'৿' }

    @Test fun banglaNamesUseBanglaScript() {
        assertEquals("বডিওয়েট স্কোয়াট", BanglaNames.of("Bodyweight Squat"))
        assertEquals("পুশ-আপ", BanglaNames.of("Push-Up"))
        assertEquals("ডাম্বেল বাইসেপ কার্ল", BanglaNames.of("Dumbbell Bicep Curl"))
        assertEquals("বাইসেপ কার্ল বারবেল", BanglaNames.of("Bicep Curl - Barbell"))
        // Unknown words stay readable instead of disappearing.
        assertTrue(BanglaNames.of("Zottman Curl").contains("Zottman"))
    }

    @Test fun banglaDigits() {
        assertEquals("৩", CoachLines.num(3))
        assertEquals("৪৫", CoachLines.num(45))
    }

    @Test fun everyCueHasEnglishAndBanglaText() {
        val cues = listOf(
            CoachCue.Ready(squat), CoachCue.Work(squat, 12, null, false), CoachCue.Work(plank, null, 30, true),
            CoachCue.Rest(20, pushup), CoachCue.Count(3), CoachCue.Half, CoachCue.Tip(squat), CoachCue.Motivate,
            CoachCue.RestTip, CoachCue.Done, CoachCue.GymExercise(pushup, 3, 8, 12), CoachCue.SetLogged(1, 3),
            CoachCue.SetLogged(3, 3), CoachCue.RestOver
        )
        cues.forEach { cue ->
            val en = CoachLines.text(cue, CoachLanguage.ENGLISH, Random(1))
            val bn = CoachLines.text(cue, CoachLanguage.BANGLA, Random(1))
            assertTrue("$cue en", en.isNotBlank() && !hasBangla(en))
            assertTrue("$cue bn: $bn", hasBangla(bn))
        }
        assertEquals("স্কোয়াট".let { "বডিওয়েট $it। ১২ বার। শুরু করুন!" },
            CoachLines.text(CoachCue.Work(squat, 12, null, false), CoachLanguage.BANGLA))
        assertTrue(CoachLines.text(CoachCue.Work(plank, null, 30, true), CoachLanguage.BANGLA).startsWith("এটাই শেষ ব্যায়াম"))
    }

    @Test fun tipsMatchTheExercise() {
        repeat(10) { seed ->
            val r = Random(seed)
            assertTrue(Tips.forExercise(squat, CoachLanguage.ENGLISH, r) in
                listOf("Chest up, weight in your heels.", "Knees follow your toes; don't let them cave in.", "Sit back like into a chair."))
            val plankTip = Tips.forExercise(plank, CoachLanguage.BANGLA, r)
            assertTrue(plankTip, hasBangla(plankTip))
        }
        // Push-ups get push-up cues, not squat cues.
        assertFalse(Tips.forExercise(pushup, CoachLanguage.ENGLISH, Random(2)).contains("heels"))
    }

    @Test fun playerUrlPointsAtHostedPageAndSanitisesIds() {
        assertEquals("https://zednh.github.io/gym-trainer/player.html?v=dQw4w9WgXcQ", playerUrl("dQw4w9WgXcQ"))
        val evil = playerUrl("dQw4w9WgXcQ\"><script>")
        assertFalse(evil.contains("<") || evil.contains("\""))
    }

    @Test fun pickedSearchResultIdIsExtracted() {
        // The exact URL shape YouTube's mobile site switches to when a result is tapped.
        assertEquals("IODxDxX7oi4", com.gymtrainer.app.data.YouTube.parseId(
            "https://m.youtube.com/watch?v=IODxDxX7oi4&pp=ygUTcHVzaCB1cCBwcm9wZXIgZm9ybdIHCQkQCAGHKiGM7w%3D%3D"))
        assertEquals("abcdefghijk", com.gymtrainer.app.data.YouTube.parseId("https://m.youtube.com/shorts/abcdefghijk"))
    }
}
