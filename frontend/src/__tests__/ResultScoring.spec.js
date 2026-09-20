import {
  describe,
  expect,
  it,
} from 'vitest'

import {
  attemptScoreSummary,
  bestAttempt,
  gradingStatusLabel,
  normalizeGradingStatus,
  resultItemScore,
} from '@/utils/resultScoring'

function result({
  questionPoints,
  awardedPoints,
  correct = false,
  gradingStatus,
}) {
  return {
    questionPoints,
    awardedPoints,
    correct,
    gradingStatus,
  }
}

describe('result scoring', () => {
  it('calculates weighted score from awardedPoints instead of stats.percent', () => {
    const attempt = {
      stats: {
        total: 3,
        right: 1,
        percent: 33.33,
      },
      results: [
        result({
          questionPoints: 10,
          awardedPoints: 10,
          correct: true,
          gradingStatus: 'correct',
        }),
        result({
          questionPoints: 10,
          awardedPoints: 5,
          gradingStatus: 'partial',
        }),
        result({
          questionPoints: 10,
          awardedPoints: 0,
          gradingStatus: 'incorrect',
        }),
      ],
    }

    expect(
      attemptScoreSummary(attempt)
    ).toMatchObject({
      score: 15,
      maxScore: 30,
      percent: 50,
      source: 'results',
      statusCounts: {
        correct: 1,
        partial: 1,
        incorrect: 1,
      },
    })
  })

  it('chooses the best attempt by weighted percentage, not by legacy right-answer percentage', () => {
    const weightedWinner = {
      attemptId: 1,
      attemptOrdinal: 1,
      completedAt: '2026-09-20T10:00:00Z',
      stats: {
        total: 10,
        right: 4,
        percent: 40,
      },
      results: [
        result({
          questionPoints: 10,
          awardedPoints: 7,
          gradingStatus: 'partial',
        }),
      ],
    }

    const legacyWinner = {
      attemptId: 2,
      attemptOrdinal: 2,
      completedAt: '2026-09-20T11:00:00Z',
      stats: {
        total: 10,
        right: 5,
        percent: 50,
      },
      results: [
        result({
          questionPoints: 10,
          awardedPoints: 5,
          gradingStatus: 'partial',
        }),
      ],
    }

    expect(
      bestAttempt([
        legacyWinner,
        weightedWinner,
      ])?.attemptId
    ).toBe(1)
  })

  it('uses score, completion time and ordinal as deterministic tie breakers', () => {
    const lowerScore = {
      attemptId: 1,
      attemptOrdinal: 1,
      completedAt: '2026-09-20T10:00:00Z',
      results: [
        result({
          questionPoints: 10,
          awardedPoints: 5,
        }),
      ],
    }

    const higherScore = {
      attemptId: 2,
      attemptOrdinal: 2,
      completedAt: '2026-09-20T09:00:00Z',
      results: [
        result({
          questionPoints: 20,
          awardedPoints: 10,
        }),
      ],
    }

    expect(
      bestAttempt([
        lowerScore,
        higherScore,
      ])?.attemptId
    ).toBe(2)

    const newer = {
      ...higherScore,
      attemptId: 3,
      completedAt: '2026-09-20T12:00:00Z',
    }

    expect(
      bestAttempt([
        higherScore,
        newer,
      ])?.attemptId
    ).toBe(3)
  })

  it('normalizes partial grading and clamps invalid awarded points', () => {
    expect(
      normalizeGradingStatus({
        questionPoints: 4,
        awardedPoints: 2,
      })
    ).toBe('partial')

    expect(
      gradingStatusLabel({
        gradingStatus: 'partial',
      })
    ).toBe('Частично верно')

    expect(
      resultItemScore({
        questionPoints: 4,
        awardedPoints: 7,
      })
    ).toMatchObject({
      questionPoints: 4,
      awardedPoints: 4,
    })
  })

  it('falls back to legacy stats when point data is not present', () => {
    expect(
      attemptScoreSummary({
        stats: {
          total: 4,
          right: 3,
          percent: 75,
        },
        results: [
          { correct: true },
          { correct: true },
          { correct: true },
          { correct: false },
        ],
      })
    ).toMatchObject({
      score: 3,
      maxScore: 4,
      percent: 75,
      source: 'stats',
    })
  })

  it('prefers future attempt-level score fields when backend adds them', () => {
    expect(
      attemptScoreSummary({
        score: 8.5,
        maxScore: 10,
        scorePercent: 85,
        stats: {
          total: 2,
          right: 1,
          percent: 50,
        },
      })
    ).toMatchObject({
      score: 8.5,
      maxScore: 10,
      percent: 85,
      source: 'attempt',
    })
  })
})
