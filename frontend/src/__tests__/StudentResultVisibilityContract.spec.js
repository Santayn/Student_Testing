import {
  describe,
  expect,
  it,
} from 'vitest'

import {
  shallowMount,
} from '@vue/test-utils'

import {
  UiTag,
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
        correctAnswer: 'Секретный правильный ответ',
        correct: true,
        questionPoints: 2,
        awardedPoints: 2,
        gradingStatus: 'correct',
        gradingNote: 'Скрытая заметка',
      },
    ],
  }
}

describe('result attempt visibility', () => {
  it('renders student answers as responsive cards without teacher-only grading data', () => {
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

    expect(
      wrapper.findAll('.result-answer')
    ).toHaveLength(1)

    expect(wrapper.text()).toContain('2 + 2?')
    expect(wrapper.text()).toContain('Ответ студента')
    expect(wrapper.text()).not.toContain('Правильный ответ')
    expect(wrapper.text()).not.toContain('Секретный правильный ответ')
    expect(wrapper.text()).not.toContain('Скрытая заметка')
    expect(
      wrapper.find('.result-answer__data--teacher').exists()
    ).toBe(false)

    wrapper.unmount()
  })

  it('keeps teacher grading data in the teacher card layout', () => {
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

    expect(wrapper.text()).toContain('Правильный ответ')
    expect(wrapper.text()).toContain('Секретный правильный ответ')
    expect(wrapper.text()).toContain('2 из 2')
    expect(
      wrapper.find('.result-answer__data--teacher').exists()
    ).toBe(true)

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

    const statusTag = wrapper
      .findAllComponents(UiTag)
      .find(
        (tag) =>
          tag.props('value') === 'Частично верно'
      )

    expect(statusTag).toBeTruthy()
    expect(statusTag.props('variant')).toBe('warning')
    expect(wrapper.text()).toContain('5 из 10')

    wrapper.unmount()
  })
})
