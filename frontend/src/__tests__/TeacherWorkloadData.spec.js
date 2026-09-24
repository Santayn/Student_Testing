import { nextTick, ref } from 'vue'
import { beforeEach, describe, expect, it, vi } from 'vitest'

vi.mock('@/api', () => ({
  getApiErrorMessage: (_error, fallback) => fallback,
  groupsApi: { getById: vi.fn() },
  teachingApi: {
    getAssignments: vi.fn(),
    getLoadTypes: vi.fn(),
  },
}))

import { groupsApi, teachingApi } from '@/api'
import { useTeacherWorkloadData } from '@/composables/useTeacherWorkloadData'

function deferred() {
  let resolve
  let reject
  const promise = new Promise((done, fail) => {
    resolve = done
    reject = fail
  })
  return { promise, resolve, reject }
}

function setup(memberships = [
  { id: 11, subjectId: 7 },
  { id: 12, subjectId: 7 },
]) {
  const subjectMemberships = ref(memberships)
  const loadTeacherSubjects = vi.fn().mockResolvedValue(undefined)
  const workload = useTeacherWorkloadData({
    subjectMemberships,
    loadTeacherSubjects,
  })
  return { ...workload, subjectMemberships, loadTeacherSubjects }
}

beforeEach(() => {
  vi.resetAllMocks()
  teachingApi.getLoadTypes.mockResolvedValue({ data: [{ id: 3, name: 'Лекции' }] })
  teachingApi.getAssignments.mockImplementation(async ({ subjectMembershipId }) => ({
    data: [{
      id: subjectMembershipId,
      subjectMembershipId,
      groupId: 20,
      loadTypeId: 3,
      hoursPerWeek: 2,
    }],
  }))
  groupsApi.getById.mockResolvedValue({ data: { id: 20, name: 'КБ-23', code: 'КБ-23' } })
})

describe('useTeacherWorkloadData', () => {
  it('loads both memberships for one subject and deduplicates group lookups', async () => {
    const workload = setup()
    try {
      await workload.initialize()

      expect(workload.loadTeacherSubjects).toHaveBeenCalledTimes(1)
      expect(teachingApi.getLoadTypes).toHaveBeenCalledTimes(1)
      expect(teachingApi.getAssignments).toHaveBeenCalledTimes(2)
      expect(teachingApi.getAssignments).toHaveBeenCalledWith({
        subjectMembershipId: 11,
        studyCourse: 1,
        semester: 1,
        academicYear: workload.academicYear.value,
      })
      expect(teachingApi.getAssignments).toHaveBeenCalledWith({
        subjectMembershipId: 12,
        studyCourse: 1,
        semester: 1,
        academicYear: workload.academicYear.value,
      })
      expect(groupsApi.getById).toHaveBeenCalledTimes(1)
      expect(groupsApi.getById).toHaveBeenCalledWith(20)
      expect(workload.assignments.value.map((item) => item.subjectMembershipId)).toEqual([11, 12])
      expect(workload.assignments.value[0].groupName).toBe('КБ-23')
      expect(workload.loading.value).toBe(false)
    } finally {
      workload.dispose()
    }
  })

  it('does not request assignments when the teacher has no memberships', async () => {
    const workload = setup([])
    try {
      await workload.initialize()
      expect(teachingApi.getAssignments).not.toHaveBeenCalled()
      expect(groupsApi.getById).not.toHaveBeenCalled()
      expect(workload.assignments.value).toEqual([])
      expect(workload.loading.value).toBe(false)
    } finally {
      workload.dispose()
    }
  })

  it('does not overwrite the newer period with a late response from an older read', async () => {
    const workload = setup([{ id: 11, subjectId: 7 }])
    try {
      await workload.initialize()
      const oldRead = deferred()
      teachingApi.getAssignments
        .mockImplementationOnce(() => oldRead.promise)
        .mockResolvedValueOnce({ data: [{ id: 30, subjectMembershipId: 11, groupId: 20 }] })
      groupsApi.getById.mockClear()

      const pendingOld = workload.refreshAssignments()
      workload.semester.value = 2
      await nextTick() // period watcher starts the newer read
      await vi.waitFor(() => {
        expect(workload.assignments.value.map((item) => item.id)).toEqual([30])
      })

      oldRead.resolve({ data: [{ id: 10, subjectMembershipId: 11, groupId: 20 }] })
      await pendingOld
      expect(workload.assignments.value.map((item) => item.id)).toEqual([30])
      expect(groupsApi.getById).toHaveBeenCalledTimes(1)
    } finally {
      workload.dispose()
    }
  })

  it('keeps the last successful data visible when the next read fails', async () => {
    const workload = setup([{ id: 11, subjectId: 7 }])
    try {
      await workload.initialize()
      teachingApi.getAssignments.mockRejectedValueOnce(new Error('offline'))
      await workload.refreshAssignments()
      expect(workload.assignments.value.map((item) => item.id)).toEqual([11])
      expect(workload.notice.value).toEqual({
        type: 'danger',
        message: 'Не удалось загрузить назначенную учебную нагрузку.',
      })
      expect(workload.loading.value).toBe(false)
    } finally {
      workload.dispose()
    }
  })

  it('ignores a response from an unmounted workload screen', async () => {
    const workload = setup([{ id: 11, subjectId: 7 }])
    await workload.initialize()
    const late = deferred()
    teachingApi.getAssignments.mockReturnValueOnce(late.promise)
    const pending = workload.refreshAssignments()
    workload.dispose()
    late.resolve({ data: [{ id: 777, subjectMembershipId: 11, groupId: 20 }] })
    await pending
    expect(workload.assignments.value.map((item) => item.id)).toEqual([11])
  })
})
