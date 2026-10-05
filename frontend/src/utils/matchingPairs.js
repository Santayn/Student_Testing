function comparable(value) {
  return String(value ?? '')
    .normalize('NFKC')
    .trim()
    .toLocaleLowerCase('ru-RU')
    .replace(/ё/g, 'е')
    .replace(/[^\p{L}\p{N}]+/gu, ' ')
    .replace(/\s+/g, ' ')
    .trim()
}

export function emptyMatchingPair(ordinal = 1) {
  return {
    ordinal,
    left: '',
    right: '',
  }
}

export function normalizeMatchingPairs(pairs) {
  return (Array.isArray(pairs) ? pairs : [])
    .map((pair, index) => ({
      ordinal: index + 1,
      left: String(pair?.left ?? '').trim(),
      right: String(pair?.right ?? '').trim(),
    }))
    .filter((pair) => pair.left || pair.right)
}

export function ensureMatchingPairRows(pairs, minimum = 2) {
  const rows = Array.isArray(pairs)
    ? pairs.map((pair, index) => ({
        ordinal: index + 1,
        left: String(pair?.left ?? ''),
        right: String(pair?.right ?? ''),
      }))
    : []

  while (rows.length < minimum) {
    rows.push(emptyMatchingPair(rows.length + 1))
  }

  return rows
}

export function matchingPairsValidationMessage(pairs) {
  const normalized = normalizeMatchingPairs(pairs)

  if (normalized.length < 2) {
    return 'Для вопроса на соответствие нужны минимум две пары.'
  }

  if (normalized.some((pair) => !pair.left || !pair.right)) {
    return 'Заполните обе части каждой пары соответствия.'
  }

  const leftValues = normalized.map((pair) => comparable(pair.left))
  const rightValues = normalized.map((pair) => comparable(pair.right))

  if (new Set(leftValues).size !== leftValues.length) {
    return 'Значения в левой колонке должны быть уникальными.'
  }

  if (new Set(rightValues).size !== rightValues.length) {
    return 'Значения в правой колонке должны быть уникальными.'
  }

  return ''
}

export function parseMatchingDisplay(value) {
  const text = String(value ?? '').trim()

  if (!text) {
    return []
  }

  const pairs = text
    .split(/\s*\|\s*/)
    .map((segment, index) => {
      const separatorIndex = segment.indexOf('->')

      if (separatorIndex < 0) {
        return null
      }

      const left = segment.slice(0, separatorIndex).trim()
      const rawRight = segment.slice(separatorIndex + 2).trim()
      const right = rawRight === '-' ? '' : rawRight

      if (!left) {
        return null
      }

      return {
        ordinal: index + 1,
        left,
        right,
      }
    })
    .filter(Boolean)

  // Backend требует минимум две пары для matching-вопроса. Этот порог
  // одновременно защищает обычные текстовые ответы, где случайно встретилось "->".
  return pairs.length >= 2
    ? pairs
    : []
}

export function matchingResultPairs(givenAnswer, correctAnswer = null) {
  const givenPairs = parseMatchingDisplay(givenAnswer)
  const correctPairs = parseMatchingDisplay(correctAnswer)

  const source = correctPairs.length
    ? correctPairs
    : givenPairs

  if (!source.length) {
    return []
  }

  return source.map((pair, index) => {
    const given = givenPairs[index]
    const correct = correctPairs[index]
    const givenRight = given?.right ?? ''
    const correctRight = correct?.right ?? ''

    return {
      ordinal: index + 1,
      left: correct?.left || given?.left || pair.left,
      givenRight,
      correctRight,
      matches:
        correctPairs.length > 0 &&
        comparable(givenRight) !== '' &&
        comparable(givenRight) === comparable(correctRight),
    }
  })
}

export function matchingRightIndexForPrompt(
  selections,
  promptOrdinal
) {
  const ordinal = Number(promptOrdinal)

  if (!Number.isFinite(ordinal) || ordinal <= 0) {
    return null
  }

  const index = (Array.isArray(selections) ? selections : [])
    .findIndex((value) => Number(value) === ordinal)

  return index >= 0 ? index : null
}

export function assignMatchingRightIndex(
  selections,
  promptOrdinal,
  rightIndex,
  optionCount
) {
  const ordinal = Number(promptOrdinal)
  const count = Math.max(0, Number(optionCount) || 0)
  const next = Array.from(
    { length: count },
    (_, index) => (
      Array.isArray(selections)
        ? selections[index] ?? ''
        : ''
    )
  )

  if (!Number.isFinite(ordinal) || ordinal <= 0) {
    return next
  }

  next.forEach((value, index) => {
    if (Number(value) === ordinal) {
      next[index] = ''
    }
  })

  if (
    rightIndex === null ||
    rightIndex === undefined ||
    rightIndex === ''
  ) {
    return next
  }

  const index = Number(rightIndex)

  if (
    !Number.isInteger(index) ||
    index < 0 ||
    index >= count
  ) {
    return next
  }

  next[index] = ordinal
  return next
}
