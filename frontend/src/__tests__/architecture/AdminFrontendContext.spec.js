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

describe('admin frontend context', () => {
  it('loads all active teacher memberships for admin teacher screens', () => {
    const composable = source('composables/teacher/useTeacherSubjects.js')

    const loader = source('utils/teacherSubjectContext.js')

    expect(loader)
      .toContain('authStore.isAdminMode')

    expect(loader)
      .toContain('activeOnly: true')

    expect(composable)
      .toContain('преподаватель #')
  })

  it('uses admin-capable APIs for the admin results context', () => {
    const resultsView = source('views/results/ResultsView.vue')
    const resultsContext = source('composables/results/useResultsContextOptions.js')

    expect(resultsView)
      .toContain('useResultsContextOptions')

    expect(resultsContext)
      .toContain('subjectsApi.getAll({ signal })')

    expect(resultsContext)
      .toContain('lecturesApi.getAll({')

    expect(resultsContext)
      .toContain('lecturesApi.getTests(')
  })

  it('shows all subjects to admin regardless of admin person binding', () => {
    const subjectsView = source('views/subjects/SubjectsView.vue')

    expect(subjectsView)
      .toContain('if (authStore.isAdminMode)')

    expect(subjectsView)
      .toContain('loadSubjectCatalog(subjectsApi, cache)')
  })

  it('keeps the personal teacher profile tab tied to the TEACHER role', () => {
    const profileView = source('views/ProfileView.vue')
    const profileContext = source('composables/profile/useProfileContext.js')

    expect(profileView)
      .toContain('v-if="authStore.isTeacher"')

    expect(profileContext)
      .toContain('if (authStore.isTeacher)')

    expect(profileContext)
      .not.toContain('authStore.isTeacher || authStore.isAdmin')
  })
})
