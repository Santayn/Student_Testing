import {
  beforeEach,
  describe,
  expect,
  it,
  vi,
} from 'vitest'

import {
  findReactivatableTeacherMembership,
  isAssignableTeacherMembership,
  isReactivatableTeacherMembership,
} from '@/utils/teacherMembershipEligibility'
import {
  assignSubjectsToTeacher,
} from '@/utils/teacherSubjectAssignment'

describe('teacher subject membership state', () => {
  const api = {
    addPersonToSubject: vi.fn(),
    updateSubjectMembership: vi.fn(),
  }

  beforeEach(() => {
    vi.clearAllMocks()
    api.addPersonToSubject.mockResolvedValue({
      data: { id: 900 },
    })
    api.updateSubjectMembership.mockResolvedValue({
      data: {},
    })
  })

  it('treats status 2 as paused, not active', () => {
    const membership = {
      id: 44,
      personId: 7,
      subjectId: 5,
      role: 1,
      status: 2,
      removedAtUtc: null,
    }

    expect(isAssignableTeacherMembership(membership)).toBe(false)
    expect(isReactivatableTeacherMembership(membership)).toBe(true)
    expect(
      findReactivatableTeacherMembership([membership], 7, 5)
    ).toEqual(membership)
  })

  it('reactivates a paused membership instead of creating a duplicate', async () => {
    const memberships = [
      {
        id: 44,
        personId: 7,
        subjectId: 5,
        role: 1,
        status: 2,
        removedAtUtc: null,
        notes: 'Старое примечание',
      },
    ]

    const result = await assignSubjectsToTeacher({
      api,
      memberships,
      personId: 7,
      subjectIds: [5],
      notes: '',
    })

    expect(api.updateSubjectMembership).toHaveBeenCalledWith(
      44,
      {
        status: 1,
        notes: 'Старое примечание',
      }
    )
    expect(api.addPersonToSubject).not.toHaveBeenCalled()
    expect(result).toEqual([
      {
        subjectId: 5,
        membershipId: 44,
        action: 'reactivated',
      },
    ])
  })

  it('creates a new membership when no paused non-removed membership exists', async () => {
    await assignSubjectsToTeacher({
      api,
      memberships: [],
      personId: 7,
      subjectIds: [5],
      notes: 'Новая нагрузка',
    })

    expect(api.addPersonToSubject).toHaveBeenCalledWith(
      5,
      {
        personId: 7,
        role: 1,
        notes: 'Новая нагрузка',
      }
    )
    expect(api.updateSubjectMembership).not.toHaveBeenCalled()
  })
})
