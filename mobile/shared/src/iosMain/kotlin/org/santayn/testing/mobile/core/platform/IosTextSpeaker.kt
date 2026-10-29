package org.santayn.testing.mobile.core.platform

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.ObjCSignatureOverride
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import platform.AVFAudio.AVAudioSession
import platform.AVFAudio.AVAudioSessionCategoryOptionDuckOthers
import platform.AVFAudio.AVAudioSessionCategoryPlayback
import platform.AVFAudio.AVAudioSessionModeSpokenAudio
import platform.AVFAudio.AVAudioSessionSetActiveOptionNotifyOthersOnDeactivation
import platform.AVFAudio.AVSpeechBoundary
import platform.AVFAudio.AVSpeechSynthesisVoice
import platform.AVFAudio.AVSpeechSynthesizer
import platform.AVFAudio.AVSpeechSynthesizerDelegateProtocol
import platform.AVFAudio.AVSpeechUtterance
import platform.AVFAudio.outputVolume
import platform.AVFAudio.setActive
import platform.darwin.NSObject

/**
 * Синтез речи iOS (AVSpeechSynthesizer, голос ru-RU).
 *
 * Аудиосессия переводится в «воспроизведение»: без этого при включённом беззвучном режиме
 * (переключатель сбоку iPhone) чтение идёт, но его не слышно.
 */
@OptIn(ExperimentalForeignApi::class)
class IosTextSpeaker : TextSpeaker {
    private val _speakingKey = MutableStateFlow<String?>(null)
    override val speakingKey: StateFlow<String?> = _speakingKey.asStateFlow()

    private val _problem = MutableStateFlow<SpeechProblem?>(null)
    override val problem: StateFlow<SpeechProblem?> = _problem.asStateFlow()

    private val synthesizer = AVSpeechSynthesizer()
    private var current: AVSpeechUtterance? = null
    private val delegate = Delegate { utterance ->
        // Отмена предыдущей фразы приходит уже после старта новой — её не считаем окончанием.
        if (utterance === current) {
            current = null
            _speakingKey.value = null
            deactivateSession()
        }
    }

    init {
        synthesizer.delegate = delegate
    }

    override fun speak(key: String, text: String) {
        _problem.value = null
        val voice = AVSpeechSynthesisVoice.voiceWithLanguage("ru-RU")
        if (voice == null) {
            _speakingKey.value = null
            _problem.value = SpeechProblem.NO_RUSSIAN_VOICE
            return
        }
        activateSession()
        if (AVAudioSession.sharedInstance().outputVolume == 0f) _problem.value = SpeechProblem.MUTED
        val utterance = AVSpeechUtterance(string = text)
        utterance.voice = voice
        current = utterance
        _speakingKey.value = key
        synthesizer.stopSpeakingAtBoundary(AVSpeechBoundary.AVSpeechBoundaryImmediate)
        synthesizer.speakUtterance(utterance)
    }

    override fun stop() {
        current = null
        synthesizer.stopSpeakingAtBoundary(AVSpeechBoundary.AVSpeechBoundaryImmediate)
        _speakingKey.value = null
        deactivateSession()
    }

    override fun dismissProblem() {
        _problem.value = null
    }

    /** Экранов установки голоса, доступных приложению, на iOS нет — пользователю показывается путь в Настройках. */
    override fun openSettings(problem: SpeechProblem) = false

    private fun activateSession() {
        val session = AVAudioSession.sharedInstance()
        session.setCategory(AVAudioSessionCategoryPlayback, AVAudioSessionModeSpokenAudio, AVAudioSessionCategoryOptionDuckOthers, null)
        session.setActive(true, null)
    }

    /** Возвращаем громкость другим приложениям (музыка приглушалась на время чтения). */
    private fun deactivateSession() {
        AVAudioSession.sharedInstance().setActive(false, AVAudioSessionSetActiveOptionNotifyOthersOnDeactivation, null)
    }

    private class Delegate(private val onFinished: (AVSpeechUtterance) -> Unit) : NSObject(), AVSpeechSynthesizerDelegateProtocol {
        @ObjCSignatureOverride
        override fun speechSynthesizer(synthesizer: AVSpeechSynthesizer, didFinishSpeechUtterance: AVSpeechUtterance) =
            onFinished(didFinishSpeechUtterance)

        @ObjCSignatureOverride
        override fun speechSynthesizer(synthesizer: AVSpeechSynthesizer, didCancelSpeechUtterance: AVSpeechUtterance) =
            onFinished(didCancelSpeechUtterance)
    }
}
