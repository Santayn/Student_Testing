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
  resolve(process.cwd(), 'src', 'composables', 'lectures', 'useLectureSaveFlow.js'),
  'utf8'
)

const dataFlow = readFileSync(
  resolve(process.cwd(), 'src', 'composables', 'lectures', 'useLectureManagementData.js'),
  'utf8'
)


const drawerFlow = readFileSync(
  resolve(process.cwd(), 'src', 'composables', 'lectures', 'useLectureDrawerWorkspace.js'),
  'utf8'
)

const deleteFlow = readFileSync(
  resolve(process.cwd(), 'src', 'composables', 'lectures', 'useLectureDelete.js'),
  'utf8'
)
const materialFlow = readFileSync(
  resolve(process.cwd(), 'src', 'composables', 'lectures', 'useLectureMaterials.js'),
  'utf8'
)

const materialManager = readFileSync(
  resolve(process.cwd(), 'src', 'components', 'teacher', 'LectureMaterialsManager.vue'),
  'utf8'
)

describe('teacher lectures overlay workspace', () => {
  it('keeps the main page focused on browsing and filtering lectures', () => {
    expect(lectures).toContain('UiFilterBar')
    expect(lectures).toContain('useLectureManagementData')
    expect(dataFlow).toContain('filteredLectures')
    expect(dataFlow).toContain('visibilityFilter')
    expect(dataFlow).toContain('testFilter')
    expect(dataFlow).toContain('sortMode')
    expect(dataFlow).toContain('createLatestRequestGuard')
    expect(lectures).toContain('teacher-entity-list')
  })

  it('moves lecture create and edit work into the shared drawer lifecycle', () => {
    expect(lectures).toContain('<LectureEditorDrawer')
    expect(editorDrawer).toContain('<UiDrawer')
    expect(lectures).toContain('useOverlayForm')
    expect(lectures).toContain('UiUnsavedChangesConfirm')
    expect(editorDrawer).toContain('teacher-lecture-form-section')
    expect(lectures).toContain('useLectureDrawerWorkspace')
    expect(drawerFlow).toContain('openCreateLecture')
    expect(drawerFlow).toContain('openEditLecture')
    expect(drawerFlow).toContain('requestLectureDrawerClose')
    expect(drawerFlow).toContain('pendingFilesDirty')
    expect(editorDrawer).toContain("emit('save')")
    expect(editorDrawer).toContain("emit('close')")
  })

  it('keeps tests and materials inside the lecture editing context', () => {
    expect(lectures).toContain('useLectureSaveFlow')
    expect(saveFlow).toContain('syncLectureTests')
    expect(saveFlow).toContain('uploadPendingFiles')

    expect(lectures).toContain('useLectureMaterials')
    expect(editorDrawer).toContain('<LectureMaterialsManager')
    expect(materialManager).toContain('<UiFileInput')
    expect(materialFlow).toContain('loadMaterials')
    expect(materialFlow).toContain('downloadMaterial')
    expect(materialFlow).toContain('requestDeleteMaterial')
  })

  it('uses dialogs instead of browser confirmation for destructive actions', () => {
    expect(lectures).toContain('useLectureDelete')
    expect(deleteFlow).toContain('deleteConfirmVisible')
    expect(lectures).toContain('materialDeleteConfirmVisible')
    expect(lectures).toContain('UiDialog')
    expect(lectures).not.toContain('window.confirm')
  })

  it('preserves the teacher membership mutation guard and contextual routes', () => {
    expect(deleteFlow).toContain('await ensureSelectedMembershipActive()')
    expect(saveFlow).toContain('await ensureSelectedMembershipActive()')
    expect(lectures).toContain("name: 'teacher-topics'")
    expect(lectures).toContain("name: 'teacher-questions'")
    expect(lectures).toContain("name: 'teacher-test-create'")
    expect(lectures).toContain('subjectMembershipId')
  })
})
