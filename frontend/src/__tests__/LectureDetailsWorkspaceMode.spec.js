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

describe('lecture details workspace mode', () => {
  it('uses the effective student workspace mode for test-taking controls', () => {
    const lectureDetails = source(
      '../views/lectures/LectureDetailsView.vue'
    )

    expect(lectureDetails)
      .toContain('authStore.isStudentMode')

    expect(lectureDetails)
      .not.toContain('authStore.isStudent ||')
  })
})
