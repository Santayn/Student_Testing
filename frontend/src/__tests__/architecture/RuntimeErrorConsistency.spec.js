// @vitest-environment node

import { readFileSync } from 'node:fs'
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

describe('runtime error consistency', () => {
  it('uses status-based friendly destructive CRUD errors', () => {
    const faculties = source('views/admin/FacultiesView.vue')
    const subjects = source('views/admin/SubjectsAdminView.vue')
    const groups = source('views/admin/GroupsView.vue')

    for (const view of [faculties, subjects, groups]) {
      expect(view).toContain('isApiConflict(error)')
      expect(view).toContain('isApiForbidden(error)')
    }

    expect(faculties).toContain('Факультет используется связанными группами')
    expect(subjects).toContain('Предмет используется факультетами, лекциями, тестами')
    expect(groups).toContain('Группа используется назначениями, участниками, результатами')
  })

  it('clears stale entities and reports a dedicated not-found state', () => {
    const subject = source('views/subjects/SubjectDetailsView.vue')
    const lecture = source('views/lectures/LectureDetailsView.vue')

    expect(subject).toContain('subject.value = null')
    expect(subject).toContain('isApiNotFound(requestError)')
    expect(subject).toContain('Предмет больше не существует или недоступен.')

    expect(lecture).toContain('lecture.value = null')
    expect(lecture).toContain('materials.value = []')
    expect(lecture).toContain('tests.value = []')
    expect(lecture).toContain('isApiNotFound(requestError)')
    expect(lecture).toContain('Лекция больше не существует или недоступна.')
  })

  it('preserves existing partial-failure recovery instead of pretending multi-step saves are atomic', () => {
    const lectureSave = source('composables/lectures/useLectureSaveFlow.js')
    const users = source('views/admin/UsersView.vue')

    expect(lectureSave).toContain('partialCreateState.value = {')
    expect(lectureSave).toContain('Лекция уже создана, но не удалось сохранить связанные тесты или материалы')
    expect(lectureSave).toContain('retryingPartialCreate')

    expect(users).toContain('let completedSteps = 0')
    expect(users).toContain('await loadData({ clearMessage: false })')
    expect(users).toContain('Часть предыдущих изменений уже была применена; форма синхронизирована с сервером.')
  })
})
