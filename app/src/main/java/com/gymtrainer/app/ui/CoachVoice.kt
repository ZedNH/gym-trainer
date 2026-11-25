package com.gymtrainer.app.ui

import android.content.Context
import android.content.Intent
import android.speech.tts.TextToSpeech
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.gymtrainer.app.domain.CoachCue
import com.gymtrainer.app.domain.CoachLanguage
import com.gymtrainer.app.domain.CoachLines

/**
 * Text-to-speech coach. Speaks [CoachCue]s in the chosen language; if the phone has no Bangla
 * voice it falls back to English and sets [banglaMissing] so the screen can offer to install it.
 */
class CoachVoice(context: Context, language: CoachLanguage) {
    private var tts: TextToSpeech? = null
    private var ready = false
    var requested by mutableStateOf(language); private set
    /** Language actually being spoken (English when Bangla isn't installed). */
    var speaking by mutableStateOf(CoachLanguage.ENGLISH); private set
    var banglaMissing by mutableStateOf(false); private set

    init {
        var engine: TextToSpeech? = null
        engine = TextToSpeech(context.applicationContext) { status ->
            if (status == TextToSpeech.SUCCESS) { tts = engine; ready = true; apply() }
        }
    }

    fun setLanguage(lang: CoachLanguage) { requested = lang; apply() }

    private fun apply() {
        val engine = tts ?: return
        val locale = requested.locales.firstOrNull { engine.isLanguageAvailable(it) >= TextToSpeech.LANG_AVAILABLE }
        if (locale != null) {
            engine.language = locale
            speaking = requested
            banglaMissing = false
            engine.setSpeechRate(if (requested == CoachLanguage.BANGLA) 0.95f else 1f)
        } else {
            engine.language = CoachLanguage.ENGLISH.locales.first()
            speaking = CoachLanguage.ENGLISH
            banglaMissing = requested == CoachLanguage.BANGLA
        }
    }

    /** Speaks [cue]. Countdown numbers interrupt; tips queue behind the current line. */
    fun say(cue: CoachCue) {
        val engine = tts ?: return
        if (!ready) return
        val text = CoachLines.text(cue, speaking)
        val mode = if (cue is CoachCue.Tip || cue is CoachCue.Motivate || cue is CoachCue.RestTip) TextToSpeech.QUEUE_ADD else TextToSpeech.QUEUE_FLUSH
        engine.speak(text, mode, null, "cue${System.nanoTime()}")
    }

    fun stop() { tts?.stop() }
    fun shutdown() { tts?.stop(); tts?.shutdown(); tts = null }

    companion object {
        /** Opens the phone's text-to-speech settings to download voices (e.g. Bangla). */
        fun installIntent() = Intent(TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
}

@Composable
fun rememberCoachVoice(): CoachVoice {
    val context = LocalContext.current
    val settings = rememberSettings()
    val voice = remember { CoachVoice(context, CoachLanguage.from(settings.coachLanguage)) }
    DisposableEffect(voice) { onDispose { voice.shutdown() } }
    return voice
}
