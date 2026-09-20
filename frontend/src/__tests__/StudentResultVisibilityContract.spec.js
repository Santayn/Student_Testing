import {
  readFileSync,
} from 'node:fs'

import {
  describe,
  expect,
  it,
} from 'vitest'

function source(relativePath) {
  return readFileSync(
    new URL(
      relativePath,
      import.meta.url
    ),
    'utf8'
  )
}

describe('student result visibility contract', () => {
  it('does not show per-question correctness or the correct answer immediately after submit', () => {
    const testView =
      source(
        '../views/tests/TestView.vue'
      )

    expect(testView)
      .not.toContain(
        'detail.correct'
      )

    expect(testView)
      .not.toContain(
        'detail.correctAnswer'
      )

    expect(testView)
      .not.toContain(
        '<dt>Правильный ответ</dt>'
      )
  })

  it('keeps correct answer and correctness columns teacher-only in result history', () => {
    const attemptCard =
      source(
        '../components/results/ResultAttemptCard.vue'
      )

    expect(attemptCard)
      .toContain(
        "const teacherOnlyColumns = ["
      )

    expect(attemptCard)
      .toContain(
        "if (props.mode === 'teacher')"
      )

    expect(attemptCard)
      .toContain(
        "label: 'Правильный ответ'"
      )

    expect(attemptCard)
      .toContain(
        "label: 'Результат'"
      )
  })
})
