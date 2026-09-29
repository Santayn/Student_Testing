import {
  readFileSync,
} from 'node:fs'
import { resolve } from 'node:path'


import {
  describe,
  expect,
  it,
} from 'vitest'

function source(relativePath) {
  return readFileSync(
    resolve(process.cwd(), 'src', relativePath),
    'utf8'
  )
}

describe('student attempt API contract', () => {
  it('uses public attemptsRemaining instead of raw attempts API', () => {
    const lectureView =
      source('views/lectures/LectureDetailsView.vue')

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
      source('composables/tests/useTestAttemptLifecycle.js')

    const testView =
      source('views/tests/TestView.vue')

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
      source('views/tests/TestView.vue')

    const attemptDraft =
      source('composables/tests/useAttemptDraft.js')

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
