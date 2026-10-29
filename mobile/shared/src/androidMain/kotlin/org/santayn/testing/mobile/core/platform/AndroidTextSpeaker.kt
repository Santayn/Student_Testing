package org.santayn.testing.mobile.core.platform

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import co.touchlab.kermit.Logger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

/**
 * Синтез речи Android. Движок создаётся при первом обращении.
 *
 * Русский голос ищется по всем установленным синтезаторам: системный синтезатор многих телефонов
 * (Honor, Huawei, Xiaomi) не читает по-русски, а «Синтезатор речи Google» может стоять вторым.
 * Если не нашёлся ни один — [problem] говорит, что исправить; следующее нажатие пробует заново
 * (пользователь мог установить голос и вернуться).
 */
class AndroidTextSpeaker(context: Context) : TextSpeaker {
    private val context = context.applicationContext
    private val log = Logger.withTag("TTS")

    private val _speakingKey = MutableStateFlow<String?>(null)
    override val speakingKey: StateFlow<String?> = _speakingKey.asStateFlow()

    private val _problem = MutableStateFlow<SpeechProblem?>(null)
    override val problem: StateFlow<SpeechProblem?> = _problem.asStateFlow()

    private enum class State { IDLE, INITIALIZING, READY }

    private var state = State.IDLE
    private var tts: TextToSpeech? = null
    private var pending: Pair<String, String>? = null

    /** Синтезаторы, которые осталось попробовать в текущем поиске русского голоса. */
    private val candidates = ArrayDeque<String>()
    private val tried = mutableSetOf<String?>()
    private var foundEngineWithoutRussian: String? = null
    private var currentEngine: String? = null

    override fun speak(key: String, text: String) {
        _problem.value = null
        _speakingKey.value = key
        when (state) {
            State.READY -> say(key, text)
            State.INITIALIZING -> pending = key to text
            State.IDLE -> {
                pending = key to text
                candidates.clear()
                tried.clear()
                foundEngineWithoutRussian = null
                startEngine(null)
            }
        }
    }

    override fun stop() {
        pending = null
        tts?.stop()
        _speakingKey.value = null
    }

    override fun dismissProblem() {
        _problem.value = null
    }

