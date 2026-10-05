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
  groupsApi: {
    getAll: vi.fn(),
  },
  membershipsApi: {
    getSubjectMemberships: vi.fn(),
  },
  teachingApi: {
    getLoadTypes: vi.fn(),
    getAssignments: vi.fn(),
  },
  usersApi: {
    getPeople: vi.fn(),
  },
  getApiErrorMessage: vi.fn(
    (error, fallback) => error?.message || fallback
  ),
}))

import {
  facultiesApi,
  groupsApi,
  membershipsApi,
  teachingApi,
  usersApi,
} from '@/api'

import {
  useAdminTeachingAssignmentsData,
} from '@/composables/admin/teaching-assignments/useAdminTeachingAssignmentsData'

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

function setupDefaults() {
  facultiesApi.getAll.mockResolvedValue({ data: [] })
  facultiesApi.getSubjects.mockResolvedValue({ data: [] })
  groupsApi.getAll.mockResolvedValue({ data: [] })
  membershipsApi.getSubjectMemberships.mockResolvedValue({ data: [] })
  teachingApi.getLoadTypes.mockResolvedValue({ data: [] })
  teachingApi.getAssignments.mockResolvedValue({ data: [] })
  usersApi.getPeople.mockResolvedValue({ data: [] })
}

describe('admin teaching assignments data state', () => {
  beforeEach(() => {
    vi.resetAllMocks()
    setupDefaults()
  })

  it('loads base dictionaries and chooses the first faculty when the current one is unavailable', async () => {
    facultiesApi.getAll.mockResolvedValue({
      data: [
        { id: 2, name: 'Экономика' },
        { id: 1, name: 'Инженерный' },
      ],
    })
    usersApi.getPeople.mockResolvedValue({
      data: [
        { id: 10, firstName: 'Анна', lastName: 'Иванова', email: 'a@example.test' },
      ],
    })
    membershipsApi.getSubjectMemberships.mockResolvedValue({
      data: [
        { id: 100, role: 1, subjectId: 50, personId: 10, status: 1 },
        { id: 200, role: 2, subjectId: 50, personId: 10, status: 1 },
      ],
    })
    teachingApi.getLoadTypes.mockResolvedValue({
      data: [
        { id: 2, name: 'Практика' },
        { id: 1, name: 'Лекция' },
      ],
    })

    const state = useAdminTeachingAssignmentsData()
    const loaded = await state.loadBaseData()

    expect(loaded).toBe(true)
    expect(state.faculties.value.map((item) => item.id)).toEqual([1, 2])
    expect(state.context.facultyId).toBe('1')
    expect(state.teacherMemberships.value.map((item) => item.id)).toEqual([100])
    expect(state.loadTypes.value.map((item) => item.id)).toEqual([1, 2])
    expect(state.personLabel(10)).toBe('Иванова Анна')
    expect(state.loadingBase.value).toBe(false)
  })

  it('loads faculty subjects/groups and then assignments for the selected academic context', async () => {
    const state = useAdminTeachingAssignmentsData()
    state.context.facultyId = '7'
    state.context.studyCourse = '3'
    state.context.semester = '2'
    state.context.academicYear = '2026'

    facultiesApi.getSubjects.mockResolvedValue({
      data: [
        { id: 20, name: 'Базы данных' },
        { id: 10, name: 'Алгоритмы' },
      ],
    })
    groupsApi.getAll.mockResolvedValue({
      data: [
        { id: 2, name: 'Группа Б', code: 'B' },
        { id: 1, name: 'Группа А', code: 'A' },
      ],
    })
    teachingApi.getAssignments.mockResolvedValue({
      data: [
        { id: 500, groupId: 1, subjectMembershipId: 100, status: 1 },
      ],
    })

    await state.loadFacultyContext()

    expect(facultiesApi.getSubjects).toHaveBeenCalledWith(7)
    expect(groupsApi.getAll).toHaveBeenCalledWith({ facultyId: 7 })
    expect(teachingApi.getAssignments).toHaveBeenCalledWith({
      facultyId: 7,
      studyCourse: 3,
      semester: 2,
      academicYear: 2026,
    })
    expect(state.facultySubjects.value.map((item) => item.id)).toEqual([10, 20])
    expect(state.groups.value.map((item) => item.id)).toEqual([1, 2])
    expect(state.assignments.value.map((item) => item.id)).toEqual([500])
  })

  it('filters and summarizes assignments locally without new assignment reads', async () => {
    const state = useAdminTeachingAssignmentsData()

    state.people.value = [
      { id: 1, firstName: 'Анна', lastName: 'Иванова' },
      { id: 2, firstName: 'Борис', lastName: 'Петров' },
    ]
    state.facultySubjects.value = [
      { id: 10, name: 'Java' },
      { id: 20, name: 'SQL' },
    ]
    state.groups.value = [
      { id: 100, name: 'Разработка', code: 'DEV' },
      { id: 200, name: 'Аналитика', code: 'AN' },
    ]
    state.loadTypes.value = [
      { id: 1000, name: 'Лекция' },
      { id: 2000, name: 'Практика' },
    ]
    state.teacherMemberships.value = [
      { id: 10000, subjectId: 10, personId: 1, role: 1 },
      { id: 20000, subjectId: 20, personId: 2, role: 1 },
    ]
    state.assignments.value = [
      {
        id: 1,
        subjectMembershipId: 10000,
        groupId: 100,
        loadTypeId: 1000,
        status: 1,
        hoursPerWeek: 4,
        notes: 'Основная нагрузка',
      },
      {
        id: 2,
        subjectMembershipId: 20000,
        groupId: 200,
        loadTypeId: 2000,
        status: 2,
        hoursPerWeek: 2,
        notes: 'Черновик',
      },
    ]

    state.teacherFilter.value = '1'
    state.searchQuery.value = 'java'

    expect(state.filteredAssignments.value.map((item) => item.id)).toEqual([1])
    expect(state.summary.value).toEqual({
      assignments: 2,
      active: 1,
      hours: 4,
      teachers: 1,
    })
    expect(teachingApi.getAssignments).not.toHaveBeenCalled()

    state.resetFilters()
    expect(state.filteredAssignments.value).toHaveLength(2)
  })

  it('ignores an older base refresh that completes after a newer one', async () => {
    const facultiesOld = deferred()
    const peopleOld = deferred()
    const membershipsOld = deferred()
    const loadTypesOld = deferred()
    const facultiesNew = deferred()
    const peopleNew = deferred()
    const membershipsNew = deferred()
    const loadTypesNew = deferred()

    facultiesApi.getAll
      .mockReturnValueOnce(facultiesOld.promise)
      .mockReturnValueOnce(facultiesNew.promise)
    usersApi.getPeople
      .mockReturnValueOnce(peopleOld.promise)
      .mockReturnValueOnce(peopleNew.promise)
    membershipsApi.getSubjectMemberships
      .mockReturnValueOnce(membershipsOld.promise)
      .mockReturnValueOnce(membershipsNew.promise)
    teachingApi.getLoadTypes
      .mockReturnValueOnce(loadTypesOld.promise)
      .mockReturnValueOnce(loadTypesNew.promise)

    const state = useAdminTeachingAssignmentsData()
    const oldLoad = state.loadBaseData()
    const newLoad = state.loadBaseData()

    facultiesNew.resolve({ data: [{ id: 2, name: 'Новый' }] })
    peopleNew.resolve({ data: [{ id: 2, firstName: 'Новый' }] })
    membershipsNew.resolve({ data: [{ id: 2, role: 1 }] })
    loadTypesNew.resolve({ data: [{ id: 2, name: 'Новый тип' }] })
    await newLoad

    facultiesOld.resolve({ data: [{ id: 1, name: 'Старый' }] })
    peopleOld.resolve({ data: [{ id: 1, firstName: 'Старый' }] })
    membershipsOld.resolve({ data: [{ id: 1, role: 1 }] })
    loadTypesOld.resolve({ data: [{ id: 1, name: 'Старый тип' }] })
    await oldLoad

    expect(state.faculties.value.map((item) => item.id)).toEqual([2])
    expect(state.people.value.map((item) => item.id)).toEqual([2])
    expect(state.loadTypes.value.map((item) => item.id)).toEqual([2])
    expect(state.loadingBase.value).toBe(false)
  })

  it('ignores stale assignments when a newer period refresh finishes first', async () => {
    const oldAssignments = deferred()
    const newAssignments = deferred()

    teachingApi.getAssignments
      .mockReturnValueOnce(oldAssignments.promise)
      .mockReturnValueOnce(newAssignments.promise)

    const state = useAdminTeachingAssignmentsData()
    state.context.facultyId = '1'
    state.context.academicYear = '2026'

    const oldLoad = state.refreshAssignments()
    state.context.semester = '2'
    const newLoad = state.refreshAssignments()

    newAssignments.resolve({ data: [{ id: 2 }] })
    await newLoad
    oldAssignments.resolve({ data: [{ id: 1 }] })
    await oldLoad

    expect(state.assignments.value.map((item) => item.id)).toEqual([2])
    expect(state.loadingAssignments.value).toBe(false)
  })
})
