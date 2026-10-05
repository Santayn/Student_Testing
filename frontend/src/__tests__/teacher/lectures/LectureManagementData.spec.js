import {
  beforeEach,
  describe,
  expect,
  it,
  vi,
} from 'vitest'
import { ref } from 'vue'

const {
  getLectures,
  getLectureTests,
  getTests,
  getApiErrorMessage,
} = vi.hoisted(() => ({
  getLectures: vi.fn(),
  getLectureTests: vi.fn(),
  getTests: vi.fn(),
  getApiErrorMessage: vi.fn((error, fallback) => error?.message || fallback),
}))

vi.mock('@/api', () => ({
  lecturesApi: {
    getAll: getLectures,
    getTests: getLectureTests,
  },
  testsApi: {
    getAll: getTests,
  },
  getApiErrorMessage,
}))

import { useLectureManagementData } from '@/composables/lectures/useLectureManagementData'

beforeEach(() => {
  getLectures.mockReset()
  getLectureTests.mockReset()
  getTests.mockReset()
  getApiErrorMessage.mockClear()
})

function deferred() {
  let resolve
  const promise = new Promise((res) => {
    resolve = res
  })
  return { promise, resolve }
}

function response(data) {
  return { data }
}

function createHarness({ lectureId = null } = {}) {
  const selectedMembership = ref({ id: 10 })
  const selectedSubjectId = ref(20)
  const notice = ref({ type: 'info', message: '' })
  const onOpenRouteLecture = vi.fn(async () => undefined)

  const state = useLectureManagementData({
    selectedMembership,
    selectedSubjectId,
    route: { query: lectureId ? { lectureId } : {} },
    notice,
    onOpenRouteLecture,
  })

  return {
    ...state,
    selectedMembership,
    selectedSubjectId,
    notice,
    onOpenRouteLecture,
  }
}

describe('lecture management data', () => {
  it('loads lectures, tests and per-lecture links for the selected membership', async () => {
    getLectures.mockResolvedValueOnce(response([
      { id: 2, ordinal: 2, title: 'Бета', publicVisible: false },
      { id: 1, ordinal: 1, title: 'Альфа', publicVisible: true },
    ]))
    getTests.mockResolvedValueOnce(response([
      { id: 8, title: 'Тест Б' },
      { id: 7, title: 'Тест А' },
    ]))
    getLectureTests
      .mockResolvedValueOnce(response([{ id: 7, title: 'Тест А' }]))
      .mockResolvedValueOnce(response([]))

    const state = createHarness()
    await state.loadLectures()

    expect(getLectures).toHaveBeenCalledWith(
      { subjectMembershipId: 10 },
      expect.objectContaining({ signal: expect.any(AbortSignal) })
    )
    expect(getTests).toHaveBeenCalledWith(
      { subjectId: 20 },
      expect.objectContaining({ signal: expect.any(AbortSignal) })
    )
    expect(state.lectures.value.map((item) => item.id)).toEqual([1, 2])
    expect(state.availableTests.value.map((item) => item.id)).toEqual([7, 8])
    expect(state.lectureTests(1).map((item) => item.id)).toEqual([7])
    expect(state.lectureTestSummary(2)).toBe('Нет связанных тестов')
  })

  it('opens a deep-linked lecture only once for the same membership and route id', async () => {
    getLectures.mockResolvedValue(response([
      { id: 5, ordinal: 1, title: 'Лекция' },
    ]))
    getTests.mockResolvedValue(response([]))
    getLectureTests.mockResolvedValue(response([]))

    const state = createHarness({ lectureId: '5' })

    await state.loadLectures({ openRouteLecture: true })
    await state.loadLectures({ openRouteLecture: true })

    expect(state.onOpenRouteLecture).toHaveBeenCalledTimes(1)
    expect(state.onOpenRouteLecture).toHaveBeenCalledWith(
      expect.objectContaining({ id: 5 })
    )

    state.resetRouteLectureHandling()
    await state.loadLectures({ openRouteLecture: true })
    expect(state.onOpenRouteLecture).toHaveBeenCalledTimes(2)
  })

  it('ignores stale responses after the membership context changes', async () => {
    const firstLectures = deferred()

    getLectures
      .mockReturnValueOnce(firstLectures.promise)
      .mockResolvedValueOnce(response([
        { id: 22, ordinal: 1, title: 'Новая лекция' },
      ]))
    getTests.mockResolvedValue(response([]))
    getLectureTests.mockResolvedValue(response([]))

    const state = createHarness()
    const firstLoad = state.loadLectures()

    state.selectedMembership.value = { id: 11 }
    state.selectedSubjectId.value = 21
    const secondLoad = state.loadLectures()

    firstLectures.resolve(response([
      { id: 10, ordinal: 1, title: 'Старая лекция' },
    ]))

    await Promise.all([firstLoad, secondLoad])

    expect(state.lectures.value.map((item) => item.id)).toEqual([22])
  })


  it('aborts the superseded lecture cascade and keeps the current loading state isolated', async () => {
    const first = deferred()
    const configs = []

    getLectures
      .mockImplementationOnce((_params, config) => {
        configs.push(config)
        return first.promise
      })
      .mockImplementationOnce((_params, config) => {
        configs.push(config)
        return Promise.resolve(response([
          { id: 22, ordinal: 1, title: 'Новая лекция' },
        ]))
      })
    getTests.mockResolvedValue(response([]))
    getLectureTests.mockResolvedValue(response([]))

    const state = createHarness()
    const firstLoad = state.loadLectures()

    state.selectedMembership.value = { id: 11 }
    state.selectedSubjectId.value = 21
    const secondLoad = state.loadLectures()

    expect(configs[0].signal.aborted).toBe(true)
    expect(configs[1].signal.aborted).toBe(false)

    await secondLoad
    expect(state.loading.value).toBe(false)
    expect(state.lectures.value.map((item) => item.id)).toEqual([22])

    first.resolve(response([
      { id: 10, ordinal: 1, title: 'Старая лекция' },
    ]))
    await firstLoad

    expect(state.lectures.value.map((item) => item.id)).toEqual([22])
    expect(state.notice.value.message).toBe('')
  })

  it('filters by visibility, linked tests and search text', async () => {
    getLectures.mockResolvedValueOnce(response([
      { id: 1, ordinal: 2, title: 'Закрытая', description: '', publicVisible: false },
      { id: 2, ordinal: 1, title: 'Открытая', description: 'Основы', publicVisible: true },
    ]))
    getTests.mockResolvedValueOnce(response([{ id: 4, title: 'Контрольный' }]))
    getLectureTests.mockImplementation((lectureId) => {
      return Promise.resolve(
        response(
          Number(lectureId) === 1
            ? [{ id: 4, title: 'Контрольный' }]
            : []
        )
      )
    })

    const state = createHarness()
    await state.loadLectures()

    state.visibilityFilter.value = 'visible'
    expect(state.filteredLectures.value.map((item) => item.id)).toEqual([2])

    state.visibilityFilter.value = 'all'
    state.testFilter.value = 'without-tests'
    expect(state.filteredLectures.value.map((item) => item.id)).toEqual([2])

    state.testFilter.value = 'all'
    state.searchQuery.value = 'контрольный'
    expect(state.filteredLectures.value.map((item) => item.id)).toEqual([1])
  })
})
