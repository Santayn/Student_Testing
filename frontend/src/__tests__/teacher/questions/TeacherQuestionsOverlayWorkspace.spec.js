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

const questionEditorState = readFileSync(
  resolve(
    process.cwd(),
    'src',
    'composables',
    'questions',
    'useQuestionEditorState.js'
  ),
  'utf8'
)

const questionOptions = readFileSync(
  resolve(
    process.cwd(),
    'src',
    'composables',
    'questions',
    'useQuestionOptions.js'
  ),
  'utf8'
)

const questionTopicContext = readFileSync(
  resolve(
    process.cwd(),
    'src',
    'composables',
    'questions',
    'useQuestionTopicContext.js'
  ),
  'utf8'
)


const questionDrawerWorkspace = readFileSync(
  resolve(
    process.cwd(),
    'src',
    'composables',
    'questions',
    'useQuestionDrawerWorkspace.js'
  ),
  'utf8'
)

const questionImport = readFileSync(
  resolve(
    process.cwd(),
    'src',
    'composables',
    'questions',
    'useQuestionImport.js'
  ),
  'utf8'
)

const questionMutations = readFileSync(
  resolve(
    process.cwd(),
    'src',
    'composables',
    'questions',
    'useQuestionMutations.js'
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
    expect(questions).toContain('useQuestionEditorState')
    expect(questionEditorState).toContain('useOverlayForm')
    expect(questions).toContain('useQuestionDrawerWorkspace')
    expect(questions).toContain('openCreateQuestion')
    expect(questionDrawerWorkspace).toContain('async function editQuestion(question)')
    expect(questions).toContain('UiUnsavedChangesConfirm')
    expect(questionDrawerWorkspace).toContain('requestQuestionDrawerClose')
  })

  it('keeps answer option editing inside the question drawer', () => {
    expect(questions).toContain('teacher-choice-list')
    expect(questions).toContain('saveOption')
    expect(questions).toContain('useQuestionOptions')
    expect(questionOptions).toContain('api.createOption')
    expect(questionOptions).toContain('api.updateOption')
    expect(questionOptions).toContain('optionDraftDirty')
  })

  it('uses a visual pair editor for matching questions', () => {
    expect(questions).toContain('MatchingPairsEditor')
    expect(questions).toContain('form.matchingPairs')
    expect(questionEditorState).toContain('matchingPairsValidationMessage')
    expect(questions).not.toContain('matchingPairsText')
  })

  it('keeps Word import as a compact overlay action', () => {
    expect(questions).toContain('title="Импорт вопросов из Word"')
    expect(questions).toContain('useQuestionImport')
    expect(questions).toContain('openImportDialog')
    expect(questionImport).toContain('questionsApi.importFile')
  })

  it('revalidates the current teacher membership before mutations', () => {
    const mutationGuards = questionMutations.match(
      /ensureSelectedMembershipActive\(\)/g
    ) ?? []
    const optionGuards = questionOptions.match(
      /ensureSelectedMembershipActive\(\)/g
    ) ?? []
    const importGuards = questionImport.match(
      /ensureSelectedMembershipActive\(\)/g
    ) ?? []

    expect(
      mutationGuards.length +
      optionGuards.length +
      importGuards.length
    ).toBeGreaterThanOrEqual(4)
    expect(questions).toContain('useQuestionMutations')
    expect(questionMutations).toContain('api.create({')
    expect(questionMutations).toContain('api.update(')
    expect(questionMutations).toContain('api.updateActive(')
  })

  it('preserves contextual navigation and the topic deep-link', () => {
    expect(questions).toContain('useQuestionTopicContext')
    expect(questionTopicContext).toContain('route.query.topicId')
    expect(questions).toContain("name: 'teacher-topics'")
    expect(questions).toContain("name: 'teacher-test-create'")
  })

  it('does not reset workspace filters while saving a question', () => {
    const saveStart = questionMutations.indexOf('async function saveQuestion()')
    const saveEnd = questionMutations.indexOf('async function toggleQuestionActive')
    const saveSource = questionMutations.slice(saveStart, saveEnd)

    expect(saveSource).not.toContain('resetFilters()')
    expect(saveSource).toContain('await loadQuestions()')
    expect(saveSource).toContain('finishSaving({')
  })
})
