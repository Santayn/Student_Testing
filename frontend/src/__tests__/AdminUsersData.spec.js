import {
  beforeEach,
  describe,
  expect,
  it,
  vi,
} from 'vitest'

vi.mock('@/api', () => ({
  rolesApi: {
    getAll: vi.fn(),
  },
  usersApi: {
    getAll: vi.fn(),
    getPeople: vi.fn(),
  },
  getApiErrorMessage: vi.fn(
    (error, fallback) => error?.message || fallback
  ),
}))

import {
  getApiErrorMessage,
  rolesApi,
  usersApi,
} from '@/api'

import {
  useAdminUsersData,
} from '@/composables/useAdminUsersData'

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

function setup() {
  return useAdminUsersData()
}

describe('admin users data state', () => {
  beforeEach(() => {
    vi.resetAllMocks()
    rolesApi.getAll.mockResolvedValue({ data: [] })
    usersApi.getAll.mockResolvedValue({ data: [] })
    usersApi.getPeople.mockResolvedValue({ data: [] })
  })

  it('loads users, roles and people and keeps presentation helpers on the data boundary', async () => {
    rolesApi.getAll.mockResolvedValue({
      data: [
        { id: 2, name: 'TEACHER' },
        { id: 1, name: 'STUDENT' },
      ],
    })
    usersApi.getPeople.mockResolvedValue({
      data: [
        { id: 20, firstName: 'Борис', lastName: 'Яковлев', email: 'b@example.test' },
        { id: 10, firstName: 'Анна', lastName: 'Алексеева', email: 'a@example.test' },
      ],
    })
    usersApi.getAll.mockResolvedValue({
      data: [
        { id: 100, login: 'anna', active: true, personId: 10, roleIds: [1] },
        { id: 200, login: 'boris', active: false, personId: 20, roleIds: [2] },
      ],
    })

    const state = setup()
    await state.loadData()

    expect(state.roles.value.map((role) => role.id)).toEqual([1, 2])
    expect(state.people.value.map((person) => person.id)).toEqual([10, 20])
    expect(state.userFullName(state.users.value[0])).toBe('Алексеева Анна')
    expect(state.roleNamesForUser(state.users.value[1])).toEqual(['TEACHER'])
    expect(state.loading.value).toBe(false)
  })

  it('combines search, role, activity and profile filters locally', async () => {
    rolesApi.getAll.mockResolvedValue({
      data: [
        { id: 1, name: 'STUDENT' },
        { id: 2, name: 'TEACHER' },
      ],
    })
    usersApi.getPeople.mockResolvedValue({
      data: [
        { id: 10, firstName: 'Анна', lastName: 'Алексеева', email: 'anna@example.test', phone: '+1' },
        { id: 20, firstName: 'Борис', lastName: 'Белов', email: 'boris@example.test', phone: '+2' },
      ],
    })
    usersApi.getAll.mockResolvedValue({
      data: [
        { id: 1, login: 'student-a', active: true, personId: 10, roleIds: [1] },
        { id: 2, login: 'teacher-b', active: false, personId: 20, roleIds: [2] },
        { id: 3, login: 'teacher-unbound', active: true, personId: null, roleIds: [2] },
      ],
    })

    const state = setup()
    await state.loadData()

    state.roleFilter.value = 2
    state.activeFilter.value = 'active'
    state.profileFilter.value = 'unbound'
    state.searchQuery.value = 'teacher'

    expect(state.filteredUsers.value.map((user) => user.id)).toEqual([3])
    expect(state.hasActiveFilters.value).toBe(true)

    state.resetFilters()

    expect(state.filteredUsers.value.map((user) => user.id)).toEqual([1, 2, 3])
    expect(state.hasActiveFilters.value).toBe(false)
  })

  it('does not offer a Person already bound to another user but keeps the current binding selectable', async () => {
    usersApi.getPeople.mockResolvedValue({
      data: [
        { id: 10, firstName: 'Анна', lastName: 'Алексеева', email: 'a@example.test' },
        { id: 20, firstName: 'Борис', lastName: 'Белов', email: 'b@example.test' },
        { id: 30, firstName: 'Вера', lastName: 'Волкова', email: 'v@example.test' },
      ],
    })
    usersApi.getAll.mockResolvedValue({
      data: [
        { id: 1, login: 'anna', active: true, personId: 10, roleIds: [] },
        { id: 2, login: 'boris', active: true, personId: 20, roleIds: [] },
      ],
    })

    const state = setup()
    await state.loadData()

    expect(
      state.availablePersonOptionsFor(1).map((option) => option.value)
    ).toEqual([10, 30])
  })

  it('ignores a stale read when a newer refresh finishes first', async () => {
    const rolesOld = deferred()
    const usersOld = deferred()
    const peopleOld = deferred()
    const rolesNew = deferred()
    const usersNew = deferred()
    const peopleNew = deferred()

    rolesApi.getAll
      .mockReturnValueOnce(rolesOld.promise)
      .mockReturnValueOnce(rolesNew.promise)
    usersApi.getAll
      .mockReturnValueOnce(usersOld.promise)
      .mockReturnValueOnce(usersNew.promise)
    usersApi.getPeople
      .mockReturnValueOnce(peopleOld.promise)
      .mockReturnValueOnce(peopleNew.promise)

    const state = setup()
    const oldLoad = state.loadData()
    const newLoad = state.loadData()

    rolesNew.resolve({ data: [{ id: 2, name: 'TEACHER' }] })
    usersNew.resolve({ data: [{ id: 2, login: 'new-user', roleIds: [2] }] })
    peopleNew.resolve({ data: [] })
    await newLoad

    rolesOld.resolve({ data: [{ id: 1, name: 'STUDENT' }] })
    usersOld.resolve({ data: [{ id: 1, login: 'old-user', roleIds: [1] }] })
    peopleOld.resolve({ data: [] })
    await oldLoad

    expect(state.users.value.map((user) => user.login)).toEqual(['new-user'])
    expect(state.roles.value.map((role) => role.id)).toEqual([2])
    expect(state.loading.value).toBe(false)
  })

  it('reports only the current load failure through the existing notice state', async () => {
    usersApi.getAll.mockRejectedValue(new Error('Сеть недоступна'))

    const state = setup()
    await state.loadData()

    expect(state.notice.value).toEqual({
      type: 'error',
      message: 'Сеть недоступна',
    })
    expect(getApiErrorMessage).toHaveBeenCalled()
    expect(state.loading.value).toBe(false)
  })
})
