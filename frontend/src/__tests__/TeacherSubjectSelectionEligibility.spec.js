import {
  beforeEach,
  describe,
  expect,
  it,
  vi,
} from 'vitest'

const state = vi.hoisted(() => ({
  auth: {
    isAdmin: true,
    personId: 999,
  },
}))

vi.mock('@/stores/auth', () => ({
  useAuthStore: () => state.auth,
}))

vi.mock('@/api', () => ({
  membershipsApi: {
    getSubjectMemberships: vi.fn(),
  },
  subjectsApi: {
    getById: vi.fn(),
  },
}))

import {
  membershipsApi,
  subjectsApi,
} from '@/api'
import {
  useTeacherSubjects,
} from '@/composables/useTeacherSubjects'

function membership({
  id,
  subjectId,
  personId,
  role = 1,
  status = 1,
  removedAtUtc = null,
}) {
  return {
    id,
    subjectId,
    personId,
    role,
    status,
    removedAtUtc,
  }
}

describe('teacher subject selection eligibility', () => {
  beforeEach(() => {
    vi.clearAllMocks()

    state.auth.isAdmin = true
    state.auth.personId = 999

    subjectsApi.getById.mockImplementation(
      async (subjectId) => ({
        data: {
          id: Number(subjectId),
          name: `Предмет ${subjectId}`,
        },
      })
    )
  })

  it('keeps inactive memberships for history but excludes them from interactive options', async () => {
    membershipsApi.getSubjectMemberships.mockResolvedValue({
      data: [
        membership({ id: 1, subjectId: 7, personId: 42 }),
        membership({ id: 2, subjectId: 7, personId: 43, status: 2 }),
        membership({ id: 3, subjectId: 8, personId: 44 }),
        membership({ id: 4, subjectId: 9, personId: 45, role: 2 }),
      ],
    })

    const context = useTeacherSubjects()

    await context.loadTeacherSubjects()

    expect(
      context.subjectMemberships.value.map((item) => item.id)
    ).toEqual([1, 2, 3])

    expect(
      context.activeSubjectMemberships.value.map((item) => item.id)
    ).toEqual([1, 3])

    expect(
      context.membershipOptions.value.map((item) => item.value)
    ).toEqual([1, 3])

    expect(context.selectedMembershipId.value).toBe('')
  })

  it('auto-selects only when exactly one active teacher membership remains', async () => {
    membershipsApi.getSubjectMemberships.mockResolvedValue({
      data: [
        membership({ id: 10, subjectId: 7, personId: 42 }),
        membership({ id: 11, subjectId: 8, personId: 42, status: 2 }),
      ],
    })

    const context = useTeacherSubjects()

    await context.loadTeacherSubjects()

    expect(context.selectedMembershipId.value).toBe('10')
    expect(context.selectedMembership.value?.id).toBe(10)
  })

  it('does not resolve a selected subject through an inactive membership', async () => {
    membershipsApi.getSubjectMemberships.mockResolvedValue({
      data: [
        membership({ id: 20, subjectId: 7, personId: 42 }),
        membership({ id: 21, subjectId: 9, personId: 42, status: 2 }),
      ],
    })

    const context = useTeacherSubjects()

    await context.loadTeacherSubjects()

    context.selectedMembershipId.value = ''
    context.selectedSubjectId.value = '9'

    expect(context.selectedMembershipId.value).toBe('')
    expect(context.selectedMembership.value).toBeNull()
  })
})
