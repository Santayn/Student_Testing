function questionType(question) {
  return Number(question?.type)
}

function choiceOptions(question) {
  return Array.isArray(question?.options)
    ? question.options
    : []
}

function matchingOptions(question) {
  return Array.isArray(question?.matchingOptions)
    ? question.matchingOptions
    : []
}

export function isQuestionAnswered(question, answers) {
  const id = String(question?.id ?? '')
  const type = questionType(question)
  const hasChoices = choiceOptions(question).length > 0

  if (type === 1 && hasChoices) {
    const value = answers.single?.[id]

    return !(
      value === null ||
      value === undefined ||
      value === ''
    )
  }

  if (type === 2 && hasChoices) {
    return Array.isArray(answers.multiple?.[id]) &&
      answers.multiple[id].length > 0
  }

  if (type === 3) {
    const options = matchingOptions(question)
    const selections = answers.matching?.[id]

    return options.length > 0 &&
      Array.isArray(selections) &&
      selections.length >= options.length &&
      selections
        .slice(0, options.length)
        .every((value) => String(value ?? '').trim() !== '')
  }

  return String(answers.text?.[id] ?? '').trim() !== ''
}

export function testAnswerProgress(questions, answers) {
  const source = Array.isArray(questions)
    ? questions
    : []

  const answered = source.filter(
    (question) => isQuestionAnswered(question, answers)
  ).length

  const total = source.length

  return {
    answered,
    unanswered: Math.max(0, total - answered),
    total,
    percent: total > 0
      ? Math.round((answered / total) * 100)
      : 0,
  }
}
