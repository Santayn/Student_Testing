import {
  beforeEach,
  describe,
  expect,
  it,
} from 'vitest'

import {
  clearTestAttemptDraft,
  readTestAttemptDraft,
  saveTestAttemptDraft,
} from '@/utils/testAttemptDraft'

function questions({
  matchingOrder = [
    'Правая A',
    'Правая B',
  ],
  textQuestion = 'Свободный вопрос',
} = {}) {
  return [
    {
      id: 101,
      type: 1,
      text: 'Один вариант',
      options: [
        { id: 1, text: 'A' },
        { id: 2, text: 'B' },
      ],
    },
    {
      id: 102,
      type: 2,
      text: 'Несколько вариантов',
      options: [
        { id: 3, text: 'C' },
        { id: 4, text: 'D' },
        { id: 5, text: 'E' },
      ],
    },
    {
      id: 103,
      type: 3,
      text: 'Сопоставление',
      matchingPrompts: [
        { ordinal: 1, text: 'Левая A' },
        { ordinal: 2, text: 'Левая B' },
      ],
      matchingOptions:
        matchingOrder,
    },
    {
      id: 104,
      type: 4,
      text: textQuestion,
    },
  ]
}

describe('test attempt draft session', () => {
  beforeEach(() => {
    sessionStorage.clear()
  })

  it('restores answers only for the same attempt and compatible question set', () => {
    const initialQuestions =
      questions()

    saveTestAttemptDraft({
      testId: 12,
      assignmentId: 34,
      attemptId: 56,
      questions: initialQuestions,
      singleAnswers: {
        101: 2,
      },
      multipleAnswers: {
        102: [3, 5],
      },
      textAnswers: {
        104: 'Мой ответ',
      },
      matchingAnswers: {
        103: [2, 1],
      },
    })

    const resumedQuestions =
      questions({
        matchingOrder: [
          'Правая B',
          'Правая A',
        ],
      })

    const restored =
      readTestAttemptDraft({
        testId: 12,
        assignmentId: 34,
        attemptId: 56,
        questions:
          resumedQuestions,
      })

    expect(restored).toEqual({
      singleAnswers: {
        101: 2,
      },
      multipleAnswers: {
        102: [3, 5],
      },
      textAnswers: {
        104: 'Мой ответ',
      },
      matchingAnswers: {
        103: [1, 2],
      },
    })
  })

  it('rejects and clears a draft from another attempt', () => {
    const currentQuestions =
      questions()

    saveTestAttemptDraft({
      testId: 12,
      assignmentId: 34,
      attemptId: 56,
      questions: currentQuestions,
      textAnswers: {
        104: 'Старый ответ',
      },
    })

    expect(
      readTestAttemptDraft({
        testId: 12,
        assignmentId: 34,
        attemptId: 57,
        questions: currentQuestions,
      })
    ).toBeNull()

    expect(
      readTestAttemptDraft({
        testId: 12,
        assignmentId: 34,
        attemptId: 56,
        questions: currentQuestions,
      })
    ).toBeNull()
  })

  it('rejects a draft when the question contract changed', () => {
    saveTestAttemptDraft({
      testId: 12,
      assignmentId: 34,
      attemptId: 56,
      questions: questions(),
      textAnswers: {
        104: 'Ответ на старый вопрос',
      },
    })

    const restored =
      readTestAttemptDraft({
        testId: 12,
        assignmentId: 34,
        attemptId: 56,
        questions: questions({
          textQuestion:
            'Вопрос был изменён',
        }),
      })

    expect(restored).toBeNull()
  })

  it('clears an existing draft explicitly', () => {
    const currentQuestions =
      questions()

    saveTestAttemptDraft({
      testId: 12,
      assignmentId: 34,
      attemptId: 56,
      questions: currentQuestions,
      textAnswers: {
        104: 'Черновик',
      },
    })

    clearTestAttemptDraft(
      12,
      34
    )

    expect(
      readTestAttemptDraft({
        testId: 12,
        assignmentId: 34,
        attemptId: 56,
        questions: currentQuestions,
      })
    ).toBeNull()
  })
})
