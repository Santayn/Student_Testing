package org.santayn.testing.mobile.core.platform

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Чтение текста вслух встроенным синтезатором речи (Android TextToSpeech / iOS AVSpeechSynthesizer).
 *
 * Дополняет, а не заменяет TalkBack/VoiceOver: кнопка «Прочитать вслух» нужна тем,
 * кто плохо видит, но не пользуется программой чтения с экрана.
 *
 * Если прочитать не получается (нет синтезатора, нет русского голоса, звук на нуле), реализация
 * сообщает об этом через [problem] — молча «ничего не происходит» быть не должно.
 */
interface TextSpeaker {
    /** Ключ текста, который сейчас читается, или null. */
    val speakingKey: StateFlow<String?>

    /** Почему чтение не началось или не слышно; показывается пользователю, сбрасывается [dismissProblem]. */
    val problem: StateFlow<SpeechProblem?>

    fun speak(key: String, text: String)

    fun stop()

    fun dismissProblem()

    /**
     * Открывает системный экран, где проблему можно исправить (установка синтезатора, загрузка голоса).
     * false — на этой платформе такого экрана нет, пользователю показывается только подсказка.
     */
    fun openSettings(problem: SpeechProblem): Boolean
}

/** Причина, по которой текст не прочитан (или не будет слышен). */
enum class SpeechProblem {
    /** На устройстве нет синтезатора речи или он не запускается. */
    NO_ENGINE,

    /** Синтезатор есть, но не умеет читать по-русски (голос не установлен). */
    NO_RUSSIAN_VOICE,

    /** Громкость звука на нуле — чтение идёт, но его не слышно. */
    MUTED,

    /** Синтезатор вернул ошибку при чтении. */
    FAILED,
}

/**
 * Делит длинный текст на части не длиннее [maxLength] — синтезатор Android принимает до 4000 символов
 * за раз, а текст лекции бывает длиннее. Режет по абзацам, затем по предложениям, затем по пробелам.
 */
fun splitForSpeech(text: String, maxLength: Int): List<String> {
    require(maxLength > 0)
    val parts = mutableListOf<String>()
    var rest = text.trim()
    while (rest.length > maxLength) {
        val window = rest.substring(0, maxLength + 1)
        val cut = listOf(
            window.lastIndexOf('\n'),
            SENTENCE_END.findAll(window).lastOrNull()?.let { it.range.last + 1 } ?: -1,
            window.lastIndexOf(' '),
        ).firstOrNull { it > maxLength / 2 } ?: maxLength
        rest.substring(0, cut).trim().takeIf { it.isNotEmpty() }?.let(parts::add)
        rest = rest.substring(cut).trim()
    }
    if (rest.isNotEmpty()) parts += rest
    return parts
}

private val SENTENCE_END = Regex("""[.!?…](?=\s)""")

/** Заглушка для тестов и превью. */
class SilentSpeaker : TextSpeaker {
    override val speakingKey = MutableStateFlow<String?>(null)
    override val problem = MutableStateFlow<SpeechProblem?>(null)
    override fun speak(key: String, text: String) = Unit
    override fun stop() = Unit
    override fun dismissProblem() {
        problem.value = null
    }
    override fun openSettings(problem: SpeechProblem) = false
}
