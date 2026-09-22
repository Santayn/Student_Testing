// @vitest-environment node

import { readFileSync } from 'node:fs'
import { resolve } from 'node:path'

import {
  describe,
  expect,
  it,
} from 'vitest'

const questions = readFileSync(
  resolve(
    process.cwd(),
    'src',
    'views',
    'teacher',
    'QuestionsView.vue'
  ),
  'utf8'
)

describe('teacher questions overlay workspace', () => {
  it('keeps the page focused on search, filters and existing questions', () => {
    expect(questions).toContain('UiFilterBar')
    expect(questions).toContain('filteredQuestions')
    expect(questions).toContain('typeFilter')
    expect(questions).toContain('statusFilter')
    expect(questions).toContain('sortMode')
    expect(questions).toContain('resetFilters')
    expect(questions).not.toContain('UiTable')
  })

  it('creates and edits a question in the shared drawer lifecycle', () => {
    expect(questions).toContain('UiDrawer')
    expect(questions).toContain('useOverlayForm')
    expect(questions).toContain('openCreateQuestion')
    expect(questions).toContain('async function editQuestion(question)')
    expect(questions).toContain('UiUnsavedChangesConfirm')
    expect(questions).toContain('requestQuestionDrawerClose')
  })

  it('keeps answer option editing inside the question drawer', () => {
    expect(questions).toContain('teacher-choice-list')
    expect(questions).toContain('saveOption')
    expect(questions).toContain('questionsApi.createOption')
    expect(questions).toContain('questionsApi.updateOption')
    expect(questions).toContain('optionDraftDirty')
  })

  it('uses a visual pair editor for matching questions', () => {
    expect(questions).toContain('MatchingPairsEditor')
    expect(questions).toContain('form.matchingPairs')
    expect(questions).toContain('matchingPairsValidationMessage')
    expect(questions).not.toContain('matchingPairsText')
  })

  it('keeps Word import as a compact overlay action', () => {
    expect(questions).toContain('title="Импорт вопросов из Word"')
    expect(questions).toContain('openImportDialog')
    expect(questions).toContain('questionsApi.importFile')
  })

  it('revalidates the current teacher membership before mutations', () => {
    const guards = questions.match(
      /ensureSelectedMembershipActive\(\)/g
    ) ?? []

    expect(guards.length).toBeGreaterThanOrEqual(4)
    expect(questions).toContain('questionsApi.create({')
    expect(questions).toContain('questionsApi.update(')
    expect(questions).toContain('questionsApi.updateActive(')
  })

  it('preserves contextual navigation and the topic deep-link', () => {
    expect(questions).toContain('route.query.topicId')
    expect(questions).toContain("name: 'teacher-topics'")
    expect(questions).toContain("name: 'teacher-test-create'")
  })

  it('does not reset workspace filters while saving a question', () => {
    const saveStart = questions.indexOf('async function saveQuestion()')
    const saveEnd = questions.indexOf('async function toggleQuestionActive')
    const saveSource = questions.slice(saveStart, saveEnd)

    expect(saveSource).not.toContain('resetFilters()')
    expect(saveSource).toContain('await loadQuestions()')
    expect(saveSource).toContain('finishSaving({')
  })
})
