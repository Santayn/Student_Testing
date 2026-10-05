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

const studentWorkingPages = [
  source('views', 'subjects', 'SubjectsView.vue'),
  source('views', 'subjects', 'SubjectDetailsView.vue'),
  source('views', 'lectures', 'SubjectLecturesView.vue'),
  source('views', 'lectures', 'LectureDetailsView.vue'),
  source('views', 'tests', 'TestView.vue'),
  source('views', 'results', 'ResultsView.vue'),
]

const resultAttemptCard = source(
  'components',
  'results',
  'ResultAttemptCard.vue'
)

const studentMatchingQuestion = source(
  'components',
  'tests',
  'StudentMatchingQuestion.vue'
)

const uiSelect = source(
  'components',
  'ui',
  'UiSelect.vue'
)

const foundationSource = source(
  'theme',
  'foundation.css'
)

describe('student working page responsive layout', () => {
  it('does not preserve wide legacy tables in the student learning flow', () => {
    for (const page of studentWorkingPages) {
      expect(page).not.toContain('UiTable')
    }

    expect(resultAttemptCard).not.toContain('UiTable')
  })

  it('uses the current st design tokens instead of legacy aliases', () => {
    const legacyTokens = [
      'var(--text)',
      'var(--text-secondary)',
      'var(--surface)',
      'var(--surface-secondary)',
      'var(--border)',
    ]

    for (const page of [
      ...studentWorkingPages,
      resultAttemptCard,
    ]) {
      for (const token of legacyTokens) {
        expect(page).not.toContain(token)
      }
    }
  })

  it('uses responsive card/list layouts for subjects, lectures and results', () => {
    expect(studentWorkingPages[0]).toContain('subjects-grid')
    expect(studentWorkingPages[2]).toContain('lectures-grid')
    expect(studentWorkingPages[3]).toContain('lecture-tests')
    expect(resultAttemptCard).toContain('result-answer-list')
    expect(resultAttemptCard).toContain('@media (max-width: 720px)')
  })

  it('keeps test matching controls touch-safe through the shared UI kit', () => {
    const testView = studentWorkingPages[4]

    expect(testView).toContain('StudentMatchingQuestion')
    expect(testView).toContain('UiTag')
    expect(studentMatchingQuestion).toContain('UiSelect')
    expect(uiSelect).toContain('class="st-ui-control"')
    expect(foundationSource).toContain('.st-ui-control.p-select')
    expect(foundationSource).toContain('.p-select-option')
    expect(foundationSource).toContain('min-height: 44px')
  })
})
