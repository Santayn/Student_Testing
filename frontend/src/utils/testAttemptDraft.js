const STORAGE_PREFIX =
  'student-test-draft:v1'

function usableId(value) {
  const number = Number(value)

  return (
    Number.isFinite(number) &&
    number > 0
  )
}

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

function questionType(question) {
  return Number(
    question?.type
  )
}

function questionText(question) {
  return String(
    question?.text ??
    question?.question ??
    ''
  )
}

function choiceOptions(question) {
  return Array.isArray(
    question?.options
  )
    ? question.options
    : []
}

function matchingPrompts(question) {
  return Array.isArray(
    question?.matchingPrompts
  )
    ? question.matchingPrompts
    : []
}

function matchingOptions(question) {
  return Array.isArray(
    question?.matchingOptions
  )
    ? question.matchingOptions
    : []
}

function questionSignature(question) {
  const type =
    questionType(question)

  const signature = {
    id: Number(question?.id),
    type,
    text: questionText(question),
  }

  if (type === 1 || type === 2) {
    signature.options =
      choiceOptions(question)
        .map((option) => ({
          id: Number(option?.id),
          text: String(
            option?.text ??
            option?.label ??
            ''
          ),
        }))
        .sort((left, right) => (
          left.id - right.id
        ))
  }

  if (type === 3) {
    signature.prompts =
      matchingPrompts(question)
        .map((prompt) => ({
          ordinal:
            Number(prompt?.ordinal),
          text: String(
            prompt?.text ?? ''
          ),
        }))
        .sort((left, right) => (
          left.ordinal - right.ordinal
        ))

    signature.matchingOptions =
      matchingOptions(question)
        .map((option) => (
          String(option ?? '')
        ))
        .sort()
  }

  return signature
}

function questionSetSignature(questions) {
  return (
    Array.isArray(questions)
      ? questions
      : []
  )
    .map(questionSignature)
    .sort((left, right) => (
      left.id - right.id
    ))
}

function signaturesMatch(
  savedSignature,
  currentSignature
) {
  return (
    JSON.stringify(savedSignature) ===
    JSON.stringify(currentSignature)
  )
}

function allowedOptionIds(question) {
  return new Set(
    choiceOptions(question)
      .map((option) => (
        Number(option?.id)
      ))
      .filter(usableId)
  )
}

function allowedPromptOrdinals(question) {
  return new Set(
    matchingPrompts(question)
      .map((prompt) => (
        Number(prompt?.ordinal)
      ))
      .filter((ordinal) => (
        Number.isFinite(ordinal) &&
        ordinal > 0
      ))
  )
}

function buildAnswerSnapshot({
  questions,
  singleAnswers,
  multipleAnswers,
  textAnswers,
  matchingAnswers,
}) {
  const snapshot = {
    single: {},
    multiple: {},
    text: {},
    matching: {},
  }

  questions.forEach((question) => {
    const id =
      String(question.id)

    const type =
      questionType(question)

    if (type === 1) {
      const allowed =
        allowedOptionIds(question)

      const selected =
        Number(singleAnswers?.[id])

      if (allowed.has(selected)) {
        snapshot.single[id] =
          selected
      }

      return
    }

    if (type === 2) {
      const allowed =
        allowedOptionIds(question)

      const selected =
        Array.isArray(
          multipleAnswers?.[id]
        )
          ? multipleAnswers[id]
          : []

      const normalized =
        [...new Set(
          selected
            .map(Number)
            .filter((value) => (
              allowed.has(value)
            ))
        )]

      if (normalized.length) {
        snapshot.multiple[id] =
          normalized
      }

      return
    }

    if (type === 3) {
      const allowedOrdinals =
        allowedPromptOrdinals(
          question
        )

      const selections =
        Array.isArray(
          matchingAnswers?.[id]
        )
          ? matchingAnswers[id]
          : []

      const byRightValue = {}

      matchingOptions(question)
        .forEach((right, index) => {
          const ordinal =
            Number(
              selections[index]
            )

          if (
            allowedOrdinals.has(
              ordinal
            )
          ) {
            byRightValue[
              String(right ?? '')
            ] = ordinal
          }
        })

      if (
        Object.keys(byRightValue)
          .length
      ) {
        snapshot.matching[id] =
          byRightValue
      }

      return
    }

    const answer =
      String(
        textAnswers?.[id] ?? ''
      )

    if (answer.length) {
      snapshot.text[id] =
        answer
    }
  })

  return snapshot
}

