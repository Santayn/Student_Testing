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

describe('lecture details workspace mode', () => {
  it('uses the effective student workspace mode for test-taking controls', () => {
    const lectureDetails = source('views/lectures/LectureDetailsView.vue')

    expect(lectureDetails)
      .toContain('authStore.isStudentMode')

    expect(lectureDetails)
      .not.toContain('authStore.isStudent ||')
  })
})
