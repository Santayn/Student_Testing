package org.santayn.testing.mobile.core.platform

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Чтение текста вслух встроенным синтезатором речи (Android TextToSpeech / iOS AVSpeechSynthesizer).
 *
 * Дополняет, а не заменяет TalkBack/VoiceOver: кнопка «Прочитать вслух» нужна тем,
 * кто плохо видит, но не пользуется программой чтения с экрана.
 */
interface TextSpeaker {
    /** Ключ текста, который сейчас читается, или null. */
    val speakingKey: StateFlow<String?>

    fun speak(key: String, text: String)

    fun stop()
}

/** Заглушка для тестов и превью. */
class SilentSpeaker : TextSpeaker {
    override val speakingKey = MutableStateFlow<String?>(null)
    override fun speak(key: String, text: String) = Unit
    override fun stop() = Unit
}
