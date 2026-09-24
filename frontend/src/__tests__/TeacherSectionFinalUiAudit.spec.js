// @vitest-environment node

import { readFileSync } from 'node:fs'
import { resolve } from 'node:path'

import {
  describe,
  expect,
  it,
} from 'vitest'

function source(...segments) {
  return readFileSync(
    resolve(process.cwd(), 'src', ...segments),
    'utf8'
  )
}

const topics = source('views', 'teacher', 'TopicLibraryView.vue')
const questions = source('views', 'teacher', 'QuestionsView.vue')
const lectures = source('views', 'teacher', 'LectureManagementView.vue')
const lectureDrawer = source('components', 'teacher', 'LectureEditorDrawer.vue')
const templates = source('views', 'teacher', 'CourseTemplatesView.vue')
const workload = source('views', 'teacher', 'TeacherWorkloadView.vue')
const testEditor = source('views', 'teacher', 'TestEditorView.vue')

const teacherPages = [
  topics,
  questions,
  lectures,
  templates,
  workload,
  testEditor,
]

describe('teacher section final UI audit', () => {
  it('uses the approved workspace and overlay pattern for CRUD collections', () => {
    expect(topics).toContain('UiFilterBar')
    expect(topics).toContain('UiDialog')
    expect(topics).toContain('useOverlayForm')

    for (const page of [questions, templates]) {
      expect(page).toContain('UiFilterBar')
      expect(page).toContain('UiDrawer')
      expect(page).toContain('useOverlayForm')
    }

    expect(lectures).toContain('UiFilterBar')
    expect(lectures).toContain('useOverlayForm')
    expect(lectures).toContain('<LectureEditorDrawer')
    expect(lectureDrawer).toContain('<UiDrawer')
  })

  it('keeps complex test creation as a dedicated route instead of forcing it into a drawer', () => {
    expect(testEditor).toContain('TeacherPageShell')
    expect(testEditor).not.toContain('UiDrawer')
    expect(testEditor).not.toContain('useOverlayForm')
  })

  it('keeps personal workload read-only while still searchable', () => {
    expect(workload).toContain('UiFilterBar')
    expect(workload).toContain('filteredAssignments')
    expect(workload).not.toContain('UiDrawer')
    expect(workload).not.toContain('createAssignment')
  })

  it('contains no legacy wide table or history-back navigation in teacher working pages', () => {
    for (const page of teacherPages) {
      expect(page).not.toContain('UiTable')
      expect(page).not.toContain('router.back()')
      expect(page).not.toContain('window.confirm')
    }
  })

  it('keeps user-friendly matching editing in the question drawer', () => {
    expect(questions).toContain('MatchingPairsEditor')
    expect(questions).not.toContain('left -> right')
  })
})
