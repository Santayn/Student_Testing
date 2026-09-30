// @vitest-environment node

import {
  readFileSync,
} from 'node:fs'
import {
  resolve,
} from 'node:path'

import {
  describe,
  expect,
  it,
} from 'vitest'

function source(relativePath) {
  return readFileSync(
    resolve(
      process.cwd(),
      'src',
      relativePath
    ),
    'utf8'
  )
}

describe('form lifecycle contract', () => {
  it.each([
    [
      'groups',
      'views/admin/GroupsView.vue',
      'groupFormElement',
    ],
    [
      'faculties',
      'views/admin/FacultiesView.vue',
      'facultyFormElement',
    ],
    [
      'subjects',
      'views/admin/SubjectsAdminView.vue',
      'subjectFormElement',
    ],
    [
      'teacher topics',
      'views/teacher/TopicLibraryView.vue',
      'topicFormElement',
    ],
  ])(
    '%s clears stale field errors and can focus the invalid control',
    (
      _name,
      relativePath,
      formRef
    ) => {
      const view = source(
        relativePath
      )

      expect(view).toContain(
        'clearFormFieldError'
      )
      expect(view).toContain(
        'focusFirstInvalidField'
      )
      expect(view).toContain(
        `ref="${formRef}"`
      )
      expect(view).toContain(
        '@update:model-value='
      )
    }
  )

  it('extends field-error lifecycle to question and lecture editors', () => {
    const questions = source(
      'views/teacher/QuestionsView.vue'
    )
    const questionMutations = source(
      'composables/questions/useQuestionMutations.js'
    )
    const lectures = source(
      'views/teacher/LectureManagementView.vue'
    )
    const lectureDrawer = source(
      'components/teacher/LectureEditorDrawer.vue'
    )
    const lectureSave = source(
      'composables/lectures/useLectureSaveFlow.js'
    )

    expect(questions).toContain(
      'formFieldErrors'
    )
    expect(questions).toContain(
      'focusFirstInvalidField'
    )
    expect(questionMutations).toContain(
      'presentApiError'
    )
    expect(questionMutations).toContain(
      'FORM_FIELD_ERROR_SUMMARY'
    )

    expect(lectures).toContain(
      'formFieldErrors'
    )
    expect(lectureDrawer).toContain(
      'focusFirstInvalidField'
    )
    expect(lectureDrawer).toContain(
      "emit('field-change', 'title')"
    )
    expect(lectureSave).toContain(
      'structuredFieldErrors'
    )
    expect(lectureSave).toContain(
      'FORM_FIELD_ERROR_SUMMARY'
    )
    expect(lectureSave).toContain(
      'getApiErrorMessage'
    )
    expect(lectureSave).toContain(
      'Лекция уже создана'
    )
  })

  it('keeps double-submit protection in migrated mutation flows', () => {
    const groups = source(
      'views/admin/GroupsView.vue'
    )
    const faculties = source(
      'views/admin/FacultiesView.vue'
    )
    const subjects = source(
      'views/admin/SubjectsAdminView.vue'
    )
    const topics = source(
      'composables/teacher/useTeacherTopicMutations.js'
    )

    expect(groups).toContain(
      'if (saving.value)'
    )
    expect(faculties).toContain(
      'if (saving.value)'
    )
    expect(subjects).toContain(
      'if (saving.value)'
    )
    expect(topics).toContain(
      'if (saving?.value)'
    )
  })
})
