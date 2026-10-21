import {
  attemptScoreSummary,
} from '@/utils/resultScoring'

function safeStats(stats) {
  return {
    total: stats?.total ?? 0,
    right: stats?.right ?? 0,
    percent: stats?.percent ?? 0,
  }
}

const PENDING_REVIEW_STATUSES = new Set([
  'ai_pending',
  'needs_teacher',
])

function optionalText(value) {
  const text = typeof value === 'string' ? value.trim() : ''
  return text || undefined
}

function safeStudentResultItem(item) {
  return {
    questionText:
      item?.questionText ?? '',
    givenAnswer:
      item?.givenAnswer ?? '',
    // Ответ ещё проверяют ИИ или преподаватель — баллы могут измениться.
    reviewPending:
      PENDING_REVIEW_STATUSES.has(item?.reviewStatus) || undefined,
    // Комментарии к ответу самого студента: что упущено. Правильный ответ не раскрывают.
    aiFeedback:
      optionalText(item?.aiFeedback),
    teacherComment:
      optionalText(item?.teacherComment),
  }
}

export function sanitizeStudentAttempt(attempt) {
  const source = attempt ?? {}
  const score = attemptScoreSummary(source)

  return {
    attemptId:
      source.attemptId ?? null,
    testId:
      source.testId ?? null,
    testName:
      source.testName ?? '',
    attemptOrdinal:
      source.attemptOrdinal ?? null,
    completedAt:
      source.completedAt ?? null,
    stats: safeStats(source.stats),

    // T3-FE needs weighted scoring, but student components do not need
    // per-question correctness/points. Calculate the aggregate once at
    // the boundary and then discard the sensitive detail fields.
    score: score.score,
    maxScore: score.maxScore,
    scorePercent: score.percent,

    results: Array.isArray(source.results)
      ? source.results.map(
          safeStudentResultItem
        )
      : [],
  }
}

export function sanitizeStudentResultData(data) {
  const source = data ?? {}

  return {
    stats: safeStats(source.stats),
    selectedTestName:
      source.selectedTestName ?? null,
    attemptCount:
      source.attemptCount ??
      (Array.isArray(source.attempts)
        ? source.attempts.length
        : 0),
    attempts: Array.isArray(
      source.attempts
    )
      ? source.attempts.map(
          sanitizeStudentAttempt
        )
      : [],
  }
}

function safeStudentSubmitDetail(detail) {
  return {
    questionText:
      detail?.questionText ?? '',
    givenAnswer:
      detail?.givenAnswer ?? '',
    reviewPending:
      detail?.pendingReview === true || undefined,
  }
}

export function sanitizeStudentSubmitResult(data) {
  const source = data ?? {}

  return {
    attemptId:
      source.attemptId ?? null,
    score:
      source.score ?? 0,
    correctCount:
      source.correctCount ?? 0,
    totalCount:
      source.totalCount ?? 0,
    pendingReviewCount:
      source.pendingReviewCount ?? 0,
    details: Array.isArray(
      source.details
    )
      ? source.details.map(
          safeStudentSubmitDetail
        )
      : [],
  }
}
