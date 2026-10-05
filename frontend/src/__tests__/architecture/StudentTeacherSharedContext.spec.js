import {
  beforeEach,
  describe,
  expect,
  it,
  vi,
} from 'vitest'

import {
  createLearningContextCache,
  invalidateAllLearningContextCaches,
} from '@/utils/learningContextCache'

import {
  loadStudentLearningContext,
} from '@/utils/studentLearningContext'

import {
  loadTeacherSubjectContext,
} from '@/utils/teacherSubjectContext'

function result(data) {
  return Promise.resolve({ data })
}

beforeEach(() => invalidateAllLearningContextCaches())

describe('shared Student/Teacher data loading', () => {
  it('shares a complete student graph between Profile and Subjects', async () => {
    const cache = createLearningContextCache()
    const apis = {
      membershipsApi: {
        getGroupMemberships: vi.fn(() => result([
          { id: 11, groupId: 4, role: 1, status: 1 },
        ])),
        getSubjectMembership: vi.fn(() => result({ id: 31, subjectId: 41 })),
      },
      groupsApi: {
        getById: vi.fn(() => result({ id: 4, facultyId: 5, name: 'Group' })),
      },
      facultiesApi: {
        getById: vi.fn(() => result({ id: 5, name: 'Faculty' })),
      },
      teachingApi: {
        getAssignments: vi.fn(() => result([
          { id: 21, groupId: 4, subjectMembershipId: 31, status: 1 },
        ])),
        getEnrollments: vi.fn(() => result([])),
      },
      subjectsApi: {
        getById: vi.fn(() => result({ id: 41, name: 'Algorithms' })),
      },
    }

    const args = { personId: 77, ...apis, cache }
    const [profile, subjects] = await Promise.all([
      loadStudentLearningContext(args),
      loadStudentLearningContext(args),
    ])

    expect(profile.subjects).toEqual(subjects.subjects)
    expect(profile.subjects.map((subject) => subject.id)).toEqual([41])
    expect(apis.membershipsApi.getGroupMemberships).toHaveBeenCalledTimes(1)
    expect(apis.groupsApi.getById).toHaveBeenCalledTimes(1)
    expect(apis.facultiesApi.getById).toHaveBeenCalledTimes(1)
    expect(apis.teachingApi.getAssignments).toHaveBeenCalledTimes(1)
    expect(apis.subjectsApi.getById).toHaveBeenCalledTimes(1)

    await loadStudentLearningContext(args)
    expect(apis.membershipsApi.getGroupMemberships).toHaveBeenCalledTimes(1)
  })

  it('shares teacher memberships and one subject catalog across screens', async () => {
    const cache = createLearningContextCache()
    const authStore = { personId: 77, isAdminMode: false }
    const membershipsApi = {
      getSubjectMemberships: vi.fn(() => result([
        { id: 3, personId: 77, subjectId: 41, role: 1, status: 1 },
        { id: 4, personId: 77, subjectId: 42, role: 1, status: 2 },
      ])),
    }
    const subjectsApi = {
      getAll: vi.fn(() => result([
        { id: 41, name: 'Algorithms' },
        { id: 42, name: 'Databases' },
        { id: 99, name: 'Not assigned' },
      ])),
    }
    const args = { authStore, membershipsApi, subjectsApi, cache }

    const [first, second] = await Promise.all([
      loadTeacherSubjectContext(args),
      loadTeacherSubjectContext(args),
    ])

    expect(first).toEqual(second)
    expect(first.memberships.map((item) => item.id)).toEqual([3, 4])
    expect(first.subjects.map((item) => item.id)).toEqual([41, 42])
    expect(membershipsApi.getSubjectMemberships).toHaveBeenCalledTimes(1)
    expect(subjectsApi.getAll).toHaveBeenCalledTimes(1)

    await loadTeacherSubjectContext(args)
    expect(membershipsApi.getSubjectMemberships).toHaveBeenCalledTimes(1)
  })
})
