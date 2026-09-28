import {
  beforeEach,
  describe,
  expect,
  it,
  vi,
} from 'vitest'

vi.mock('@/api', () => ({
  membershipsApi: {
    getGroupMemberships: vi.fn(),
    addPersonToGroup: vi.fn(),
    updateGroupMembershipStatus: vi.fn(),
  },
  usersApi: {
    getPeople: vi.fn(),
  },
  getApiErrorMessage: vi.fn(
    (error, fallback) => error?.message || fallback
  ),
}))

import {
  membershipsApi,
  usersApi,
} from '@/api'

import {
  useAdminGroupMembers,
} from '@/composables/useAdminGroupMembers'

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

function peopleFixture() {
  return [
    {
      id: 10,
      firstName: 'Анна',
      lastName: 'Иванова',
      email: 'anna@example.com',
      phone: '+70000000001',
    },
    {
      id: 20,
      firstName: 'Борис',
      lastName: 'Петров',
      email: 'boris@example.com',
      phone: '+70000000002',
    },
    {
      id: 30,
      firstName: 'Вера',
      lastName: 'Сидорова',
      email: 'vera@example.com',
      phone: '+70000000003',
    },
  ]
}

function baseMemberships() {
  return [
    {
      id: 101,
      personId: 10,
      role: 1,
      status: 1,
      removedAtUtc: null,
      notes: null,
    },
    {
      id: 202,
      personId: 20,
      role: 1,
      status: 2,
      removedAtUtc: null,
      notes: null,
    },
  ]
}

describe('admin group members state', () => {
  beforeEach(() => {
    vi.resetAllMocks()

    membershipsApi.getGroupMemberships.mockResolvedValue({
      data: baseMemberships(),
    })
    membershipsApi.addPersonToGroup.mockResolvedValue({ data: {} })
    membershipsApi.updateGroupMembershipStatus.mockResolvedValue({ data: {} })
    usersApi.getPeople
      .mockResolvedValueOnce({ data: peopleFixture() })
      .mockResolvedValueOnce({ data: peopleFixture() })
  })

  it('opens a group, loads membership context and filters candidates locally', async () => {
    const state = useAdminGroupMembers()

    await state.openGroupMembers({
      id: 7,
      code: 'KB-01',
      name: 'КБ-01',
    })

    expect(membershipsApi.getGroupMemberships).toHaveBeenCalledWith({
      groupId: 7,
      activeOnly: false,
    })
    expect(usersApi.getPeople).toHaveBeenNthCalledWith(1)
    expect(usersApi.getPeople).toHaveBeenNthCalledWith(2, {
      role: 'STUDENT',
    })
    expect(state.membersDrawerVisible.value).toBe(true)
    expect(state.currentStudentMemberships.value.map((item) => item.personId)).toEqual([10])
    expect(state.availableStudents.value.map((person) => person.id)).toEqual([20, 30])
    expect(state.availableStudentActionLabel({ id: 20 })).toBe('Вернуть в группу')

    state.memberSearch.value = 'сидорова'

    expect(state.filteredCurrentStudentMemberships.value).toEqual([])
    expect(state.filteredAvailableStudents.value.map((person) => person.id)).toEqual([30])
  })

  it('reactivates a paused membership instead of creating a duplicate', async () => {
    const state = useAdminGroupMembers()
    await state.openGroupMembers({ id: 7, name: 'Группа' })

    usersApi.getPeople
      .mockResolvedValueOnce({ data: peopleFixture() })
      .mockResolvedValueOnce({ data: peopleFixture() })
    membershipsApi.getGroupMemberships.mockResolvedValueOnce({
      data: [
        { ...baseMemberships()[0] },
        { ...baseMemberships()[1], status: 1 },
      ],
    })

    await state.addStudentToGroup(peopleFixture()[1])

    expect(membershipsApi.updateGroupMembershipStatus).toHaveBeenCalledWith(
      202,
      { status: 1 }
    )
    expect(membershipsApi.addPersonToGroup).not.toHaveBeenCalled()
    expect(state.memberNotice.value.message).toContain('снова в составе группы')
  })

  it('creates a membership only for a student without an existing paused assignment', async () => {
    const state = useAdminGroupMembers()
    await state.openGroupMembers({ id: 7, name: 'Группа' })

    usersApi.getPeople
      .mockResolvedValueOnce({ data: peopleFixture() })
      .mockResolvedValueOnce({ data: peopleFixture() })
    membershipsApi.getGroupMemberships.mockResolvedValueOnce({
      data: baseMemberships(),
    })

    await state.addStudentToGroup(peopleFixture()[2])

    expect(membershipsApi.addPersonToGroup).toHaveBeenCalledWith(
      7,
      {
        personId: 30,
        role: 1,
        notes: null,
      }
    )
  })

  it('requires an explicit target and marks removal historically before reloading', async () => {
    const state = useAdminGroupMembers()
    await state.openGroupMembers({ id: 7, name: 'Группа' })

    const membership = baseMemberships()[0]
    state.requestRemoveMember(membership)

    expect(state.memberRemoveConfirmVisible.value).toBe(true)
    expect(state.memberRemoveTarget.value.person.id).toBe(10)

    usersApi.getPeople
      .mockResolvedValueOnce({ data: peopleFixture() })
      .mockResolvedValueOnce({ data: peopleFixture() })
    membershipsApi.getGroupMemberships.mockResolvedValueOnce({
      data: [
        { ...membership, status: 3 },
        baseMemberships()[1],
      ],
    })

    await state.removeStudentFromGroup()

    expect(membershipsApi.updateGroupMembershipStatus).toHaveBeenCalledWith(
      101,
      { status: 3 }
    )
    expect(state.memberRemoveConfirmVisible.value).toBe(false)
    expect(state.memberRemoveTarget.value).toBeNull()
    expect(state.memberNotice.value.message).toContain('убран из группы')
  })

  it('ignores a stale membership load after another group is opened', async () => {
    const oldMemberships = deferred()
    const oldPeople = deferred()
    const oldStudents = deferred()
    const newMemberships = deferred()
    const newPeople = deferred()
    const newStudents = deferred()

    membershipsApi.getGroupMemberships
      .mockReturnValueOnce(oldMemberships.promise)
      .mockReturnValueOnce(newMemberships.promise)
    usersApi.getPeople
      .mockReset()
      .mockReturnValueOnce(oldPeople.promise)
      .mockReturnValueOnce(oldStudents.promise)
      .mockReturnValueOnce(newPeople.promise)
      .mockReturnValueOnce(newStudents.promise)

    const state = useAdminGroupMembers()
    const oldOpen = state.openGroupMembers({ id: 1, name: 'Старая' })
    const newOpen = state.openGroupMembers({ id: 2, name: 'Новая' })

    newMemberships.resolve({
      data: [{ id: 2, personId: 20, role: 1, status: 1, removedAtUtc: null }],
    })
    newPeople.resolve({ data: [peopleFixture()[1]] })
    newStudents.resolve({ data: [peopleFixture()[1]] })
    await newOpen

    oldMemberships.resolve({
      data: [{ id: 1, personId: 10, role: 1, status: 1, removedAtUtc: null }],
    })
    oldPeople.resolve({ data: [peopleFixture()[0]] })
    oldStudents.resolve({ data: [peopleFixture()[0]] })
    await oldOpen

    expect(state.membersGroup.value.id).toBe(2)
    expect(state.groupMemberships.value.map((item) => item.id)).toEqual([2])
    expect(state.people.value.map((person) => person.id)).toEqual([20])
  })
})
