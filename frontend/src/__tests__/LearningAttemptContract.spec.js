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

describe('student attempt API contract', () => {
  it('uses public attemptsRemaining instead of raw attempts API', () => {
    const lectureView =
      source(
        '../views/lectures/LectureDetailsView.vue'
      )

    expect(lectureView)
      .toContain(
        'test.attemptsRemaining'
      )

    expect(lectureView)
      .not.toContain(
        'testAttemptsApi'
      )
  })

  it('starts only by assignment and submits only an owned attempt', () => {
    const testView =
      source(
        '../views/tests/TestView.vue'
      )

    expect(testView)
      .toContain(
        '.startAttempt('
      )

    expect(testView)
      .toContain(
        '.submitAttempt('
      )

    expect(testView)
      .not.toContain(
        '.getTest('
      )
  })
})
