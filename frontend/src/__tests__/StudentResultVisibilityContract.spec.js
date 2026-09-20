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
        questionPoints: 2,
        awardedPoints: 2,
        gradingStatus: 'correct',
        gradingNote: 'Скрытая заметка',
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
    expect(labels).not.toContain(
      'Баллы'
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
    expect(labels).toContain(
      'Баллы'
    )
  })


  it('removes teacher-only row data before UiTable receives student rows', () => {
    const wrapper = shallowMount(
      ResultAttemptCard,
      {
        props: {
          attempt: attempt(),
          mode: 'student',
          open: true,
        },
      }
    )

    const table = wrapper.findComponent(
      UiTable
    )

    expect(table.props('rows')).toEqual([
      {
        displayIndex: 1,
        questionText: '2 + 2?',
        givenAnswer: '4',
      },
    ])

    wrapper.unmount()
  })

  it('keeps teacher grading row data in teacher mode', () => {
    const wrapper = shallowMount(
      ResultAttemptCard,
      {
        props: {
          attempt: attempt(),
          mode: 'teacher',
          open: true,
        },
      }
    )

    const table = wrapper.findComponent(
      UiTable
    )

    expect(table.props('rows')[0]).toMatchObject({
      correctAnswer: '4',
      correct: true,
      questionPoints: 2,
      awardedPoints: 2,
      gradingStatus: 'correct',
      gradingNote: 'Скрытая заметка',
    })

    wrapper.unmount()
  })

  it('represents partial grading explicitly for teacher mode', () => {
    const wrapper = shallowMount(
      ResultAttemptCard,
      {
        props: {
          attempt: {
            ...attempt(),
            results: [
              {
                questionText: 'Опишите нормализацию',
                givenAnswer: 'Краткий ответ',
                correctAnswer: 'Полный ответ',
                correct: false,
                questionPoints: 10,
                awardedPoints: 5,
                gradingStatus: 'partial',
              },
            ],
          },
          mode: 'teacher',
          open: true,
        },
      }
    )

    const table = wrapper.findComponent(
      UiTable
    )

    const statusColumn = table
      .props('columns')
      .find(
        (column) =>
          column.key === 'status'
      )

    const pointsColumn = table
      .props('columns')
      .find(
        (column) =>
          column.key === 'points'
      )

    const row = wrapper.props(
      'attempt'
    ).results[0]

    expect(
      statusColumn.value(row)
    ).toBe('Частично верно')

    expect(
      pointsColumn.value(row)
    ).toBe('5 / 10')

    wrapper.unmount()
  })
})
