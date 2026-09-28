import { ref } from 'vue'

import {
  describe,
  expect,
  it,
} from 'vitest'

import {
  questionToForm,
  useQuestionEditorState,
} from '@/composables/useQuestionEditorState'

function setup() {
  const selectedMembership = ref({ id: 7 })
  const selectedTopicId = ref('10')
  const state = useQuestionEditorState({
    selectedMembership,
    selectedTopicId,
  })

  return {
    selectedMembership,
    selectedTopicId,
    ...state,
  }
}

describe('question editor state', () => {
  it('normalizes server questions and matching rows for the editor', () => {
    expect(questionToForm({
      id: 5,
      question: 'Match',
      type: 3,
      points: 2,
      ordinal: 4,
      matchingPairs: [
        { ordinal: 2, left: 'B', right: '2' },
        { ordinal: 1, left: 'A', right: '1' },
      ],
      active: false,
    })).toMatchObject({
      id: 5,
      question: 'Match',
      type: 3,
      points: 2,
      ordinal: 4,
      active: false,
      matchingPairs: [
        { ordinal: 1, left: 'B', right: '2' },
        { ordinal: 2, left: 'A', right: '1' },
      ],
    })
  })

  it('owns the overlay dirty lifecycle without changing create/edit semantics', () => {
    const state = setup()

    state.openCreate({ ordinal: 3 })
    expect(state.isCreate.value).toBe(true)
    expect(state.drawerTitle.value).toBe('Новый вопрос')
    expect(state.dirty.value).toBe(false)

    state.form.question = 'Новый вопрос'
    expect(state.dirty.value).toBe(true)

    state.openEdit({
      id: 8,
      question: 'Старый вопрос',
      type: 4,
      points: 1,
      ordinal: 1,
      correctAnswer: 'Ответ',
      active: true,
    })

    expect(state.isCreate.value).toBe(false)
    expect(state.drawerTitle.value).toBe('Редактирование вопроса')
    expect(state.isTextType.value).toBe(true)
    expect(state.dirty.value).toBe(false)
  })

  it('keeps the existing question validation rules', () => {
    const state = setup()
    state.openCreate()

    expect(state.validationMessage()).toBe('Введите текст вопроса.')

    state.form.question = 'Вопрос'
    state.form.points = -1
    expect(state.validationMessage()).toContain('не меньше нуля')

    state.form.points = 1
    state.form.ordinal = 0
    expect(state.validationMessage()).toContain('больше нуля')

    state.form.ordinal = 1
    expect(state.validationMessage()).toBe('')

    state.selectedMembership.value = null
    expect(state.validationMessage()).toBe('Выберите предмет и тему.')
  })

  it('validates matching questions through the shared pair rules', () => {
    const state = setup()
    state.openCreate({
      question: 'Сопоставьте',
      type: 3,
      points: 1,
      ordinal: 1,
      matchingPairs: [
        { ordinal: 1, left: '', right: '' },
        { ordinal: 2, left: '', right: '' },
      ],
    })

    expect(state.isMatchingType.value).toBe(true)
    expect(state.validationMessage()).not.toBe('')
  })
})
