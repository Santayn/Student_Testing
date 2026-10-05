import {
  afterAll,
  beforeEach,
  describe,
  expect,
  it,
  vi,
} from 'vitest'
import {
  config,
  flushPromises,
  shallowMount,
} from '@vue/test-utils'

const state = vi.hoisted(() => ({
  auth: {
    isStudent: true,
    isTeacher: false,
    isAdmin: false,
    isStudentMode: true,
    isTeacherMode: false,
    isAdminMode: false,
  },
}))

vi.mock('@/stores/auth', () => ({
  useAuthStore: () => state.auth,
}))

vi.mock('@/api', () => ({
  getApiErrorMessage: (
    _error,
    fallback
  ) => fallback,
  lecturesApi: {
    getAll: vi.fn(),
    getTests: vi.fn(),
  },
  subjectsApi: {
    getAll: vi.fn(),
  },
  resultsApi: {
    getStudentSubjects: vi.fn(),
    getStudentData: vi.fn(),
    getTeacherSubjects: vi.fn(),
    getTeacherLectures: vi.fn(),
    getTeacherTests: vi.fn(),
    getTeacherGroups: vi.fn(),
    getTeacherStudents: vi.fn(),
    getTeacherData: vi.fn(),
  },
}))

import {
  resultsApi,
} from '@/api'
import ResultAttemptCard from '@/components/results/ResultAttemptCard.vue'
import ResultsView from '@/views/results/ResultsView.vue'

function attempt({
  attemptId,
  attemptOrdinal,
  legacyPercent,
  awardedPoints,
}) {
  return {
    attemptId,
    testId: 7,
    testName: 'Контрольная',
    attemptOrdinal,
    completedAt:
      `2026-09-20T1${attemptOrdinal}:00:00Z`,
    stats: {
      total: 10,
      right: legacyPercent / 10,
      percent: legacyPercent,
    },
    results: [
      {
        questionText: 'Текстовый вопрос',
        givenAnswer: 'Ответ',
        correctAnswer: 'Эталон',
        correct: false,
        questionPoints: 10,
        awardedPoints,
        gradingStatus:
          awardedPoints > 0
            ? 'partial'
            : 'incorrect',
        gradingNote: 'Скрытая заметка',
      },
    ],
  }
}

describe('student best attempt scoring', () => {
  const previousRenderStubDefaultSlot =
    config.global.renderStubDefaultSlot

  beforeEach(() => {
    config.global.renderStubDefaultSlot = true
    vi.clearAllMocks()

    resultsApi
      .getStudentSubjects
      .mockResolvedValue({
        data: [],
      })

    resultsApi
      .getStudentData
      .mockResolvedValue({
        data: {
          stats: {
            total: 20,
            right: 9,
            percent: 45,
          },
          attemptCount: 2,
          attempts: [
            attempt({
              attemptId: 101,
              attemptOrdinal: 1,
              legacyPercent: 40,
              awardedPoints: 7,
            }),
            attempt({
              attemptId: 102,
              attemptOrdinal: 2,
              legacyPercent: 50,
              awardedPoints: 5,
            }),
          ],
        },
      })
  })

  afterAll(() => {
    config.global.renderStubDefaultSlot =
      previousRenderStubDefaultSlot
  })

  it('marks the attempt with the highest awarded-points percentage as best', async () => {
    const wrapper = shallowMount(
      ResultsView
    )

    await flushPromises()

    const cards = wrapper.findAllComponents(
      ResultAttemptCard
    )

    expect(cards).toHaveLength(2)

    const bestCard = cards.find(
      (card) =>
        card.props('best') === true
    )

    expect(bestCard).toBeTruthy()
    expect(
      bestCard.props('attempt')
        .attemptId
    ).toBe(101)

    expect(
      bestCard.props('attempt')
    ).toMatchObject({
      score: 7,
      maxScore: 10,
      scorePercent: 70,
    })

    expect(
      bestCard.props('attempt')
        .results
    ).toEqual([
      {
        questionText: 'Текстовый вопрос',
        givenAnswer: 'Ответ',
      },
    ])

    wrapper.unmount()
  })
})
