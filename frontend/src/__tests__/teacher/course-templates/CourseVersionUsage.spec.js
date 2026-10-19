import { ref } from 'vue'

import {
  beforeEach,
  describe,
  expect,
  it,
  vi,
} from 'vitest'

const api = vi.hoisted(() => ({
  lecturesAll: vi.fn(),
  assignments: vi.fn(),
  lectureAssignments: vi.fn(),
  groupById: vi.fn(),
}))

vi.mock('@/api', () => ({
  lecturesApi: { getAll: api.lecturesAll },
  teachingApi: {
    getAssignments: api.assignments,
    getLectureAssignments: api.lectureAssignments,
  },
  groupsApi: { getById: api.groupById },
}))

import { useCourseVersionUsage } from '@/composables/course-templates/useCourseVersionUsage'

beforeEach(() => {
  vi.clearAllMocks()
  api.groupById.mockImplementation((id) => Promise.resolve({ data: { id, name: `Группа-${id}` } }))
  api.lectureAssignments.mockResolvedValue({ data: [] })
})

describe('course version usage', () => {
  it('counts version lectures and lists the groups a version is issued to', async () => {
    api.lecturesAll.mockImplementation(({ courseVersionId }) =>
      Promise.resolve({ data: courseVersionId === 1 ? [{ id: 9 }, { id: 10 }] : [] })
    )
    api.assignments.mockResolvedValue({
      data: [
        { id: 50, groupId: 3, courseVersionId: 1, status: 1, academicYear: 2026, semester: 1 },
        { id: 51, groupId: 4, courseVersionId: 2, status: 2, academicYear: 2026, semester: 2 },
        { id: 52, groupId: 5, courseVersionId: null, status: 1, academicYear: 2026, semester: 1 },
      ],
    })

    const usage = useCourseVersionUsage({
      versions: ref([{ id: 1 }, { id: 2 }]),
      selectedMembership: ref({ id: 7 }),
    })

    await usage.loadUsage()

    expect(api.assignments).toHaveBeenCalledWith({ subjectMembershipId: 7 })
    expect(usage.usageOf(1)).toEqual({
      lectureCount: 2,
      groups: ['Группа-3 (2026/2027, 1 сем.)'],
      emptyForGroups: [],
    })
    // Черновик нагрузки не считается: предупреждение только для активных групп.
    expect(usage.usageOf(2)).toEqual({
      lectureCount: 0,
      groups: ['Группа-4 (2026/2027, 2 сем., черновик)'],
      emptyForGroups: [],
    })
  })

  it('warns when an active group gets an empty version, unless lectures are assigned separately', async () => {
    api.lecturesAll.mockResolvedValue({ data: [] })
    api.assignments.mockResolvedValue({
      data: [
        { id: 60, groupId: 3, courseVersionId: 1, status: 1, academicYear: 2026, semester: 1 },
        { id: 61, groupId: 4, courseVersionId: 1, status: 1, academicYear: 2026, semester: 1 },
      ],
    })
    api.lectureAssignments.mockImplementation(({ teachingAssignmentId }) =>
      Promise.resolve({ data: teachingAssignmentId === 61 ? [{ id: 1, status: 1 }] : [] })
    )

    const usage = useCourseVersionUsage({
      versions: ref([{ id: 1 }]),
      selectedMembership: ref({ id: 7 }),
    })

    await usage.loadUsage()

    expect(usage.usageOf(1).emptyForGroups).toEqual(['Группа-3'])
    expect(usage.usageOf(1).groups[1]).toContain('назначено лекций отдельно: 1')
  })

  it('degrades to an unknown lecture count when a request fails', async () => {
    api.lecturesAll.mockRejectedValue(new Error('offline'))
    api.assignments.mockResolvedValue({ data: [] })

    const usage = useCourseVersionUsage({
      versions: ref([{ id: 1 }]),
      selectedMembership: ref({ id: 7 }),
    })

    await usage.loadUsage()

    expect(usage.usageOf(1)).toEqual({ lectureCount: null, groups: [], emptyForGroups: [] })
  })
})
