import {
  beforeEach,
  describe,
  expect,
  it,
  vi,
} from 'vitest'

vi.mock('@/api', () => ({
  facultiesApi: {
    getAll: vi.fn(),
  },
  getApiErrorMessage: vi.fn(
    (error, fallback) => error?.message || fallback
  ),
}))

import {
  facultiesApi,
  getApiErrorMessage,
} from '@/api'

import {
  useAdminFacultiesData,
} from '@/composables/useAdminFacultiesData'

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

describe('admin faculties data state', () => {
  beforeEach(() => {
    vi.resetAllMocks()
    facultiesApi.getAll.mockResolvedValue({ data: [] })
  })

  it('loads faculties and sorts them by name by default', async () => {
    facultiesApi.getAll.mockResolvedValue({
      data: [
        { id: 2, name: 'Экономика', code: 'ECON', description: null },
        { id: 1, name: 'Информатика', code: 'IT', description: 'ИТ' },
      ],
    })

    const state = useAdminFacultiesData()
    await state.loadFaculties()

    expect(state.filteredFaculties.value.map((faculty) => faculty.id)).toEqual([1, 2])
    expect(state.filterResultText.value).toBe('Показано: 2 из 2')
    expect(state.loading.value).toBe(false)
  })

  it('combines search, description filter and sort locally without new API reads', async () => {
    facultiesApi.getAll.mockResolvedValue({
      data: [
        { id: 1, name: 'Информатика', code: 'IT', description: 'Разработка' },
        { id: 2, name: 'Экономика', code: 'ECON', description: '' },
        { id: 3, name: 'Инженерный', code: 'ENG', description: 'Проектирование' },
      ],
    })

    const state = useAdminFacultiesData()
    await state.loadFaculties()

    state.descriptionFilter.value = 'with-description'
    state.searchQuery.value = 'ин'
    state.sortMode.value = 'name-desc'

    expect(state.filteredFaculties.value.map((faculty) => faculty.id)).toEqual([1, 3])
    expect(state.hasActiveFilters.value).toBe(true)
    expect(facultiesApi.getAll).toHaveBeenCalledTimes(1)

    state.resetFilters()

    expect(state.hasActiveFilters.value).toBe(false)
    expect(state.filteredFaculties.value.map((faculty) => faculty.id)).toEqual([3, 1, 2])
  })

  it('ignores stale refresh data when a newer request finishes first', async () => {
    const oldRequest = deferred()
    const newRequest = deferred()

    facultiesApi.getAll
      .mockReturnValueOnce(oldRequest.promise)
      .mockReturnValueOnce(newRequest.promise)

    const state = useAdminFacultiesData()
    const oldLoad = state.loadFaculties()
    const newLoad = state.loadFaculties()

    newRequest.resolve({
      data: [{ id: 2, name: 'Новый', code: 'NEW' }],
    })
    await newLoad

    oldRequest.resolve({
      data: [{ id: 1, name: 'Старый', code: 'OLD' }],
    })
    await oldLoad

    expect(state.faculties.value.map((faculty) => faculty.id)).toEqual([2])
    expect(state.loading.value).toBe(false)
  })

  it('reports the current load failure through the existing notice state', async () => {
    facultiesApi.getAll.mockRejectedValue(
      new Error('Сеть недоступна')
    )

    const state = useAdminFacultiesData()
    await state.loadFaculties()

    expect(state.notice.value).toEqual({
      type: 'error',
      message: 'Сеть недоступна',
    })
    expect(getApiErrorMessage).toHaveBeenCalled()
    expect(state.loading.value).toBe(false)
  })
})
