// @vitest-environment node

import { readFileSync } from 'node:fs'
import { resolve } from 'node:path'

import {
  describe,
  expect,
  it,
} from 'vitest'

import {
  assignMatchingRightIndex,
  matchingPairsValidationMessage,
  matchingResultPairs,
  matchingRightIndexForPrompt,
  normalizeMatchingPairs,
  parseMatchingDisplay,
} from '@/utils/matchingPairs'

function source(...parts) {
  return readFileSync(
    resolve(process.cwd(), 'src', ...parts),
    'utf8'
  )
}

describe('matching question user experience', () => {
  it('edits matching questions as explicit pairs instead of developer text syntax', () => {
    const questions = source(
      'views',
      'teacher',
      'QuestionsView.vue'
    )
    const editor = source(
      'components',
      'questions',
      'MatchingPairsEditor.vue'
    )

    expect(questions).toContain('MatchingPairsEditor')
    expect(questions).toContain('v-model="form.matchingPairs"')
    expect(questions).not.toContain('matchingPairsText')
    expect(questions).not.toContain('parseMatchingPairsText')
    expect(editor).toContain('Добавить пару')
    expect(editor).toContain('Элемент слева')
    expect(editor).toContain('Правильное соответствие')
  })

  it('validates the same matching constraints before sending the existing backend payload', () => {
    expect(
      matchingPairsValidationMessage([
        { left: 'HTTP', right: 'Протокол' },
      ])
    ).toContain('минимум две пары')

    expect(
      matchingPairsValidationMessage([
        { left: 'HTTP', right: 'Протокол' },
        { left: 'HTTP', right: 'Сервис' },
      ])
    ).toContain('левой колонке')

    expect(
      normalizeMatchingPairs([
        { left: ' HTTP ', right: ' Протокол ' },
        { left: 'DNS', right: 'Имена' },
      ])
    ).toEqual([
      { ordinal: 1, left: 'HTTP', right: 'Протокол' },
      { ordinal: 2, left: 'DNS', right: 'Имена' },
    ])
  })

  it('parses the current backend result display into readable pairs', () => {
    const raw = 'HTTP -> Протокол | DNS -> Ошибка'
    const correct = 'HTTP -> Протокол | DNS -> Система имён'

    expect(parseMatchingDisplay(raw)).toHaveLength(2)
    expect(
      matchingResultPairs(raw, correct)
    ).toEqual([
      {
        ordinal: 1,
        left: 'HTTP',
        givenRight: 'Протокол',
        correctRight: 'Протокол',
        matches: true,
      },
      {
        ordinal: 2,
        left: 'DNS',
        givenRight: 'Ошибка',
        correctRight: 'Система имён',
        matches: false,
      },
    ])
  })

  it('renders matching results through a dedicated pair component', () => {
    const attemptCard = source(
      'components',
      'results',
      'ResultAttemptCard.vue'
    )
    const resultPairs = source(
      'components',
      'results',
      'ResultMatchingPairs.vue'
    )

    expect(attemptCard).toContain('ResultMatchingPairs')
    expect(attemptCard).toContain('isMatchingResult(row)')
    expect(resultPairs).toContain('Ответ студента')
    expect(resultPairs).toContain('Правильное соответствие')
    expect(resultPairs).toContain("pair.matches ? 'Верно' : 'Ошибка'")
  })
  it('maps the student-friendly left-to-right picker back to the existing backend selection shape', () => {
    const original = ['', '', '']

    const first = assignMatchingRightIndex(
      original,
      1,
      2,
      3
    )

    expect(first).toEqual(['', '', 1])
    expect(
      matchingRightIndexForPrompt(first, 1)
    ).toBe(2)

    const moved = assignMatchingRightIndex(
      first,
      1,
      0,
      3
    )

    expect(moved).toEqual([1, '', ''])
    expect(
      matchingRightIndexForPrompt(moved, 1)
    ).toBe(0)
  })

  it('renders the student matching interaction as readable pair rows instead of ordinal selectors', () => {
    const testView = source(
      'views',
      'tests',
      'TestView.vue'
    )
    const studentMatching = source(
      'components',
      'tests',
      'StudentMatchingQuestion.vue'
    )
    const uiSelect = source(
      'components',
      'ui',
      'UiSelect.vue'
    )

    expect(testView).toContain('StudentMatchingQuestion')
    expect(testView).not.toContain('matchingOrdinalOptions')
    expect(studentMatching).toContain('Сопоставьте элементы')
    expect(studentMatching).toContain('Выберите соответствие')
    expect(studentMatching).toContain('Сопоставлено ${completedCount} из ${normalizedPrompts.length}')
    expect(studentMatching).not.toContain('placeholder="№"')
    expect(uiSelect).toContain('option-disabled="disabled"')
  })

  it('renders the immediate student submit result through the same readable matching component', () => {
    const testView = source(
      'views',
      'tests',
      'TestView.vue'
    )

    expect(testView).toContain('ResultMatchingPairs')
    expect(testView).toContain('isMatchingSubmitDetail(detail)')
    expect(testView).toContain(':given-answer="detail.givenAnswer"')
  })

})
