import {
  beforeEach,
  describe,
  expect,
  it,
  vi,
} from 'vitest'

vi.mock('@/api', () => ({
  usersApi: {
    getPeople: vi.fn(),
  },
  subjectsApi: {
    getAll: vi.fn(),
  },
  membershipsApi: {
    getSubjectMemberships: vi.fn(),
  },
  getApiErrorMessage: vi.fn(
    (error, fallback) => error?.message || fallback
  ),
}))

import {
  getApiErrorMessage,
  membershipsApi,
  subjectsApi,
  usersApi,
} from '@/api'

import {
  useAdminTeacherSubjectsData,
} from '@/composables/admin/teacher-subjects/useAdminTeacherSubjectsData'

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

describe('admin teacher subjects data state', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    getApiErrorMessage.mockImplementation(
      (error, fallback) => error?.message || fallback
    )
    usersApi.getPeople.mockResolvedValue({ data: [] })
    subjectsApi.getAll.mockResolvedValue({ data: [] })
    membershipsApi.getSubjectMemberships.mockResolvedValue({ data: [] })
  })

  it('loads teacher/admin people, de-duplicates them and selects the first teacher', async () => {
    usersApi.getPeople
      .mockResolvedValueOnce({
        data: [
          {
            id: 2,
            firstName: 'Борис',
            lastName: 'Яковлев',
            email: 'boris@example.test',
          },
          {
            id: 1,
            firstName: 'Анна',
            lastName: 'Иванова',
          },
        ],
      })
      .mockResolvedValueOnce({
        data: [
          {
            id: 2,
            firstName: 'Борис',
            lastName: 'Яковлев',
            email: 'admin-copy@example.test',
          },
        ],
      })

    subjectsApi.getAll.mockResolvedValue({
      data: [
        { id: 20, name: 'Физика' },
        { id: 10, name: 'Алгоритмы' },
      ],
    })

    const state = useAdminTeacherSubjectsData()
    await state.loadBaseData()

    expect(usersApi.getPeople).toHaveBeenNthCalledWith(1, {
      role: 'TEACHER',
    })
    expect(usersApi.getPeople).toHaveBeenNthCalledWith(2, {
      role: 'ADMIN',
    })
    expect(state.teachers.value.map((person) => person.id)).toEqual([1, 2])
    expect(state.subjects.value.map((subject) => subject.id)).toEqual([10, 20])
    expect(state.teacherId.value).toBe('1')
    expect(state.teacherOptions.value[1].label).toContain('admin-copy@example.test')
    expect(state.loadingBase.value).toBe(false)
  })

  it('derives assigned, paused and available subjects for the selected teacher', async () => {
    usersApi.getPeople
      .mockResolvedValueOnce({
        data: [{ id: 7, firstName: 'Ирина', lastName: 'Соколова' }],
      })
      .mockResolvedValueOnce({ data: [] })

    subjectsApi.getAll.mockResolvedValue({
      data: [
        { id: 1, name: 'Алгебра' },
        { id: 2, name: 'Физика' },
        { id: 3, name: 'Химия' },
      ],
    })

    membershipsApi.getSubjectMemberships.mockResolvedValue({
      data: [
        { id: 101, personId: 7, subjectId: 1, role: 1, status: 1, notes: 'Активно' },
        { id: 102, personId: 7, subjectId: 2, role: 1, status: 2, notes: 'Вернуть позже' },
        { id: 103, personId: 7, subjectId: 3, role: 2, status: 1 },
        { id: 104, personId: 8, subjectId: 3, role: 1, status: 1 },
      ],
    })

    const state = useAdminTeacherSubjectsData()
    await Promise.all([
      state.loadBaseData(),
      state.loadMemberships(),
    ])

    expect(membershipsApi.getSubjectMemberships).toHaveBeenCalledWith({
      activeOnly: true,
    })
    expect(state.assignedSubjects.value.map((subject) => subject.id)).toEqual([1])
    expect(state.pausedTeacherMemberships.value.map((item) => item.id)).toEqual([102])
    expect(state.availableSubjects.value.map((subject) => subject.id)).toEqual([2, 3])
    expect(state.availableSubjects.value[0].pausedMembership?.id).toBe(102)
  })

  it('filters and sorts both subject columns locally without new API reads', async () => {
    usersApi.getPeople
      .mockResolvedValueOnce({ data: [{ id: 5, fullName: 'Преподаватель' }] })
      .mockResolvedValueOnce({ data: [] })
    subjectsApi.getAll.mockResolvedValue({
      data: [
        { id: 1, name: 'Алгебра', description: 'Базовый курс' },
        { id: 2, name: 'Физика', description: 'Лаборатории' },
        { id: 3, name: 'Анализ', description: 'Математика' },
      ],
    })
    membershipsApi.getSubjectMemberships.mockResolvedValue({
      data: [
        { id: 1, personId: 5, subjectId: 1, role: 1, status: 1, notes: 'Первый курс' },
      ],
    })

    const state = useAdminTeacherSubjectsData()
    await Promise.all([
      state.loadBaseData(),
      state.loadMemberships(),
    ])

    state.searchQuery.value = 'а'
    state.sortMode.value = 'name-desc'

    expect(state.filteredAssignedSubjects.value.map((subject) => subject.id)).toEqual([1])
    expect(state.filteredAvailableSubjects.value.map((subject) => subject.id)).toEqual([2, 3])
    expect(state.hasActiveFilters.value).toBe(true)
    expect(state.filterResultText.value).toBe('Назначено: 1 из 1. Доступно: 2 из 2.')
    expect(usersApi.getPeople).toHaveBeenCalledTimes(2)
    expect(subjectsApi.getAll).toHaveBeenCalledTimes(1)
    expect(membershipsApi.getSubjectMemberships).toHaveBeenCalledTimes(1)

    state.resetFilters()
    expect(state.hasActiveFilters.value).toBe(false)
  })

  it('ignores stale base and membership reads when a newer refresh finishes first', async () => {
    const teachersOld = deferred()
    const adminsOld = deferred()
    const subjectsOld = deferred()
    const teachersNew = deferred()
    const adminsNew = deferred()
    const subjectsNew = deferred()
    const membershipsOld = deferred()
    const membershipsNew = deferred()

    usersApi.getPeople
      .mockReturnValueOnce(teachersOld.promise)
      .mockReturnValueOnce(adminsOld.promise)
      .mockReturnValueOnce(teachersNew.promise)
      .mockReturnValueOnce(adminsNew.promise)
    subjectsApi.getAll
      .mockReturnValueOnce(subjectsOld.promise)
      .mockReturnValueOnce(subjectsNew.promise)
    membershipsApi.getSubjectMemberships
      .mockReturnValueOnce(membershipsOld.promise)
      .mockReturnValueOnce(membershipsNew.promise)

    const state = useAdminTeacherSubjectsData()
    const oldBase = state.loadBaseData()
    const oldMemberships = state.loadMemberships()
    const newBase = state.loadBaseData()
    const newMemberships = state.loadMemberships()

    teachersNew.resolve({ data: [{ id: 2, fullName: 'Новый' }] })
    adminsNew.resolve({ data: [] })
    subjectsNew.resolve({ data: [{ id: 2, name: 'Новый предмет' }] })
    membershipsNew.resolve({
      data: [{ id: 2, personId: 2, subjectId: 2, role: 1, status: 1 }],
    })
    await Promise.all([newBase, newMemberships])

    teachersOld.resolve({ data: [{ id: 1, fullName: 'Старый' }] })
    adminsOld.resolve({ data: [] })
    subjectsOld.resolve({ data: [{ id: 1, name: 'Старый предмет' }] })
    membershipsOld.resolve({
      data: [{ id: 1, personId: 1, subjectId: 1, role: 1, status: 1 }],
    })
    await Promise.all([oldBase, oldMemberships])

    expect(state.teachers.value.map((person) => person.id)).toEqual([2])
    expect(state.subjects.value.map((subject) => subject.id)).toEqual([2])
    expect(state.memberships.value.map((membership) => membership.id)).toEqual([2])
    expect(state.loading.value).toBe(false)
  })

  it('reports the current read failure through the shared notice state', async () => {
    subjectsApi.getAll.mockRejectedValue(new Error('Справочник недоступен'))

    const state = useAdminTeacherSubjectsData()
    await state.loadBaseData()

    expect(state.notice.value).toEqual({
      type: 'error',
      message: 'Справочник недоступен',
    })
    expect(getApiErrorMessage).toHaveBeenCalled()
    expect(state.loadingBase.value).toBe(false)
  })
})
