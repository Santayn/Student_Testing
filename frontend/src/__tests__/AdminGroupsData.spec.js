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
  groupsApi: {
    getAll: vi.fn(),
  },
  getApiErrorMessage: vi.fn(
    (error, fallback) => error?.message || fallback
  ),
}))

import {
  facultiesApi,
  getApiErrorMessage,
  groupsApi,
} from '@/api'

import {
  useAdminGroupsData,
} from '@/composables/useAdminGroupsData'

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

describe('admin groups data state', () => {
  beforeEach(() => {
    vi.resetAllMocks()
    facultiesApi.getAll.mockResolvedValue({ data: [] })
    groupsApi.getAll.mockResolvedValue({ data: [] })
  })

  it('loads faculties and groups and keeps presentation helpers on the data boundary', async () => {
    facultiesApi.getAll.mockResolvedValue({
      data: [
        { id: 2, name: 'Инженерный' },
        { id: 1, name: 'Физико-математический' },
      ],
    })
    groupsApi.getAll.mockResolvedValue({
      data: [
        { id: 10, name: 'Группа Б', code: 'B-01', facultyId: 2 },
        { id: 20, name: 'Группа А', code: 'A-01', facultyId: 1 },
      ],
    })

    const state = useAdminGroupsData()
    await state.loadData()

    expect(state.facultyOptions.value.map((option) => option.value)).toEqual(['2', '1'])
    expect(state.facultyName(1)).toBe('Физико-математический')
    expect(state.filteredGroups.value.map((group) => group.id)).toEqual([20, 10])
    expect(state.loading.value).toBe(false)
  })

  it('combines search, faculty filter and sort locally without new API reads', async () => {
    facultiesApi.getAll.mockResolvedValue({
      data: [
        { id: 1, name: 'ИТ' },
        { id: 2, name: 'Экономика' },
      ],
    })
    groupsApi.getAll.mockResolvedValue({
      data: [
        { id: 1, name: 'Разработка', code: 'DEV-02', facultyId: 1 },
        { id: 2, name: 'Аналитика', code: 'AN-01', facultyId: 1 },
        { id: 3, name: 'Финансы', code: 'FIN-01', facultyId: 2 },
      ],
    })

    const state = useAdminGroupsData()
    await state.loadData()

    state.facultyFilter.value = '1'
    state.searchQuery.value = 'а'
    state.sortMode.value = 'code-asc'

    expect(state.filteredGroups.value.map((group) => group.id)).toEqual([2, 1])
    expect(state.hasActiveFilters.value).toBe(true)
    expect(facultiesApi.getAll).toHaveBeenCalledTimes(1)
    expect(groupsApi.getAll).toHaveBeenCalledTimes(1)

    state.resetFilters()

    expect(state.hasActiveFilters.value).toBe(false)
    expect(state.filteredGroups.value.map((group) => group.id)).toEqual([2, 1, 3])
  })

  it('sorts by faculty label with group name as a stable secondary key', async () => {
    facultiesApi.getAll.mockResolvedValue({
      data: [
        { id: 1, name: 'Биология' },
        { id: 2, name: 'Архитектура' },
      ],
    })
    groupsApi.getAll.mockResolvedValue({
      data: [
        { id: 1, name: 'Бета', code: 'B', facultyId: 1 },
        { id: 2, name: 'Гамма', code: 'C', facultyId: 2 },
        { id: 3, name: 'Альфа', code: 'A', facultyId: 2 },
      ],
    })

    const state = useAdminGroupsData()
    await state.loadData()
    state.sortMode.value = 'faculty-asc'

    expect(state.filteredGroups.value.map((group) => group.id)).toEqual([3, 2, 1])
  })

  it('ignores stale data when a newer refresh finishes first', async () => {
    const facultiesOld = deferred()
    const groupsOld = deferred()
    const facultiesNew = deferred()
    const groupsNew = deferred()

    facultiesApi.getAll
      .mockReturnValueOnce(facultiesOld.promise)
      .mockReturnValueOnce(facultiesNew.promise)
    groupsApi.getAll
      .mockReturnValueOnce(groupsOld.promise)
      .mockReturnValueOnce(groupsNew.promise)

    const state = useAdminGroupsData()
    const oldLoad = state.loadData()
    const newLoad = state.loadData()

    facultiesNew.resolve({ data: [{ id: 2, name: 'Новый факультет' }] })
    groupsNew.resolve({ data: [{ id: 2, name: 'Новая группа', code: 'NEW', facultyId: 2 }] })
    await newLoad

    facultiesOld.resolve({ data: [{ id: 1, name: 'Старый факультет' }] })
    groupsOld.resolve({ data: [{ id: 1, name: 'Старая группа', code: 'OLD', facultyId: 1 }] })
    await oldLoad

    expect(state.faculties.value.map((faculty) => faculty.id)).toEqual([2])
    expect(state.groups.value.map((group) => group.id)).toEqual([2])
    expect(state.loading.value).toBe(false)
  })

  it('reports only the current load failure through the existing notice state', async () => {
    groupsApi.getAll.mockRejectedValue(new Error('Сеть недоступна'))

    const state = useAdminGroupsData()
    await state.loadData()

    expect(state.notice.value).toEqual({
      type: 'error',
      message: 'Сеть недоступна',
    })
    expect(getApiErrorMessage).toHaveBeenCalled()
    expect(state.loading.value).toBe(false)
  })
})
