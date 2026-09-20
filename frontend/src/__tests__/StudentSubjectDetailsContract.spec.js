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

describe('student subject details API contract', () => {
  it('uses the public learning subject endpoint for student-only mode', () => {
    const view = source(
      '../views/subjects/SubjectDetailsView.vue'
    )

    expect(view)
      .toContain('learningApi')

    expect(view)
      .toContain('.getSubject(')

    expect(view)
      .toContain('authStore.isStudent')

    expect(view)
      .toContain('!authStore.isTeacher')

    expect(view)
      .toContain('!authStore.isAdmin')
  })

  it('keeps the management subject endpoint for teacher and admin context', () => {
    const view = source(
      '../views/subjects/SubjectDetailsView.vue'
    )

    expect(view)
      .toContain('subjectsApi')

    expect(view)
      .toContain('.getById(')
  })
})
