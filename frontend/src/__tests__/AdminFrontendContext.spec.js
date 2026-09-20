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

describe('admin frontend context', () => {
  it('loads all active teacher memberships for admin teacher screens', () => {
    const composable = source(
      '../composables/useTeacherSubjects.js'
    )

    expect(composable)
      .toContain('authStore.isAdminMode')

    expect(composable)
      .toContain('activeOnly: true')

    expect(composable)
      .toContain('преподаватель #')
  })

  it('uses admin-capable APIs for the admin results context', () => {
    const resultsView = source(
      '../views/results/ResultsView.vue'
    )

    expect(resultsView)
      .toContain('subjectsApi.getAll()')

    expect(resultsView)
      .toContain('lecturesApi.getAll({')

    expect(resultsView)
      .toContain('lecturesApi.getTests(')
  })

  it('shows all subjects to admin regardless of admin person binding', () => {
    const subjectsView = source(
      '../views/subjects/SubjectsView.vue'
    )

    expect(subjectsView)
      .toContain('if (authStore.isAdminMode)')

    expect(subjectsView)
      .toContain('subjectsApi.getAll()')
  })

  it('keeps the personal teacher profile tab tied to the TEACHER role', () => {
    const profileView = source(
      '../views/ProfileView.vue'
    )

    expect(profileView)
      .toContain('if (authStore.isTeacher)')

    expect(profileView)
      .not.toContain('authStore.isTeacher || authStore.isAdmin')
  })
})
