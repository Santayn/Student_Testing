import {
  describe,
  expect,
  it,
} from 'vitest'

import {
  sanitizeStudentAttempt,
  sanitizeStudentResultData,
  sanitizeStudentSubmitResult,
} from '@/utils/resultContracts'

function rawAttempt() {
  return {
    attemptId: 101,
    testId: 7,
    testName: 'Контрольная',
    studentId: 55,
    studentName: 'Иван Иванов',
    attemptOrdinal: 1,
    completedAt:
      '2026-09-20T12:00:00Z',
    stats: {
      total: 2,
      right: 0,
      percent: 0,
    },
    results: [
      {
        questionText: 'Вопрос 1',
        givenAnswer: 'Ответ 1',
        correctAnswer: 'Эталон 1',
        correct: false,
        questionPoints: 10,
        awardedPoints: 7,
        gradingStatus: 'partial',
        gradingNote: 'Скрытая заметка',
      },
      {
        questionText: 'Вопрос 2',
        givenAnswer: 'Ответ 2',
        correctAnswer: 'Эталон 2',
        correct: false,
        questionPoints: 10,
        awardedPoints: 3,
        gradingStatus: 'partial',
        gradingNote: 'Ещё одна заметка',
      },
    ],
  }
}

describe('student result contracts', () => {
  it('converts per-question grading data into an aggregate score before stripping restricted fields', () => {
    const sanitized =
      sanitizeStudentAttempt(
        rawAttempt()
      )

    expect(sanitized).toMatchObject({
      attemptId: 101,
      testId: 7,
      testName: 'Контрольная',
      attemptOrdinal: 1,
      score: 10,
      maxScore: 20,
      scorePercent: 50,
    })

    expect(sanitized).not.toHaveProperty(
      'studentId'
    )
    expect(sanitized).not.toHaveProperty(
      'studentName'
    )

    expect(sanitized.results).toEqual([
      {
        questionText: 'Вопрос 1',
        givenAnswer: 'Ответ 1',
      },
      {
        questionText: 'Вопрос 2',
        givenAnswer: 'Ответ 2',
      },
    ])
  })

  it('whitelists the student result payload instead of forwarding teacher-only metadata', () => {
    const sanitized =
      sanitizeStudentResultData({
        stats: {
          total: 2,
          right: 0,
          percent: 0,
          hiddenMetric: 123,
        },
        selectedTestName: 'Контрольная',
        selectedGroupName: 'Группа 1',
        selectedStudentName: 'Другой студент',
        attemptCount: 1,
        internalFlag: true,
        attempts: [rawAttempt()],
      })

    expect(sanitized).toHaveProperty(
      'selectedTestName',
      'Контрольная'
    )
    expect(sanitized).not.toHaveProperty(
      'selectedGroupName'
    )
    expect(sanitized).not.toHaveProperty(
      'selectedStudentName'
    )
    expect(sanitized).not.toHaveProperty(
      'internalFlag'
    )
    expect(sanitized.stats).not.toHaveProperty(
      'hiddenMetric'
    )
  })

  it('strips correctness from the immediate submit response', () => {
    const sanitized =
      sanitizeStudentSubmitResult({
        attemptId: 77,
        score: 5,
        correctCount: 1,
        totalCount: 2,
        internalFlag: 'remove-me',
        details: [
          {
            questionText: '2 + 2?',
            givenAnswer: '4',
            correctAnswer: '4',
            correct: true,
            questionPoints: 2,
            awardedPoints: 2,
            gradingStatus: 'correct',
            gradingNote: 'Не показывать',
          },
        ],
      })

    expect(sanitized).toEqual({
      attemptId: 77,
      score: 5,
      correctCount: 1,
      totalCount: 2,
      details: [
        {
          questionText: '2 + 2?',
          givenAnswer: '4',
        },
      ],
    })
  })
})
