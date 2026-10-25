import {
  getCurrentScope,
  onScopeDispose,
  ref,
} from 'vue'

/** Браузеры обрывают длинные фразы (Chrome — примерно через 15 секунд), поэтому текст читается кусками. */
const MAX_UTTERANCE_LENGTH = 220

/** Делит текст на части не длиннее maxLength: по строкам, затем по предложениям, затем по пробелам. */
export function splitForSpeech(text, maxLength = MAX_UTTERANCE_LENGTH) {
  const parts = []
  for (const line of String(text ?? '').split('\n')) {
    let rest = line.trim()
    while (rest.length > maxLength) {
      const window = rest.slice(0, maxLength + 1)
      const sentenceEnd = Math.max(...['. ', '! ', '? ', '… ', '; '].map((mark) => {
        const index = window.lastIndexOf(mark)
        return index < 0 ? -1 : index + 1
      }))
      const candidates = [sentenceEnd, window.lastIndexOf(', ') + 1, window.lastIndexOf(' ')]
      const cut = candidates.find((index) => index > maxLength / 2) ?? maxLength
      parts.push(rest.slice(0, cut).trim())
      rest = rest.slice(cut).trim()
    }
    if (rest) {
      parts.push(rest)
    }
  }
  return parts.filter(Boolean)
}

function russianVoice(synth) {
  return synth.getVoices().find((voice) => voice.lang?.toLowerCase().startsWith('ru')) ?? null
}

/**
 * Чтение текста вслух встроенным синтезатором браузера (Web Speech API).
 * speakingKey — что сейчас читается; problem — почему прочитать не получилось.
 */
export function useSpeech() {
  const synth = typeof window !== 'undefined' ? window.speechSynthesis : null
  const supported = Boolean(synth && typeof window.SpeechSynthesisUtterance === 'function')
  const speakingKey = ref(null)
  const problem = ref('')
  let session = 0

  function stop() {
    session += 1
    speakingKey.value = null
    synth?.cancel()
  }

  function speak(key, text) {
    if (!supported) {
      problem.value = 'Браузер не умеет читать текст вслух.'
      return
    }
    stop()
    problem.value = ''
    const parts = splitForSpeech(text)
    if (!parts.length) {
      return
    }
    const voice = russianVoice(synth)
    const current = session
    speakingKey.value = key

    parts.forEach((part, index) => {
      const utterance = new window.SpeechSynthesisUtterance(part)
      utterance.lang = 'ru-RU'
      if (voice) {
        utterance.voice = voice
      }
      if (index === parts.length - 1) {
        utterance.onend = () => {
          if (session === current) speakingKey.value = null
        }
      }
      utterance.onerror = (event) => {
        if (session !== current || event.error === 'canceled' || event.error === 'interrupted') return
        stop()
        problem.value = 'Не удалось прочитать текст: синтезатор речи браузера вернул ошибку.'
      }
      synth.speak(utterance)
    })

    if (!voice && synth.getVoices().length) {
      problem.value = 'В системе нет русского голоса — текст может звучать с акцентом. Голос добавляется в настройках речи системы.'
    }
  }

  function toggle(key, text) {
    if (speakingKey.value === key) {
      stop()
    } else {
      speak(key, text)
    }
  }

  if (getCurrentScope()) {
    onScopeDispose(stop)
  }

  return {
    supported,
    speakingKey,
    problem,
    toggle,
    stop,
  }
}
