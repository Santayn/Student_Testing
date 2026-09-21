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

const subjects = source(
  'views', 'subjects', 'SubjectsView.vue'
)
const subjectDetails = source(
  'views', 'subjects', 'SubjectDetailsView.vue'
)

const topicLibrary = source(
  'views', 'teacher', 'TopicLibraryView.vue'
)
const questions = source(
  'views', 'teacher', 'QuestionsView.vue'
)
const testEditor = source(
  'views', 'teacher', 'TestEditorView.vue'
)
const lectures = source(
  'views', 'teacher', 'LectureManagementView.vue'
)
const workload = source(
  'views', 'teacher', 'TeacherWorkloadView.vue'
)
const courseTemplates = source(
  'views', 'teacher', 'CourseTemplatesView.vue'
)
const shell = source(
  'components', 'teacher', 'TeacherPageShell.vue'
)

const teacherWorkingPages = [
  topicLibrary,
  questions,
  testEditor,
  lectures,
  workload,
  courseTemplates,
]

describe('teacher working page responsive layout', () => {
  it('reuses the already migrated shared subject workspace for teachers', () => {
    expect(subjects).toContain('authStore.isTeacherMode')
    expect(subjects).toContain('subjects-grid')
    expect(subjectDetails).toContain('teacherLecturesRoute')
    expect(subjectDetails).toContain('teacherTopicsRoute')
  })

  it('does not keep wide legacy tables in teacher workflows', () => {
    for (const page of teacherWorkingPages) {
      expect(page).not.toContain('UiTable')
    }
  })

  it('uses responsive entity cards for long teacher content', () => {
    expect(topicLibrary).toContain('teacher-entity-list')
    expect(questions).toContain('teacher-choice-list')
    expect(questions).toContain('teacher-entity-card')
    expect(testEditor).toContain('teacher-entity-card')
    expect(lectures).toContain('teacher-entity-card')
    expect(workload).toContain('teacher-entity-card')
    expect(courseTemplates).toContain('teacher-entity-card')
  })

  it('keeps the teacher shell responsive inside the global sidebar layout', () => {
    expect(shell).toContain('.teacher-entity-card')
    expect(shell).toContain('overflow-wrap: anywhere')
    expect(shell).toContain('@media (max-width: 1180px)')
    expect(shell).toContain('@media (max-width: 720px)')
    expect(shell).toContain('.teacher-choice-row')
  })

  it('keeps useful contextual teacher transitions', () => {
    expect(topicLibrary).toContain("name: 'teacher-questions'")
    expect(topicLibrary).toContain("name: 'teacher-test-create'")
    expect(questions).toContain("name: 'teacher-topics'")
    expect(questions).toContain("name: 'teacher-test-create'")
    expect(testEditor).toContain("name: 'teacher-questions'")
    expect(lectures).toContain("name: 'teacher-topics'")
    expect(courseTemplates).toContain("name: 'teacher-lectures'")
  })

  it('uses current design tokens instead of legacy aliases', () => {
    const legacyTokens = [
      'var(--text)',
      'var(--text-secondary)',
      'var(--surface)',
      'var(--surface-secondary)',
      'var(--border)',
    ]

    for (const page of [
      ...teacherWorkingPages,
      shell,
    ]) {
      for (const token of legacyTokens) {
        expect(page).not.toContain(token)
      }
    }
  })
})
