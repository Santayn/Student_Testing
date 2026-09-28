import {
  describe,
  expect,
  it,
} from 'vitest'
import { ref } from 'vue'

import {
  useTestEditorState,
} from '@/composables/useTestEditorState'

function fixture() {
  const selectedMembershipId = ref('10')
  const selectedSubjectId = ref('20')
  const selectedMembership = ref({ id: 10 })
  const selectedTopicId = ref('30')
  const selectedGroupIds = ref([2])
  const topics = ref([
    { id: 30, ordinal: 1, name: 'Тема' },
  ])
  const questions = ref([
    { id: 1, active: true, type: 1 },
    { id: 2, active: true, type: 2 },
    { id: 3, active: true, type: 3 },
    { id: 4, active: true, type: 4 },
  ])

  const state = useTestEditorState({
    selectedMembershipId,
    selectedSubjectId,
    selectedMembership,
    selectedTopicId,
    selectedGroupIds,
    topics,
    questions,
  })

  state.setDefaultDates(
    new Date('2026-09-28T10:00:00Z')
  )

  return {
    state,
    selectedMembershipId,
    selectedSubjectId,
    selectedMembership,
    selectedTopicId,
    selectedGroupIds,
    topics,
    questions,
  }
}

describe('useTestEditorState', () => {
  it('tracks form and context changes in dirty state', () => {
    const {
      state,
      selectedGroupIds,
    } = fixture()

    state.markEditorClean()
    expect(state.editorDirty.value).toBe(false)

    state.form.value.title = 'Новый тест'
    expect(state.editorDirty.value).toBe(true)

    state.markEditorClean()
    selectedGroupIds.value = [2, 5]
    expect(state.editorDirty.value).toBe(true)
  })

  it('can accept asynchronously loaded context without resetting the form baseline', () => {
    const {
      state,
      selectedMembershipId,
      selectedTopicId,
      selectedGroupIds,
    } = fixture()

    state.markEditorClean()

    selectedMembershipId.value = '11'
    selectedTopicId.value = '31'
    selectedGroupIds.value = [7]

    state.markInitialContextClean()

    expect(state.editorDirty.value).toBe(false)

    state.form.value.title = 'Изменено пользователем'
    expect(state.editorDirty.value).toBe(true)
  })

  it('validates question counts against active questions', () => {
    const { state } = fixture()

    state.form.value.title = 'Тест'
    state.form.value.questionCount = 4
    state.form.value.singleAnswerQuestionCount = 2

    expect(state.validationError()).toBe(
      'В теме недостаточно вопросов выбранных типов.'
    )

    state.form.value.singleAnswerQuestionCount = 1
    state.form.value.multipleAnswerQuestionCount = 1
    state.form.value.matchingQuestionCount = 1
    state.form.value.textQuestionCount = 1

    expect(state.validationError()).toBe('')
  })

  it('builds contextual navigation query from current selection', () => {
    const { state } = fixture()

    expect(state.routeQuery()).toEqual({
      subjectId: '20',
      subjectMembershipId: 10,
      topicId: '30',
    })
  })
})
