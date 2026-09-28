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
    const lifecycle =
      source(
        '../composables/useTestAttemptLifecycle.js'
      )

    const testView =
      source(
        '../views/tests/TestView.vue'
      )

    expect(testView)
      .toContain(
        'useTestAttemptLifecycle'
      )

    expect(lifecycle)
      .toContain(
        '.startAttempt('
      )

    expect(lifecycle)
      .toContain(
        '.submitAttempt('
      )

    expect(lifecycle)
      .not.toContain(
        '.getTest('
      )
  })

  it('delegates attempt draft persistence to the shared composable', () => {
    const testView =
      source(
        '../views/tests/TestView.vue'
      )

    const attemptDraft =
      source(
        '../composables/useAttemptDraft.js'
      )

    expect(testView)
      .toContain(
        'useAttemptDraft'
      )

    expect(testView)
      .not.toContain(
        'saveTestAttemptDraft'
      )

    expect(testView)
      .not.toContain(
        'readTestAttemptDraft'
      )

    expect(attemptDraft)
      .toContain(
        'saveTestAttemptDraft'
      )

    expect(attemptDraft)
      .toContain(
        "'pagehide'"
      )
  })

})
