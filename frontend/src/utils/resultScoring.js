const KNOWN_GRADING_STATUSES = new Set([
  'correct',
  'partial',
  'incorrect',
])

function finiteNumber(value) {
  if (
    value === null ||
    value === undefined ||
    value === ''
  ) {
    return null
  }

  const number = Number(value)

  return Number.isFinite(number)
    ? number
    : null
}

function nonNegative(value) {
  const number = finiteNumber(value)

  if (number === null) {
    return null
  }

  return Math.max(0, number)
}

function round(value, digits = 2) {
  const factor = 10 ** digits

  return Math.round(
    (value + Number.EPSILON) * factor
  ) / factor
}

function clamp(value, minimum, maximum) {
  return Math.min(
    maximum,
    Math.max(minimum, value)
  )
}

export function normalizeGradingStatus(result) {
  const explicitStatus =
    typeof result?.gradingStatus === 'string'
      ? result.gradingStatus
          .trim()
          .toLowerCase()
      : ''

  if (
    KNOWN_GRADING_STATUSES.has(
      explicitStatus
    )
  ) {
    return explicitStatus
  }

  if (result?.correct === true) {
    return 'correct'
  }

  const maximum =
    nonNegative(
      result?.questionPoints
    )

  const awarded =
    nonNegative(
      result?.awardedPoints
    )

  if (
    maximum !== null &&
    maximum > 0 &&
    awarded !== null &&
    awarded > 0
  ) {
    return awarded >= maximum
      ? 'correct'
      : 'partial'
  }

  return 'incorrect'
}

export function gradingStatusLabel(result) {
  switch (
    normalizeGradingStatus(result)
  ) {
    case 'correct':
      return 'Верно'

    case 'partial':
      return 'Частично верно'

    default:
      return 'Неверно'
  }
}

export function resultItemScore(result) {
  const explicitMaximum =
    nonNegative(
      result?.questionPoints
    )

  const maximum =
    explicitMaximum !== null &&
    explicitMaximum > 0
      ? explicitMaximum
      : 1

  const explicitAwarded =
    nonNegative(
      result?.awardedPoints
    )

  let awarded

  if (explicitAwarded !== null) {
    awarded = clamp(
      explicitAwarded,
      0,
      maximum
    )
  } else {
    awarded =
      normalizeGradingStatus(
        result
      ) === 'correct'
        ? maximum
        : 0
  }

  return {
    awardedPoints: round(awarded),
    questionPoints: round(maximum),
    gradingStatus:
      normalizeGradingStatus(
        result
      ),
    usesExplicitPoints:
      explicitMaximum !== null ||
      explicitAwarded !== null,
  }
}

function attemptLevelScore(attempt) {
  const score =
    nonNegative(attempt?.score)

  const maximum =
    nonNegative(attempt?.maxScore)

  if (
    score === null ||
    maximum === null ||
    maximum <= 0
  ) {
    return null
  }

  const safeScore = clamp(
    score,
    0,
    maximum
  )

  const explicitPercent =
    nonNegative(
      attempt?.scorePercent
    )

  return {
    score: round(safeScore),
    maxScore: round(maximum),
    percent: round(
      explicitPercent === null
        ? safeScore * 100 / maximum
        : clamp(
            explicitPercent,
            0,
            100
          )
    ),
    source: 'attempt',
  }
}

function resultItemsScore(attempt) {
  const results =
    Array.isArray(attempt?.results)
      ? attempt.results
      : []

  if (!results.length) {
    return null
  }

  const items =
    results.map(resultItemScore)

  const hasExplicitPoints =
    items.some(
      (item) =>
        item.usesExplicitPoints
    )

  if (!hasExplicitPoints) {
    return null
  }

  const score = items.reduce(
    (sum, item) =>
      sum + item.awardedPoints,
    0
  )

  const maxScore = items.reduce(
    (sum, item) =>
      sum + item.questionPoints,
    0
  )

  if (maxScore <= 0) {
    return null
  }

  return {
    score: round(score),
    maxScore: round(maxScore),
    percent: round(
      score * 100 / maxScore
    ),
    source: 'results',
  }
}

function legacyStatsScore(attempt) {
  const total =
    nonNegative(
      attempt?.stats?.total
    ) ?? 0

  const right = clamp(
    nonNegative(
      attempt?.stats?.right
    ) ?? 0,
    0,
    total
  )

  const explicitPercent =
    nonNegative(
      attempt?.stats?.percent
    )

  return {
    score: round(right),
    maxScore: round(total),
    percent: round(
      explicitPercent === null
        ? (
            total > 0
              ? right * 100 / total
              : 0
          )
        : clamp(
            explicitPercent,
            0,
            100
          )
    ),
    source: 'stats',
  }
}

export function attemptScoreSummary(attempt) {
  const score =
    attemptLevelScore(attempt) ??
    resultItemsScore(attempt) ??
    legacyStatsScore(attempt)

  const results =
    Array.isArray(attempt?.results)
      ? attempt.results
      : []

  const statusCounts = {
    correct: 0,
    partial: 0,
    incorrect: 0,
  }

  results.forEach((result) => {
    statusCounts[
      normalizeGradingStatus(result)
    ] += 1
  })

  return {
    ...score,
    statusCounts,
  }
}

function completedTime(attempt) {
  const time = new Date(
    attempt?.completedAt ?? 0
  ).getTime()

  return Number.isFinite(time)
    ? time
    : 0
}

export function compareAttempts(
  candidate,
  current
) {
  const candidateScore =
    attemptScoreSummary(candidate)

  const currentScore =
    attemptScoreSummary(current)

  if (
    candidateScore.percent !==
    currentScore.percent
  ) {
    return (
      candidateScore.percent -
      currentScore.percent
    )
  }

  if (
    candidateScore.score !==
    currentScore.score
  ) {
    return (
      candidateScore.score -
      currentScore.score
    )
  }

  const timeDifference =
    completedTime(candidate) -
    completedTime(current)

  if (timeDifference !== 0) {
    return timeDifference
  }

  return (
    Number(
      candidate?.attemptOrdinal
    ) || 0
  ) - (
    Number(
      current?.attemptOrdinal
    ) || 0
  )
}

export function bestAttempt(attempts) {
  if (!Array.isArray(attempts)) {
    return null
  }

  return attempts.reduce(
    (best, attempt) => {
      if (!best) {
        return attempt
      }

      return compareAttempts(
        attempt,
        best
      ) > 0
        ? attempt
        : best
    },
    null
  )
}
