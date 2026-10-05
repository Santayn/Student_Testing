import {
  describe,
  expect,
  it,
} from 'vitest'

import {
  isQuestionAnswered,
  testAnswerProgress,
} from '@/utils/testAnswerProgress'

describe('student test answer progress', () => {
  const answers = {
    single: { 1: 10 },
    multiple: { 2: [20, 21] },
    matching: { 3: ['1', '2'], 4: ['1', ''] },
    text: { 5: '  ответ  ', 6: '   ' },
  }

  it('detects answered choice, text and matching questions', () => {
    expect(isQuestionAnswered({ id: 1, type: 1, options: [{ id: 10 }] }, answers)).toBe(true)
    expect(isQuestionAnswered({ id: 2, type: 2, options: [{ id: 20 }] }, answers)).toBe(true)
    expect(isQuestionAnswered({ id: 3, type: 3, matchingOptions: ['A', 'B'] }, answers)).toBe(true)
    expect(isQuestionAnswered({ id: 5, type: 4 }, answers)).toBe(true)
  })

  it('treats whitespace and partial matching as unanswered', () => {
    expect(isQuestionAnswered({ id: 4, type: 3, matchingOptions: ['A', 'B'] }, answers)).toBe(false)
    expect(isQuestionAnswered({ id: 6, type: 4 }, answers)).toBe(false)
  })

  it('returns answered, unanswered and percent for the test', () => {
    const result = testAnswerProgress([
      { id: 1, type: 1, options: [{ id: 10 }] },
      { id: 4, type: 3, matchingOptions: ['A', 'B'] },
      { id: 5, type: 4 },
      { id: 6, type: 4 },
    ], answers)

    expect(result).toEqual({
      answered: 2,
      unanswered: 2,
      total: 4,
      percent: 50,
    })
  })
})
