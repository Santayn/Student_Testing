package org.santayn.testing.mobile.core.platform

import kotlinx.cinterop.ObjCSignatureOverride
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import platform.AVFAudio.AVSpeechBoundary
import platform.AVFAudio.AVSpeechSynthesisVoice
import platform.AVFAudio.AVSpeechSynthesizer
import platform.AVFAudio.AVSpeechSynthesizerDelegateProtocol
import platform.AVFAudio.AVSpeechUtterance
import platform.darwin.NSObject

/** Синтез речи iOS (AVSpeechSynthesizer, голос ru-RU). */
class IosTextSpeaker : TextSpeaker {
    private val _speakingKey = MutableStateFlow<String?>(null)
    override val speakingKey: StateFlow<String?> = _speakingKey.asStateFlow()

    private val synthesizer = AVSpeechSynthesizer()
    private val delegate = Delegate { _speakingKey.value = null }

    init {
        synthesizer.delegate = delegate
    }

    override fun speak(key: String, text: String) {
        synthesizer.stopSpeakingAtBoundary(AVSpeechBoundary.AVSpeechBoundaryImmediate)
        val utterance = AVSpeechUtterance(string = text)
        utterance.voice = AVSpeechSynthesisVoice.voiceWithLanguage("ru-RU")
        _speakingKey.value = key
        synthesizer.speakUtterance(utterance)
    }

    override fun stop() {
        synthesizer.stopSpeakingAtBoundary(AVSpeechBoundary.AVSpeechBoundaryImmediate)
        _speakingKey.value = null
    }

    private class Delegate(private val onFinished: () -> Unit) : NSObject(), AVSpeechSynthesizerDelegateProtocol {
        @ObjCSignatureOverride
        override fun speechSynthesizer(synthesizer: AVSpeechSynthesizer, didFinishSpeechUtterance: AVSpeechUtterance) =
            onFinished()

        @ObjCSignatureOverride
        override fun speechSynthesizer(synthesizer: AVSpeechSynthesizer, didCancelSpeechUtterance: AVSpeechUtterance) =
            onFinished()
    }
}
