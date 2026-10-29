// @vitest-environment node
import { readFileSync } from 'node:fs'
import { resolve } from 'node:path'
import { describe, expect, it } from 'vitest'

import {
  getPresentationKeys,
  getPresentationLoader,
} from '@/config/presentationRegistry'

function source(relativePath) {
  return readFileSync(
    resolve(process.cwd(), 'src', relativePath),
    'utf8'
  )
}

const page = source('views/teacher/QuestionsView.vue')
const controller = source('composables/questions/useQuestionsWorkspace.js')
const overlay = source('views/teacher/questions/QuestionsOverlays.vue')
const bank = source('views/teacher/questions/QuestionsBank.vue')
const list = source('views/teacher/questions/QuestionsList.vue')
const context = source('views/teacher/questions/QuestionsContext.vue')

const variants = Object.fromEntries(
  ['compact', 'medium', 'wide'].map((mode) => [
    mode,
    source(`views/teacher/presentations/questions.${mode}.vue`),
  ])
)

describe('teacher questions responsive presentation', () => {
  it('registers three independent lazy chunks and does not load them during discovery', () => {
    expect(getPresentationKeys()).toContain('teacher/questions')
    const loaders = ['compact', 'medium', 'wide'].map((mode) =>
      getPresentationLoader('teacher/questions', mode)
    )
    expect(loaders.every((loader) => typeof loader === 'function')).toBe(true)
    expect(new Set(loaders).size).toBe(3)
  })

  it('has one route-level controller and a continuously mounted editor and import dialog', () => {
    expect(page).toContain('const workspace = useQuestionsWorkspace()')
    expect(page).toContain('provide(questionsWorkspaceKey, workspace)')
    expect(page).toContain('<ResponsivePresentation view-key="teacher/questions" />')
    expect(page).toContain('<QuestionsOverlays />')
    expect(page.indexOf('<QuestionsOverlays />')).toBeGreaterThan(
      page.indexOf('<ResponsivePresentation')
    )
    for (const mode of Object.values(variants)) {
      expect(mode).not.toContain('useQuestionsWorkspace(')
      expect(mode).not.toContain('useQuestionMutations(')
      expect(mode).not.toContain('UiDrawer')
    }
  })

  it('retains exactly one API/mutation and filter lifecycle across layout switches', () => {
    for (const symbol of [
      'useQuestionTopicContext',
      'useQuestionsList',
      'useQuestionEditorState',
      'useQuestionMutations',
      'useQuestionImport',
      'useQuestionFilters',
      'useQuestionOptions',
    ]) {
      expect(controller).toContain(symbol)
    }
    expect(controller).toContain('return reactive({')
    expect(controller).toContain('selectedTopicId,')
    expect(controller).toContain('form, formError, formFieldErrors,')
    expect(controller).toContain('saveQuestion,')
    expect(controller).toContain('onMounted(async () => {')
  })

  it('uses distinct wide / medium / compact structures, with a real compact filter UX', () => {
    expect(variants.wide).toContain('questions-wide-sidebar')
    expect(variants.wide).toContain('grid-template-columns:')
    expect(variants.medium).toContain('questions-medium-workspace')
    expect(variants.compact).toContain('<QuestionsBank compact />')
    expect(bank).toContain('v-if="compact"')
    expect(bank).toContain('<UiSearchInput v-model="searchQuery"')
    expect(bank).toContain('<details class="questions-compact-more">')
    expect(bank).toContain('<UiFilterBar')
    expect(bank).toContain('<QuestionsList />')
  })

  it('preserves edit / toggle / import functionality and unsaved-change handling', () => {
    expect(context).toContain("name: 'teacher-topics'")
    expect(context).toContain("name: 'teacher-test-create'")
    expect(list).toContain('editQuestion(question)')
    expect(list).toContain('toggleQuestionActive(question)')
    expect(overlay).toContain('UiUnsavedChangesConfirm')
    expect(overlay).toContain('MatchingPairsEditor')
    expect(overlay).toContain(':ref="setQuestionFormElement"')
    expect(overlay).toContain('importWord')
    expect(overlay).toContain('saveQuestion')
    expect(controller).toContain('focusFirstInvalidField(')
  })
})
