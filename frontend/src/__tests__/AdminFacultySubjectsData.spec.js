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
    getSubjects: vi.fn(),
  },
  subjectsApi: {
    getAll: vi.fn(),
  },
  getApiErrorMessage: vi.fn(
    (error, fallback) => error?.message || fallback
  ),
}))

import {
  facultiesApi,
  getApiErrorMessage,
  subjectsApi,
} from '@/api'

import {
  useAdminFacultySubjectsData,
} from '@/composables/useAdminFacultySubjectsData'

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

describe('admin faculty subjects data state', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    getApiErrorMessage.mockImplementation(
      (error, fallback) => error?.message || fallback
    )
    facultiesApi.getAll.mockResolvedValue({ data: [] })
    facultiesApi.getSubjects.mockResolvedValue({ data: [] })
    subjectsApi.getAll.mockResolvedValue({ data: [] })
  })

  it('loads and sorts faculties and subjects, then selects the first faculty', async () => {
    facultiesApi.getAll.mockResolvedValue({
      data: [
        { id: 2, name: 'Физический', code: 'ФИЗ' },
        { id: 1, name: 'Информатика', code: 'ИТ' },
      ],
    })
    subjectsApi.getAll.mockResolvedValue({
      data: [
        { id: 20, name: 'Физика' },
        { id: 10, name: 'Алгоритмы' },
      ],
    })

    const state = useAdminFacultySubjectsData()
    await state.loadBaseData()

    expect(state.faculties.value.map((faculty) => faculty.id)).toEqual([1, 2])
    expect(state.subjects.value.map((subject) => subject.id)).toEqual([10, 20])
    expect(state.facultyId.value).toBe('1')
    expect(state.selectedFaculty.value?.id).toBe(1)
    expect(state.facultyOptions.value[0]).toEqual({
      value: '1',
      label: 'Информатика (ИТ)',
    })
    expect(state.loadingBase.value).toBe(false)
  })

  it('loads assigned subjects and derives the available column', async () => {
    facultiesApi.getAll.mockResolvedValue({
      data: [{ id: 7, name: 'Факультет' }],
    })
    subjectsApi.getAll.mockResolvedValue({
      data: [
        { id: 1, name: 'Алгебра' },
        { id: 2, name: 'Физика' },
        { id: 3, name: 'Химия' },
      ],
    })
    facultiesApi.getSubjects.mockResolvedValue({
      data: [
        { id: 2, name: 'Физика' },
      ],
    })

    const state = useAdminFacultySubjectsData()
    await state.loadBaseData()
    await state.loadAssignedSubjects()

    expect(facultiesApi.getSubjects).toHaveBeenCalledWith(7)
    expect(state.assignedSubjects.value.map((subject) => subject.id)).toEqual([2])
    expect(state.availableSubjects.value.map((subject) => subject.id)).toEqual([1, 3])
    expect(state.loadingAssigned.value).toBe(false)
  })

  it('filters and sorts both columns locally without new API reads', async () => {
    facultiesApi.getAll.mockResolvedValue({
      data: [{ id: 5, name: 'Факультет' }],
    })
    subjectsApi.getAll.mockResolvedValue({
      data: [
        { id: 1, name: 'Алгебра', description: 'Базовый курс' },
        { id: 2, name: 'Физика', description: 'Лаборатории' },
        { id: 3, name: 'Анализ', description: 'Математика' },
      ],
    })
    facultiesApi.getSubjects.mockResolvedValue({
      data: [{ id: 1, name: 'Алгебра', description: 'Базовый курс' }],
    })

    const state = useAdminFacultySubjectsData()
    await state.loadBaseData()
    await state.loadAssignedSubjects()

    state.searchQuery.value = 'а'
    state.sortMode.value = 'name-desc'

    expect(state.filteredAssignedSubjects.value.map((subject) => subject.id)).toEqual([1])
    expect(state.filteredAvailableSubjects.value.map((subject) => subject.id)).toEqual([2, 3])
    expect(state.filterResultText.value).toBe('Назначено: 1 из 1. Доступно: 2 из 2.')
    expect(state.hasActiveFilters.value).toBe(true)
    expect(facultiesApi.getAll).toHaveBeenCalledTimes(1)
    expect(subjectsApi.getAll).toHaveBeenCalledTimes(1)
    expect(facultiesApi.getSubjects).toHaveBeenCalledTimes(1)

    state.resetFilters()
    expect(state.hasActiveFilters.value).toBe(false)
  })

  it('ignores stale base and assigned-subject reads when newer refreshes finish first', async () => {
    const facultiesOld = deferred()
    const subjectsOld = deferred()
    const facultiesNew = deferred()
    const subjectsNew = deferred()
    const assignedOld = deferred()
    const assignedNew = deferred()

    facultiesApi.getAll
      .mockReturnValueOnce(facultiesOld.promise)
      .mockReturnValueOnce(facultiesNew.promise)
    subjectsApi.getAll
      .mockReturnValueOnce(subjectsOld.promise)
      .mockReturnValueOnce(subjectsNew.promise)
    facultiesApi.getSubjects
      .mockReturnValueOnce(assignedOld.promise)
      .mockReturnValueOnce(assignedNew.promise)

    const state = useAdminFacultySubjectsData()
    state.facultyId.value = '2'

    const oldBase = state.loadBaseData()
    const newBase = state.loadBaseData()
    const oldAssigned = state.loadAssignedSubjects()
    const newAssigned = state.loadAssignedSubjects()

    facultiesNew.resolve({ data: [{ id: 2, name: 'Новый факультет' }] })
    subjectsNew.resolve({ data: [{ id: 20, name: 'Новый предмет' }] })
    assignedNew.resolve({ data: [{ id: 20, name: 'Новый предмет' }] })
    await Promise.all([newBase, newAssigned])

    facultiesOld.resolve({ data: [{ id: 1, name: 'Старый факультет' }] })
    subjectsOld.resolve({ data: [{ id: 10, name: 'Старый предмет' }] })
    assignedOld.resolve({ data: [{ id: 10, name: 'Старый предмет' }] })
    await Promise.all([oldBase, oldAssigned])

    expect(state.faculties.value.map((faculty) => faculty.id)).toEqual([2])
    expect(state.subjects.value.map((subject) => subject.id)).toEqual([20])
    expect(state.assignedSubjects.value.map((subject) => subject.id)).toEqual([20])
    expect(state.loading.value).toBe(false)
  })

  it('reports the current read failure through the shared notice state', async () => {
    subjectsApi.getAll.mockRejectedValue(new Error('Справочник недоступен'))

    const state = useAdminFacultySubjectsData()
    await state.loadBaseData()

    expect(state.notice.value).toEqual({
      type: 'error',
      message: 'Справочник недоступен',
    })
    expect(getApiErrorMessage).toHaveBeenCalled()
    expect(state.loadingBase.value).toBe(false)
  })
})