    override fun openSettings(problem: SpeechProblem): Boolean {
        val ttsSettings = Intent("com.android.settings.TTS_SETTINGS")
        val intents = when (problem) {
            SpeechProblem.NO_ENGINE -> listOf(
                Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$GOOGLE_TTS")),
                Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=$GOOGLE_TTS")),
                ttsSettings,
            )
            SpeechProblem.NO_RUSSIAN_VOICE -> listOfNotNull(
                foundEngineWithoutRussian?.let { Intent(TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA).setPackage(it) },
                Intent(TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA),
                ttsSettings,
            )
            SpeechProblem.MUTED -> listOfNotNull(
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) Intent(Settings.Panel.ACTION_VOLUME) else null,
                Intent(Settings.ACTION_SOUND_SETTINGS),
            )
            SpeechProblem.FAILED -> listOf(ttsSettings)
        }
        return intents.any { intent ->
            try {
                context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                true
            } catch (e: ActivityNotFoundException) {
                false
            } catch (e: SecurityException) {
                false
            }
        }
    }

    private fun say(key: String, text: String) {
        val engine = tts ?: return fail(SpeechProblem.NO_ENGINE)
        if (isMuted()) _problem.value = SpeechProblem.MUTED
        val params = Bundle().apply { putInt(TextToSpeech.Engine.KEY_PARAM_STREAM, AudioManager.STREAM_MUSIC) }
        // Длинный текст — частями в очередь; последняя часть несёт сам ключ, по ней чтение и заканчивается.
        val parts = splitForSpeech(text, (TextToSpeech.getMaxSpeechInputLength() - SPEECH_LENGTH_MARGIN).coerceAtLeast(500))
        parts.forEachIndexed { index, part ->
            val utteranceId = if (index == parts.lastIndex) key else "$key$PART_SEPARATOR$index"
            val mode = if (index == 0) TextToSpeech.QUEUE_FLUSH else TextToSpeech.QUEUE_ADD
            if (engine.speak(part, mode, params, utteranceId) == TextToSpeech.ERROR) {
                log.w { "speak() returned ERROR on $currentEngine (part ${index + 1}/${parts.size})" }
                engine.stop()
                return fail(SpeechProblem.FAILED)
            }
        }
    }

    /** [enginePackage] = null — синтезатор по умолчанию. */
    private fun startEngine(enginePackage: String?) {
        state = State.INITIALIZING
        tried += enginePackage
        // Без синтезатора onInit вызывается прямо из конструктора, когда объекта ещё нет, — откладываем.
        var created: TextToSpeech? = null
        var earlyStatus: Int? = null
        created = TextToSpeech(context, { status ->
            val engine = created
            if (engine == null) earlyStatus = status else onInit(engine, enginePackage, status)
        }, enginePackage)
        tts = created
        earlyStatus?.let { onInit(created, enginePackage, it) }
    }

    private fun onInit(engine: TextToSpeech, enginePackage: String?, status: Int) {
        if (engine !== tts) return
        val name = enginePackage ?: engine.defaultEngine
        if (candidates.isEmpty() && enginePackage == null) {
            // Остальные установленные синтезаторы — запасные варианты.
            engine.engines.map { it.name }.filter { it != name }.forEach(candidates::addLast)
        }
        if (status != TextToSpeech.SUCCESS) {
            log.w { "TextToSpeech init failed: $status ($name)" }
            engine.shutdown()
            return tryNextEngine()
        }
        val russian = Locale.forLanguageTag("ru-RU")
        val available = engine.isLanguageAvailable(russian)
        if (available < TextToSpeech.LANG_AVAILABLE) {
            log.w { "No Russian voice in $name: $available" }
            foundEngineWithoutRussian = foundEngineWithoutRussian ?: name
            engine.shutdown()
            return tryNextEngine()
        }
        engine.setLanguage(russian)
        engine.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) = Unit
            override fun onDone(utteranceId: String?) = finished(utteranceId)
            @Deprecated("Deprecated in Java")
            override fun onError(utteranceId: String?) = failed(utteranceId, TextToSpeech.ERROR)
            override fun onError(utteranceId: String?, errorCode: Int) = failed(utteranceId, errorCode)
            override fun onStop(utteranceId: String?, interrupted: Boolean) = finished(utteranceId)
        })
        currentEngine = name
        state = State.READY
        pending?.let { (key, text) ->
            pending = null
            say(key, text)
        }
    }

    private fun tryNextEngine() {
        val next = generateSequence { candidates.removeFirstOrNull() }.firstOrNull { it !in tried }
        if (next != null) return startEngine(next)
        tts = null
        state = State.IDLE
        fail(if (foundEngineWithoutRussian != null) SpeechProblem.NO_RUSSIAN_VOICE else SpeechProblem.NO_ENGINE)
    }

    private fun fail(problem: SpeechProblem) {
        pending = null
        _speakingKey.value = null
        _problem.value = problem
    }

    private fun failed(utteranceId: String?, errorCode: Int) {
        if (_speakingKey.value != utteranceId?.substringBefore(PART_SEPARATOR)) return
        log.w { "Utterance error $errorCode on $currentEngine" }
        // Голос выбран, но его данные ещё не скачаны (Google TTS) — та же беда, что «нет русского голоса».
        if (errorCode == TextToSpeech.ERROR_NOT_INSTALLED_YET) foundEngineWithoutRussian = currentEngine
        fail(if (errorCode == TextToSpeech.ERROR_NOT_INSTALLED_YET) SpeechProblem.NO_RUSSIAN_VOICE else SpeechProblem.FAILED)
    }

    private fun finished(utteranceId: String?) {
        _speakingKey.compareAndSet(utteranceId, null)
    }

    private fun isMuted(): Boolean {
        val audio = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager ?: return false
        return audio.getStreamVolume(AudioManager.STREAM_MUSIC) == 0
    }

    private companion object {
        const val GOOGLE_TTS = "com.google.android.tts"
        const val PART_SEPARATOR = "#part-"
        const val SPEECH_LENGTH_MARGIN = 100
    }
}
