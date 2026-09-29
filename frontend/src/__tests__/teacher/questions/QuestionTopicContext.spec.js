import {
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
} = vi.hoisted(() => ({
  getAll: vi.fn(),
}))

vi.mock('@/api', () => ({
  getApiErrorMessage: (error, fallback) =>
    error?.message || fallback,
  topicsApi: {
    getAll,
  },
}))

import {
  useQuestionTopicContext,
} from '@/composables/questions/useQuestionTopicContext'

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
  topicId,
  membershipId = 10,
} = {}) {
  const selectedMembership = ref(
    membershipId
      ? { id: membershipId }
      : null
  )
  const notice = ref({
    type: 'info',
    message: '',
  })

  const state = useQuestionTopicContext({
    route: {
      query: topicId
        ? { topicId }
        : {},
    },
    selectedMembership,
    notice,
  })

  return {
    ...state,
    selectedMembership,
    notice,
  }
}

describe('useQuestionTopicContext', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('loads sorted topics for the selected teacher membership', async () => {
    getAll.mockResolvedValue({
      data: [
        { id: 2, ordinal: 2, name: 'Графы' },
        { id: 1, ordinal: 1, name: 'Сортировки' },
      ],
    })

    const state = createState()

    await state.loadTopics()

    expect(getAll).toHaveBeenCalledWith({
      subjectMembershipId: 10,
    })
    expect(state.topics.value.map((topic) => topic.id))
      .toEqual([1, 2])
    expect(state.topicOptions.value).toEqual([
      { value: 1, label: '1. Сортировки' },
      { value: 2, label: '2. Графы' },
    ])
  })

  it('restores a topic deep-link only when it belongs to the loaded topic list', async () => {
    getAll.mockResolvedValue({
      data: [
        { id: 4, ordinal: 1, name: 'Массивы' },
        { id: 7, ordinal: 2, name: 'Деревья' },
      ],
    })

    const state = createState({
      topicId: '7',
    })

    await state.loadTopics()

    expect(state.selectedTopicId.value).toBe('7')
    expect(state.selectedImportTopicId.value).toBe('7')
    expect(state.currentTopic.value).toEqual(
      expect.objectContaining({ id: 7 })
    )
  })

  it('auto-selects the only available topic', async () => {
    getAll.mockResolvedValue({
      data: [
        { id: 15, ordinal: 1, name: 'Единственная тема' },
      ],
    })

    const state = createState()

    await state.loadTopics()

    expect(state.selectedTopicId.value).toBe('15')
    expect(state.selectedImportTopicId.value).toBe('15')
  })

  it('ignores stale responses after membership context changes', async () => {
    const first = deferred()
    const second = deferred()

    getAll
      .mockReturnValueOnce(first.promise)
      .mockReturnValueOnce(second.promise)

    const state = createState()

    const firstLoad = state.loadTopics()

    state.selectedMembership.value = { id: 11 }
    const secondLoad = state.loadTopics()

    second.resolve({
      data: [
        { id: 11, ordinal: 1, name: 'Новый контекст' },
      ],
    })
    await secondLoad

    first.resolve({
      data: [
        { id: 10, ordinal: 1, name: 'Старый контекст' },
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

  it('clears topic context without calling the API when membership is absent', async () => {
    const state = createState({
      membershipId: null,
    })

    state.topics.value = [
      { id: 1, ordinal: 1, name: 'Старая тема' },
    ]
    state.selectedTopicId.value = '1'
    state.selectedImportTopicId.value = '1'

    await state.loadTopics()

    expect(state.topics.value).toEqual([])
    expect(state.selectedTopicId.value).toBe('')
    expect(state.selectedImportTopicId.value).toBe('')
    expect(getAll).not.toHaveBeenCalled()
  })

  it('reports the current load error through the shared notice', async () => {
    getAll.mockRejectedValue(
      new Error('Темы временно недоступны')
    )

    const state = createState()

    await state.loadTopics()

    expect(state.notice.value).toEqual({
      type: 'danger',
      message: 'Темы временно недоступны',
    })
  })
})
