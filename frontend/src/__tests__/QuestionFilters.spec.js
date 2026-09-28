import { ref } from 'vue'

import {
  describe,
  expect,
  it,
} from 'vitest'

import { useQuestionFilters } from '@/composables/useQuestionFilters'

function setup() {
  const selectedTopicId = ref('10')
  const questions = ref([
    {
      id: 1,
      ordinal: 2,
      question: 'Что такое HTTP?',
      type: 1,
      points: 2,
      active: true,
      correctAnswer: 'Протокол',
    },
    {
      id: 2,
      ordinal: 1,
      question: 'Опишите DNS',
      type: 4,
      points: 5,
      active: false,
      correctAnswer: 'Система доменных имён',
    },
    {
      id: 3,
      ordinal: 3,
      question: 'Сопоставьте термины',
      type: 3,
      points: 3,
      active: true,
      correctAnswer: null,
    },
  ])

  const filters = useQuestionFilters({
    questions,
    selectedTopicId,
    questionTypeLabel(type) {
      return {
        1: 'Один вариант',
        3: 'Соответствие',
        4: 'Текстовый ответ',
      }[Number(type)] ?? `Тип ${type}`
    },
    questionDisplayAnswer(question) {
      return Number(question.type) === 3
        ? ''
        : question.correctAnswer || ''
    },
  })

  return {
    questions,
    selectedTopicId,
    ...filters,
  }
}

describe('question bank filters', () => {
  it('keeps the default ordinal ordering and reports topic statistics', () => {
    const state = setup()

    expect(
      state.filteredQuestions.value.map((item) => item.id)
    ).toEqual([2, 1, 3])
    expect(state.questionStats.value).toBe(
      'Всего 3. Активных 2. Скрытых 1.'
    )
    expect(state.filterResultText.value).toBe('Показано: 3 из 3')
  })

  it('combines search, type and status filters without mutating source questions', () => {
    const state = setup()
    const original = [...state.questions.value]

    state.searchQuery.value = 'dns'
    state.typeFilter.value = 4
    state.statusFilter.value = 'hidden'

    expect(
      state.filteredQuestions.value.map((item) => item.id)
    ).toEqual([2])
    expect(state.questions.value).toEqual(original)
    expect(state.hasActiveFilters.value).toBe(true)
  })

  it('searches by presentation labels and answer text', () => {
    const state = setup()

    state.searchQuery.value = 'текстовый ответ'
    expect(
      state.filteredQuestions.value.map((item) => item.id)
    ).toEqual([2])

    state.searchQuery.value = 'протокол'
    expect(
      state.filteredQuestions.value.map((item) => item.id)
    ).toEqual([1])
  })

  it('supports the existing sort modes and resets the workspace filters', () => {
    const state = setup()

    state.sortMode.value = 'points-desc'
    expect(
      state.filteredQuestions.value.map((item) => item.id)
    ).toEqual([2, 3, 1])

    state.searchQuery.value = 'http'
    state.typeFilter.value = 1
    state.statusFilter.value = 'active'

    state.resetFilters()

    expect(state.searchQuery.value).toBe('')
    expect(state.typeFilter.value).toBe('all')
    expect(state.statusFilter.value).toBe('all')
    expect(state.sortMode.value).toBe('ordinal')
    expect(state.hasActiveFilters.value).toBe(false)
  })

  it('keeps the empty-topic helper text', () => {
    const state = setup()

    state.selectedTopicId.value = ''

    expect(state.questionStats.value).toBe(
      'Выберите тему, чтобы открыть банк вопросов.'
    )
    expect(state.filterResultText.value).toBe('Сначала выберите тему.')
  })
})
