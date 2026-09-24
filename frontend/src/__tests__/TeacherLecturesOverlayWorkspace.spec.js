// @vitest-environment node

import { readFileSync } from 'node:fs'
import { resolve } from 'node:path'

import {
  describe,
  expect,
  it,
} from 'vitest'

const lectures = readFileSync(
  resolve(
    process.cwd(),
    'src',
    'views',
    'teacher',
    'LectureManagementView.vue'
  ),
  'utf8'
)

const editorDrawer = readFileSync(
  resolve(process.cwd(), 'src', 'components', 'teacher', 'LectureEditorDrawer.vue'),
  'utf8'
)

const saveFlow = readFileSync(
  resolve(process.cwd(), 'src', 'composables', 'useLectureSaveFlow.js'),
  'utf8'
)

describe('teacher lectures overlay workspace', () => {
  it('keeps the main page focused on browsing and filtering lectures', () => {
    expect(lectures).toContain('UiFilterBar')
    expect(lectures).toContain('filteredLectures')
    expect(lectures).toContain('visibilityFilter')
    expect(lectures).toContain('testFilter')
    expect(lectures).toContain('sortMode')
    expect(lectures).toContain('teacher-entity-list')
  })

  it('moves lecture create and edit work into the shared drawer lifecycle', () => {
    expect(lectures).toContain('<LectureEditorDrawer')
    expect(editorDrawer).toContain('<UiDrawer')
    expect(lectures).toContain('useOverlayForm')
    expect(lectures).toContain('UiUnsavedChangesConfirm')
    expect(editorDrawer).toContain('teacher-lecture-form-section')
    expect(lectures).toContain('openCreateLecture')
    expect(lectures).toContain('openEditLecture')
    expect(lectures).toContain('requestLectureDrawerClose')
    expect(lectures).toContain('pendingFilesDirty')
    expect(editorDrawer).toContain("emit('save')")
    expect(editorDrawer).toContain("emit('close')")
  })

  it('keeps tests and materials inside the lecture editing context', () => {
    expect(lectures).toContain('useLectureSaveFlow')
    expect(saveFlow).toContain('syncLectureTests')
    expect(saveFlow).toContain('uploadPendingFiles')
    expect(lectures).toContain('loadMaterials')
    expect(lectures).toContain('downloadMaterial')
    expect(lectures).toContain('requestDeleteMaterial')
  })

  it('uses dialogs instead of browser confirmation for destructive actions', () => {
    expect(lectures).toContain('deleteConfirmVisible')
    expect(lectures).toContain('materialDeleteConfirmVisible')
    expect(lectures).toContain('UiDialog')
    expect(lectures).not.toContain('window.confirm')
  })

  it('preserves the teacher membership mutation guard and contextual routes', () => {
    expect(lectures).toContain('ensureSelectedMembershipActive()')
    expect(lectures).toContain("name: 'teacher-topics'")
    expect(lectures).toContain("name: 'teacher-questions'")
    expect(lectures).toContain("name: 'teacher-test-create'")
    expect(lectures).toContain('subjectMembershipId')
  })
})
