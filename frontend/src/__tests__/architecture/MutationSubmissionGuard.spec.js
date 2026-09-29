import { readFileSync } from 'node:fs'
import { resolve } from 'node:path'

import { describe, expect, it } from 'vitest'

function source(relativePath) {
  return readFileSync(resolve(process.cwd(), 'src', relativePath), 'utf8')
}

describe('mutation submission guards', () => {
  it('keeps simple admin CRUD saves serialized while a request is pending', () => {
    const guardedViews = [
      'views/admin/FacultiesView.vue',
      'views/admin/SubjectsAdminView.vue',
      'views/admin/GroupsView.vue',
    ]

    for (const view of guardedViews) {
      expect(source(view)).toContain('if (saving.value)')
    }
  })

  it('keeps teacher mutation composables serialized by their overlay saving state', () => {
    expect(source('composables/questions/useQuestionMutations.js'))
      .toContain('if (saving?.value)')
    expect(source('composables/lectures/useLectureSaveFlow.js'))
      .toContain('if (saving?.value)')
    expect(source('composables/teacher/useTeacherTopicMutations.js'))
      .toContain('if (saving?.value)')
    expect(source('composables/course-templates/useCourseTemplateMutations.js'))
      .toContain('if (templateOverlay.saving.value)')
  })
})
