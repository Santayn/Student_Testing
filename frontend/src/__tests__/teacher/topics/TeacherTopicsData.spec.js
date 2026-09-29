import {
  nextTick,
  ref,
} from 'vue'

import {
  beforeEach,
  describe,
  expect,
  it,
  vi,
} from 'vitest'

const {
  getAll,
  getOne,
} = vi.hoisted(() => ({
  getAll: vi.fn(),
  getOne: vi.fn(),
}))

vi.mock('@/api', () => ({
  getApiErrorMessage: (error, fallback) =>
    error?.message || fallback,
  topicsApi: {
    getAll,
    getOne,
  },
}))

import {
  useTeacherTopicsData,
} from '@/composables/teacher/useTeacherTopicsData'

function deferred() {
  let resolve
  let reject

  const promise = new Promise((res, rej) => {
    resolve = res
    reject = rej
  })

  return {
    promise,
    resolve,
    reject,
  }
}

function createState({
  routeQuery = {},
} = {}) {
  const selectedMembership = ref({
    id: 10,
    subjectId: 20,
  })
  const selectedSubjectId = ref(20)
  const selectedSubject = ref({
    id: 20,
    name: 'Алгоритмы',
  })
  const onOpenRouteTopic = vi.fn()

  const data = useTeacherTopicsData({
    route: {
      query: routeQuery,
    },
    selectedMembership,
    selectedSubjectId,
    selectedSubject,
    onOpenRouteTopic,
  })

  return {
    ...data,
    selectedMembership,
    selectedSubjectId,
    selectedSubject,
    onOpenRouteTopic,
  }
}

describe('useTeacherTopicsData', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('loads topics for the selected membership and keeps filtering local', async () => {
    getAll.mockResolvedValue({
      data: [
        {
          id: 2,
          ordinal: 2,
          name: 'Графы',
          description: null,
        },
        {
          id: 1,
          ordinal: 1,
          name: 'Сортировки',
          description: 'Базовые алгоритмы',
        },
      ],
    })

    const state = createState()

    await state.loadTopics()

    expect(getAll).toHaveBeenCalledWith({
      subjectMembershipId: 10,
    })
    expect(state.topics.value.map((topic) => topic.id))
      .toEqual([1, 2])
    expect(state.contextHint.value)
      .toContain('Алгоритмы')

    state.descriptionFilter.value = 'with-description'
    state.searchQuery.value = 'сорт'
    await nextTick()

    expect(state.filteredTopics.value.map((topic) => topic.id))
      .toEqual([1])
    expect(getAll).toHaveBeenCalledTimes(1)
  })

  it('opens a route topic only after it is verified against the membership', async () => {
    getAll.mockResolvedValue({
      data: [],
    })
    getOne.mockResolvedValue({
      data: {
        id: 7,
        ordinal: 3,
        name: 'Деревья',
        subjectMembershipId: 10,
      },
    })

    const state = createState({
      routeQuery: {
        topicId: '7',
      },
    })

    await state.loadTopics({
      openRouteTopic: true,
    })

    expect(getOne).toHaveBeenCalledWith('7')
    expect(state.onOpenRouteTopic).toHaveBeenCalledTimes(1)
    expect(state.onOpenRouteTopic).toHaveBeenCalledWith(
      expect.objectContaining({
        id: 7,
        subjectMembershipId: 10,
      })
    )
  })

  it('ignores stale topic responses after the membership context changes', async () => {
    const first = deferred()
    const second = deferred()

    getAll
      .mockReturnValueOnce(first.promise)
      .mockReturnValueOnce(second.promise)

    const state = createState()

    const firstLoad = state.loadTopics()

    state.selectedMembership.value = {
      id: 11,
      subjectId: 21,
    }
    state.selectedSubjectId.value = 21

    const secondLoad = state.loadTopics()

    second.resolve({
      data: [
        {
          id: 11,
          ordinal: 1,
          name: 'Новый контекст',
        },
      ],
    })
    await secondLoad

    first.resolve({
      data: [
        {
          id: 10,
          ordinal: 1,
          name: 'Старый контекст',
        },
      ],
    })
    await firstLoad

    expect(state.topics.value).toEqual([
      expect.objectContaining({
        id: 11,
        name: 'Новый контекст',
      }),
    ])
  })

  it('clears topics when there is no selected membership', async () => {
    const state = createState()
    state.topics.value = [
      {
        id: 1,
        ordinal: 1,
        name: 'Старая тема',
      },
    ]
    state.selectedMembership.value = null

    await state.loadTopics()

    expect(state.topics.value).toEqual([])
    expect(getAll).not.toHaveBeenCalled()
    expect(state.filterResultText.value)
      .toBe('Сначала выберите предмет преподавателя.')
  })
})