function hasAnswers(snapshot) {
  return (
    Object.keys(snapshot.single).length > 0 ||
    Object.keys(snapshot.multiple).length > 0 ||
    Object.keys(snapshot.text).length > 0 ||
    Object.keys(snapshot.matching).length > 0
  )
}

function restoreAnswers(
  questions,
  snapshot
) {
  const restored = {
    singleAnswers: {},
    multipleAnswers: {},
    textAnswers: {},
    matchingAnswers: {},
  }

  questions.forEach((question) => {
    const id =
      String(question.id)

    const type =
      questionType(question)

    if (type === 1) {
      const allowed =
        allowedOptionIds(question)

      const selected =
        Number(
          snapshot?.single?.[id]
        )

      if (allowed.has(selected)) {
        restored.singleAnswers[id] =
          selected
      }

      return
    }

    if (type === 2) {
      const allowed =
        allowedOptionIds(question)

      const selected =
        Array.isArray(
          snapshot?.multiple?.[id]
        )
          ? snapshot.multiple[id]
          : []

      restored.multipleAnswers[id] =
        [...new Set(
          selected
            .map(Number)
            .filter((value) => (
              allowed.has(value)
            ))
        )]

      return
    }

    if (type === 3) {
      const allowedOrdinals =
        allowedPromptOrdinals(
          question
        )

      const byRightValue =
        snapshot?.matching?.[id]

      restored.matchingAnswers[id] =
        matchingOptions(question)
          .map((right) => {
            const ordinal =
              Number(
                byRightValue?.[
                  String(right ?? '')
                ]
              )

            return (
              allowedOrdinals.has(
                ordinal
              )
                ? ordinal
                : ''
            )
          })

      return
    }

    const answer =
      snapshot?.text?.[id]

    if (
      typeof answer === 'string'
    ) {
      restored.textAnswers[id] =
        answer
    }
  })

  return restored
}

export function clearTestAttemptDraft(
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

export function saveTestAttemptDraft({
  testId,
  assignmentId,
  attemptId,
  questions = [],
  singleAnswers = {},
  multipleAnswers = {},
  textAnswers = {},
  matchingAnswers = {},
}) {
  if (
    !usableId(testId) ||
    !usableId(assignmentId) ||
    !usableId(attemptId) ||
    !Array.isArray(questions) ||
    !questions.length
  ) {
    return
  }

  const signature =
    questionSetSignature(
      questions
    )

  if (
    signature.some((question) => (
      !usableId(question.id)
    ))
  ) {
    return
  }

  const answers =
    buildAnswerSnapshot({
      questions,
      singleAnswers,
      multipleAnswers,
      textAnswers,
      matchingAnswers,
    })

  if (!hasAnswers(answers)) {
    clearTestAttemptDraft(
      testId,
      assignmentId
    )
    return
  }

  try {
    sessionStorage.setItem(
      storageKey(
        testId,
        assignmentId
      ),
      JSON.stringify({
        version: 1,
        testId: Number(testId),
        assignmentId:
          Number(assignmentId),
        attemptId:
          Number(attemptId),
        questionSignature:
          signature,
        answers,
      })
    )
  } catch {
    // Draft persistence is best-effort only.
  }
}

export function readTestAttemptDraft({
  testId,
  assignmentId,
  attemptId,
  questions = [],
}) {
  if (
    !usableId(testId) ||
    !usableId(assignmentId) ||
    !usableId(attemptId) ||
    !Array.isArray(questions) ||
    !questions.length
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

    const value =
      JSON.parse(raw)

    const contextMatches = (
      value?.version === 1 &&
      Number(value?.testId) ===
        Number(testId) &&
      Number(value?.assignmentId) ===
        Number(assignmentId) &&
      Number(value?.attemptId) ===
        Number(attemptId)
    )

    const signatureMatches =
      signaturesMatch(
        value?.questionSignature,
        questionSetSignature(
          questions
        )
      )

    if (
      !contextMatches ||
      !signatureMatches
    ) {
      clearTestAttemptDraft(
        testId,
        assignmentId
      )

      return null
    }

    return restoreAnswers(
      questions,
      value?.answers ?? {}
    )
  } catch {
    clearTestAttemptDraft(
      testId,
      assignmentId
    )

    return null
  }
}
