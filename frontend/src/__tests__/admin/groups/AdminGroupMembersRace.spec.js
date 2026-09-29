import {
  beforeEach,
  describe,
  expect,
  it,
  vi,
} from 'vitest'

const mocks = vi.hoisted(() => ({
  getGroupMemberships: vi.fn(),
  addPersonToGroup: vi.fn(),
  updateGroupMembershipStatus: vi.fn(),
  getPeople: vi.fn(),
  getApiErrorMessage: vi.fn((error, fallback) => error?.message || fallback),
}))

vi.mock('@/api', () => ({
  getApiErrorMessage: mocks.getApiErrorMessage,
  membershipsApi: {
    getGroupMemberships: mocks.getGroupMemberships,
    addPersonToGroup: mocks.addPersonToGroup,
    updateGroupMembershipStatus: mocks.updateGroupMembershipStatus,
  },
  usersApi: {
    getPeople: mocks.getPeople,
  },
}))

import {
  useAdminGroupMembers,
} from '@/composables/admin/groups/useAdminGroupMembers'

function resolvedList(items = []) {
  return Promise.resolve({ data: items })
}

beforeEach(() => {
  vi.clearAllMocks()
  mocks.getGroupMemberships.mockImplementation(() => resolvedList())
  mocks.getPeople.mockImplementation(() => resolvedList())
  mocks.addPersonToGroup.mockResolvedValue({})
  mocks.updateGroupMembershipStatus.mockResolvedValue({})
})

describe('admin group member mutation context', () => {
  it('does not show add success for a group that is no longer open', async () => {
    let releaseAdd
    mocks.addPersonToGroup.mockImplementation(() => new Promise((resolve) => {
      releaseAdd = () => resolve({})
    }))

    const state = useAdminGroupMembers()
    state.membersGroup.value = { id: 1, code: 'A' }

    const adding = state.addStudentToGroup({
      id: 10,
      firstName: 'Иван',
      lastName: 'Иванов',
    })

    await vi.waitFor(() => {
      expect(mocks.addPersonToGroup).toHaveBeenCalledTimes(1)
    })

    state.membersGroup.value = { id: 2, code: 'B' }
    releaseAdd()
    await adding

    expect(state.memberNotice.value.message).toBe('')
  })

  it('does not close a newer remove dialog when an old group mutation finishes', async () => {
    let releaseRemove
    mocks.updateGroupMembershipStatus.mockImplementation(() => new Promise((resolve) => {
      releaseRemove = () => resolve({})
    }))

    const state = useAdminGroupMembers()
    state.membersGroup.value = { id: 1, code: 'A' }
    state.people.value = [{ id: 10, firstName: 'Иван', lastName: 'Иванов' }]

    state.requestRemoveMember({
      id: 50,
      personId: 10,
      role: 1,
      status: 1,
    })

    const removing = state.removeStudentFromGroup()

    await vi.waitFor(() => {
      expect(mocks.updateGroupMembershipStatus).toHaveBeenCalledWith(
        50,
        { status: 3 }
      )
    })

    state.membersGroup.value = { id: 2, code: 'B' }
    state.memberRemoveTarget.value = {
      membership: { id: 99, personId: 11 },
      person: { id: 11, firstName: 'Пётр' },
    }
    state.memberRemoveConfirmVisible.value = true

    releaseRemove()
    await removing

    expect(state.memberRemoveConfirmVisible.value).toBe(true)
    expect(state.memberRemoveTarget.value?.membership?.id).toBe(99)
    expect(state.memberNotice.value.message).toBe('')
  })
})
