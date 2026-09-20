import {
  describe,
  expect,
  it,
} from 'vitest'

import {
  shallowMount,
} from '@vue/test-utils'

import {
  UiTable,
} from '@/components/ui'
import ResultAttemptCard from '@/components/results/ResultAttemptCard.vue'

function attempt() {
  return {
    testId: 4,
    testName: 'Контрольная',
    attemptOrdinal: 1,
    stats: {
      total: 1,
      right: 1,
      percent: 100,
    },
    results: [
      {
        questionText: '2 + 2?',
        givenAnswer: '4',
        correctAnswer: '4',
        correct: true,
      },
    ],
  }
}

function columnLabels(mode) {
  const wrapper = shallowMount(
    ResultAttemptCard,
    {
      props: {
        attempt: attempt(),
        mode,
        open: true,
      },
    }
  )

  const table = wrapper.findComponent(
    UiTable
  )

  expect(table.exists()).toBe(true)

  const labels = table
    .props('columns')
    .map((column) => column.label)

  wrapper.unmount()
  return labels
}

describe('result attempt visibility', () => {
  it('does not expose per-question correctness columns in student mode', () => {
    const labels = columnLabels('student')

    expect(labels).toEqual([
      '#',
      'Вопрос',
      'Ответ студента',
    ])
    expect(labels).not.toContain(
      'Правильный ответ'
    )
    expect(labels).not.toContain(
      'Результат'
    )
  })

  it('keeps correctness columns in teacher mode', () => {
    const labels = columnLabels('teacher')

    expect(labels).toContain(
      'Правильный ответ'
    )
    expect(labels).toContain(
      'Результат'
    )
  })
})
