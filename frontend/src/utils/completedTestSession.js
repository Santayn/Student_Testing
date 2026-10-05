import {
  sanitizeStudentSubmitResult,
} from '@/utils/resultContracts'

const STORAGE_PREFIX =
  'student-test-completed:v1'

function storageKey(
  testId,
  assignmentId
) {
  return (
    `${STORAGE_PREFIX}:` +
    `${Number(testId)}:` +
    `${Number(assignmentId)}`
  )
}

function usableId(value) {
  const number = Number(value)

  return (
    Number.isFinite(number) &&
    number > 0
  )
}

export function readCompletedTestSession(
  testId,
  assignmentId
) {
  if (
    !usableId(testId) ||
    !usableId(assignmentId)
  ) {
    return null
  }

  try {
    const raw =
      sessionStorage.getItem(
        storageKey(
          testId,
          assignmentId
        )
      )

    if (!raw) {
      return null
    }

    const value = JSON.parse(raw)

    if (
      Number(value?.testId) !==
        Number(testId) ||
      Number(value?.assignmentId) !==
        Number(assignmentId) ||
      !value?.resultData
    ) {
      clearCompletedTestSession(
        testId,
        assignmentId
      )

      return null
    }

    const sanitized = {
      ...value,
      resultData:
        sanitizeStudentSubmitResult(
          value.resultData
        ),
    }

    try {
      sessionStorage.setItem(
        storageKey(
          testId,
          assignmentId
        ),
        JSON.stringify(sanitized)
      )
    } catch {
      // Best-effort cleanup of legacy session payloads.
    }

    return sanitized
  } catch {
    clearCompletedTestSession(
      testId,
      assignmentId
    )

    return null
  }
}

export function saveCompletedTestSession({
  testId,
  assignmentId,
  attemptId = null,
  test = null,
  resultData,
}) {
  if (
    !usableId(testId) ||
    !usableId(assignmentId) ||
    !resultData
  ) {
    return
  }

  try {
    sessionStorage.setItem(
      storageKey(
        testId,
        assignmentId
      ),
      JSON.stringify({
        testId: Number(testId),
        assignmentId:
          Number(assignmentId),
        attemptId:
          usableId(attemptId)
            ? Number(attemptId)
            : null,
        test,
        resultData:
          sanitizeStudentSubmitResult(
            resultData
          ),
      })
    )
  } catch {
    // Storage can be unavailable in restricted browser modes.
  }
}

export function clearCompletedTestSession(
  testId,
  assignmentId
) {
  if (
    !usableId(testId) ||
    !usableId(assignmentId)
  ) {
    return
  }

  try {
    sessionStorage.removeItem(
      storageKey(
        testId,
        assignmentId
      )
    )
  } catch {
    // Storage can be unavailable in restricted browser modes.
  }
}
