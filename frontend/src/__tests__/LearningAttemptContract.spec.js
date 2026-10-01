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

  it('resumes with a read-only current-attempt request before starting a new attempt', () => {
    const testView =
      source(
        '../views/tests/TestView.vue'
      )

    expect(testView)
      .toContain(
        '.getCurrentAttempt('
      )

    expect(testView)
      .toContain(
        '.startAttempt('
      )

    expect(testView.indexOf('.getCurrentAttempt('))
      .toBeLessThan(
        testView.indexOf('.startAttempt(')
      )

    expect(testView)
      .toContain(
        '.submitAttempt('
      )

    expect(testView)
      .toContain(
        'rememberAttemptInRoute('
      )
  })
  it('polls the read-only attempt status while asynchronous grading is pending', () => {
    const testView =
      source(
        '../views/tests/TestView.vue'
      )

    expect(testView)
      .toContain(
        'learningApi.getAttemptStatus('
      )

    expect(testView)
      .toContain(
        'status.correctCount'
      )

    expect(testView)
      .toContain(
        'status.totalCount'
      )

    expect(testView)
      .toContain(
        'scheduleGradingPolling()'
      )
  })

  it('restores a completed attempt from the attemptId route query without starting another attempt', () => {
    const testView =
      source(
        '../views/tests/TestView.vue'
      )

    const learningApi =
      source(
        '../api/learning.api.js'
      )

    expect(testView)
      .toContain(
        'route.query.attemptId'
      )

    expect(testView)
      .toContain(
        'restoreRouteAttempt('
      )

    expect(testView)
      .toContain(
        'learningApi.getAttemptResult(id)'
      )

    expect(testView)
      .toContain(
        'resultData.value = payload.result ?? {}'
      )

    expect(testView.indexOf('restoreRouteAttempt('))
      .toBeLessThan(
        testView.indexOf('.startAttempt(')
      )

    expect(learningApi)
      .toContain(
        '/public/learning/attempts/${attemptId}/result'
      )
  })

})
