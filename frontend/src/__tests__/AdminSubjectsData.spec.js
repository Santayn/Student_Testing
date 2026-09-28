import {
  beforeEach,
  describe,
  expect,
  it,
  vi,
} from 'vitest'

vi.mock('@/api', () => ({
  subjectsApi: {
    getAll: vi.fn(),
  },
  getApiErrorMessage: vi.fn(
    (error, fallback) => error?.message || fallback
  ),
}))

import {
  getApiErrorMessage,
  subjectsApi,
} from '@/api'

import {
  useAdminSubjectsData,
} from '@/composables/useAdminSubjectsData'

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

describe('admin subjects data state', () => {
  beforeEach(() => {
    vi.resetAllMocks()
    subjectsApi.getAll.mockResolvedValue({ data: [] })
  })

  it('loads subjects and sorts them by name by default', async () => {
    subjectsApi.getAll.mockResolvedValue({
      data: [
        { id: 2, name: 'Экономика', description: null },
        { id: 1, name: 'Информатика', description: 'ИТ' },
      ],
    })

    const state = useAdminSubjectsData()
    await state.loadSubjects()

    expect(state.filteredSubjects.value.map((subject) => subject.id)).toEqual([1, 2])
    expect(state.filterResultText.value).toBe('Показано: 2 из 2')
    expect(state.loading.value).toBe(false)
  })

  it('combines search, description filter and sort locally without new API reads', async () => {
    subjectsApi.getAll.mockResolvedValue({
      data: [
        { id: 1, name: 'Информатика', description: 'Разработка' },
        { id: 2, name: 'Экономика', description: '' },
        { id: 3, name: 'Инженерия', description: 'Проектирование' },
      ],
    })

    const state = useAdminSubjectsData()
    await state.loadSubjects()

    state.descriptionFilter.value = 'with-description'
    state.searchQuery.value = 'ин'
    state.sortMode.value = 'name-desc'

    expect(state.filteredSubjects.value.map((subject) => subject.id)).toEqual([1, 3])
    expect(state.hasActiveFilters.value).toBe(true)
    expect(subjectsApi.getAll).toHaveBeenCalledTimes(1)

    state.resetFilters()

    expect(state.hasActiveFilters.value).toBe(false)
    expect(state.filteredSubjects.value.map((subject) => subject.id)).toEqual([3, 1, 2])
  })

  it('ignores stale refresh data when a newer request finishes first', async () => {
    const oldRequest = deferred()
    const newRequest = deferred()

    subjectsApi.getAll
      .mockReturnValueOnce(oldRequest.promise)
      .mockReturnValueOnce(newRequest.promise)

    const state = useAdminSubjectsData()
    const oldLoad = state.loadSubjects()
    const newLoad = state.loadSubjects()

    newRequest.resolve({
      data: [{ id: 2, name: 'Новый' }],
    })
    await newLoad

    oldRequest.resolve({
      data: [{ id: 1, name: 'Старый' }],
    })
    await oldLoad

    expect(state.subjects.value.map((subject) => subject.id)).toEqual([2])
    expect(state.loading.value).toBe(false)
  })

  it('reports the current load failure through the existing notice state', async () => {
    subjectsApi.getAll.mockRejectedValue(
      new Error('Сеть недоступна')
    )

    const state = useAdminSubjectsData()
    await state.loadSubjects()

    expect(state.notice.value).toEqual({
      type: 'error',
      message: 'Сеть недоступна',
    })
    expect(getApiErrorMessage).toHaveBeenCalled()
    expect(state.loading.value).toBe(false)
  })
})
