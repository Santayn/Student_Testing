import {
  describe,
  expect,
  it,
  vi,
} from 'vitest'

import {
  emptyStudentLearningContext,
  loadStudentLearningContext,
} from '@/utils/studentLearningContext'

function response(data) {
  return Promise.resolve({ data })
}

function apiFixture() {
  const membershipsApi = {
    getGroupMemberships: vi.fn(() =>
      response([
        {
          id: 1,
          groupId: 10,
          role: 1,
          status: 1,
          removedAtUtc: null,
        },
        {
          id: 2,
          groupId: 20,
          role: 1,
          status: 2,
          removedAtUtc: null,
        },
        {
          id: 3,
          groupId: 30,
          role: 1,
          status: 1,
          removedAtUtc: null,
        },
        {
          id: 4,
          groupId: 40,
          role: 2,
          status: 1,
          removedAtUtc: null,
        },
        {
          id: 5,
          groupId: 50,
          role: 1,
          status: 1,
          removedAtUtc: '2026-09-01T00:00:00Z',
        },
      ])
    ),
    getSubjectMembership: vi.fn((id) =>
      response({
        id,
        subjectId: {
          500: 50,
          600: 60,
          700: 70,
          800: 80,
        }[id],
      })
    ),
  }

  const groupsApi = {
    getById: vi.fn((id) =>
      response({
        id,
        facultyId: id === 10 ? 100 : 200,
        name: `Группа ${id}`,
      })
    ),
  }

  const facultiesApi = {
    getById: vi.fn((id) =>
      response({
        id,
        name: `Факультет ${id}`,
      })
    ),
  }

  const teachingApi = {
    getAssignments: vi.fn(({ groupId }) => {
      if (groupId === 10) {
        return response([
          {
            id: 1000,
            groupId: 10,
            subjectMembershipId: 500,
            status: 1,
          },
          {
            id: 1001,
            groupId: 10,
            subjectMembershipId: 501,
            status: 2,
          },
        ])
      }

      return response([
        {
          id: 3000,
          groupId: 30,
          subjectMembershipId: 600,
          status: 1,
        },
      ])
    }),
    getEnrollments: vi.fn(({ groupMembershipId }) => {
      if (groupMembershipId === 1) {
        return response([
          {
            id: 101,
            teachingAssignmentId: 2000,
            groupMembershipId: 1,
            status: 2,
            removedAtUtc: null,
          },
          {
            id: 102,
            teachingAssignmentId: 2001,
            groupMembershipId: 1,
            status: 3,
            removedAtUtc: '2026-09-02T00:00:00Z',
          },
        ])
      }

      return response([
        {
          id: 301,
          teachingAssignmentId: 3001,
          groupMembershipId: 3,
          status: 1,
          removedAtUtc: null,
        },
        {
          id: 302,
          teachingAssignmentId: 3002,
          groupMembershipId: 3,
          status: 2,
          removedAtUtc: '2026-09-03T00:00:00Z',
        },
      ])
    }),
    getAssignment: vi.fn((id) => {
      if (id === 2000) {
        return response({
          id,
          groupId: 10,
          subjectMembershipId: 700,
          status: 1,
        })
      }

      if (id === 3001) {
        return response({
          id,
          groupId: 30,
          subjectMembershipId: 800,
          status: 2,
        })
      }

      return response(null)
    }),
  }

  const subjectsApi = {
    getById: vi.fn((id) =>
      response({
        id,
        name: `Предмет ${id}`,
      })
    ),
  }

  return {
    membershipsApi,
    groupsApi,
    facultiesApi,
    teachingApi,
    subjectsApi,
  }
}

describe('student learning context', () => {
  it('matches active public-learning membership, assignment and enrollment rules', async () => {
    const apis = apiFixture()

    const context = await loadStudentLearningContext({
      personId: 77,
      ...apis,
    })

    expect(
      context.memberships.map((item) => item.id)
    ).toEqual([1, 3])

    expect(
      context.groups.map((item) => item.id)
    ).toEqual([10, 30])

    expect(
      context.faculties.map((item) => item.id)
    ).toEqual([100, 200])

    expect(
      context.enrollments.map((item) => item.id)
    ).toEqual([101, 301])

    expect(
      context.assignments.map((item) => item.id)
    ).toEqual([1000, 3000, 2000])

    expect(
      context.subjects.map((item) => item.id)
    ).toEqual([50, 60, 70])

    expect(
      apis.membershipsApi.getGroupMemberships
    ).toHaveBeenCalledWith({
      personId: 77,
      status: 1,
      activeOnly: true,
    })

    expect(
      apis.teachingApi.getAssignments
    ).toHaveBeenCalledWith({
      groupId: 10,
      status: 1,
    })
    expect(
      apis.teachingApi.getAssignments
    ).toHaveBeenCalledWith({
      groupId: 30,
      status: 1,
    })

    expect(
      apis.membershipsApi.getSubjectMembership
    ).not.toHaveBeenCalledWith(800)
    expect(
      apis.subjectsApi.getById
    ).not.toHaveBeenCalledWith(80)
  })

  it('returns an empty context for an invalid person id without API calls', async () => {
    const apis = apiFixture()

    const context = await loadStudentLearningContext({
      personId: null,
      ...apis,
    })

    expect(context).toEqual(
      emptyStudentLearningContext()
    )
    expect(
      apis.membershipsApi.getGroupMemberships
    ).not.toHaveBeenCalled()
  })
})
