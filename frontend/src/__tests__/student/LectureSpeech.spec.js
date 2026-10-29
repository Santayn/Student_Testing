import {
  afterEach,
  describe,
  expect,
  it,
  vi,
} from 'vitest'

import { markdownPlainText } from '@/utils/markdown'
import { splitForSpeech, useSpeech } from '@/composables/shared/useSpeech'

describe('lecture text to speech', () => {
  afterEach(() => {
    vi.unstubAllGlobals()
  })

  it('turns markdown into plain sentences and skips code', () => {
    const text = markdownPlainText('## Индексы\n\n- B-дерево\n- хеш\n\n```sql\nSELECT 1;\n```\n\n| A | B |\n|---|---|\n| 1 | 2 |')
    expect(text).toBe('Индексы.\nB-дерево.\nхеш.\nA, B.\n1, 2.')
  })

  it('splits long text at sentence ends within the limit', () => {
    const text = 'Индекс ускоряет поиск строк. '.repeat(30)
    const parts = splitForSpeech(text, 100)
    expect(parts.length).toBeGreaterThan(5)
    expect(parts.every((part) => part.length <= 100 && part.endsWith('.'))).toBe(true)
  })

  it('queues all parts with a russian voice and stops on demand', () => {
    const spoken = []
    const synth = {
      speak: vi.fn((utterance) => spoken.push(utterance)),
      cancel: vi.fn(),
      getVoices: () => [{ lang: 'en-US' }, { lang: 'ru-RU', name: 'Русский' }],
    }
    vi.stubGlobal('speechSynthesis', synth)
    vi.stubGlobal('SpeechSynthesisUtterance', function Utterance(text) { this.text = text })

    const speech = useSpeech()
    speech.toggle('body', 'Первое предложение. '.repeat(30))

    expect(speech.speakingKey.value).toBe('body')
    expect(spoken.length).toBeGreaterThan(1)
    expect(spoken.every((u) => u.lang === 'ru-RU' && u.voice?.name === 'Русский')).toBe(true)

    spoken.at(-1).onend()
    expect(speech.speakingKey.value).toBe(null)

    speech.toggle('body', 'Текст.')
    speech.toggle('body', 'Текст.')
    expect(speech.speakingKey.value).toBe(null)
    expect(synth.cancel).toHaveBeenCalled()
  })
})
