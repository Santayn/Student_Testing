package org.santayn.testing.mobile.core.platform

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import co.touchlab.kermit.Logger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

/** Синтез речи Android. Движок создаётся при первом обращении. */
class AndroidTextSpeaker(private val context: Context) : TextSpeaker {
    private val _speakingKey = MutableStateFlow<String?>(null)
    override val speakingKey: StateFlow<String?> = _speakingKey.asStateFlow()

    private var tts: TextToSpeech? = null
    private var ready = false
    private var pending: Pair<String, String>? = null

    override fun speak(key: String, text: String) {
        _speakingKey.value = key
        val engine = tts
        if (engine == null || !ready) {
            pending = key to text
            if (engine == null) init()
            return
        }
        engine.speak(text, TextToSpeech.QUEUE_FLUSH, null, key)
    }

    override fun stop() {
        pending = null
        tts?.stop()
        _speakingKey.value = null
    }

    private fun init() {
        tts = TextToSpeech(context.applicationContext) { status ->
            val engine = tts ?: return@TextToSpeech
            if (status != TextToSpeech.SUCCESS) {
                Logger.withTag("TTS").w { "TextToSpeech init failed: $status" }
                _speakingKey.value = null
                return@TextToSpeech
            }
            val result = engine.setLanguage(Locale.forLanguageTag("ru-RU"))
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Logger.withTag("TTS").w { "Russian voice is not available: $result" }
            }
            engine.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) = Unit
                override fun onDone(utteranceId: String?) = finished(utteranceId)
                @Deprecated("Deprecated in Java")
                override fun onError(utteranceId: String?) = finished(utteranceId)
                override fun onStop(utteranceId: String?, interrupted: Boolean) = finished(utteranceId)
            })
            ready = true
            pending?.let { (key, text) ->
                pending = null
                engine.speak(text, TextToSpeech.QUEUE_FLUSH, null, key)
            }
        }
    }

    private fun finished(utteranceId: String?) {
        _speakingKey.compareAndSet(utteranceId, null)
    }
}
