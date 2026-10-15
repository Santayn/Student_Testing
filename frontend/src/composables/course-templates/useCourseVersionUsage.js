import { ref } from 'vue'

import {
  groupsApi,
  lecturesApi,
  teachingApi,
} from '@/api'

import { listFromResponse } from '@/utils/apiData'
import { createLatestRequestGuard } from '@/utils/latestRequest'
import {
  hidesAllLectures,
  studentLectureSource,
} from '@/utils/courseVersionAccess'

const ACTIVE_TEACHING_ASSIGNMENT = 1

const assignmentStatusTitles = {
  1: 'активно',
  2: 'черновик',
  3: 'закрыто',
  4: 'приостановлено',
}

/**
 * Where course versions of the selected template are used: how many lectures a
 * version has and which groups it is issued to through teaching assignments
 * (TeachingAssignment.courseVersionId) of the selected teacher subject.
 *
 * usageByVersionId: versionId → { lectureCount, groups: [text], emptyForGroups: [name] }.
 * emptyForGroups lists active groups that see no lectures at all because the
 * issued version is empty and the group has no separately assigned lectures.
 */
export function useCourseVersionUsage({ versions, selectedMembership }) {
  const usageByVersionId = ref(new Map())
  const request = createLatestRequestGuard()

  function usageOf(versionId) {
    return usageByVersionId.value.get(Number(versionId)) ?? null
  }

  async function loadUsage() {
    const requestId = request.begin()
    usageByVersionId.value = new Map()

    const versionList = versions.value ?? []
    const membershipId = Number(selectedMembership.value?.id ?? 0)

    if (!versionList.length) {
      return
    }

    try {
      const versionIds = new Set(versionList.map((version) => Number(version.id)))

      const [lectureLists, assignmentsResponse] = await Promise.all([
        Promise.all(
          versionList.map((version) =>
            lecturesApi.getAll({ courseVersionId: version.id }).catch(() => null)
          )
        ),
        membershipId
          ? teachingApi.getAssignments({ subjectMembershipId: membershipId }).catch(() => null)
          : Promise.resolve(null),
      ])

      const assignments = listFromResponse(assignmentsResponse)
        .filter((item) => versionIds.has(Number(item.courseVersionId)))

      const groupIds = [...new Set(assignments.map((item) => item.groupId).filter(Boolean))]

      const [groups, lectureAssignmentLists] = await Promise.all([
        Promise.all(groupIds.map((id) => groupsApi.getById(id).catch(() => null))),
        Promise.all(
          assignments.map((item) =>
            teachingApi
              .getLectureAssignments({ teachingAssignmentId: item.id })
              .catch(() => null)
          )
        ),
      ])

      if (!request.isCurrent(requestId)) {
        return
      }

      const groupNames = new Map(
        groups
          .map((response) => response?.data)
          .filter(Boolean)
          .map((group) => [Number(group.id), group.name])
      )

      const lectureCounts = new Map(
        versionList.map((version, index) => [
          Number(version.id),
          lectureLists[index] ? listFromResponse(lectureLists[index]).length : null,
        ])
      )

      const now = Date.now()
      const next = new Map()

      versionList.forEach((version) => {
        const versionId = Number(version.id)
        const lectureCount = lectureCounts.get(versionId)
        const issued = assignments
          .map((item, index) => ({ item, lectureAssignments: listFromResponse(lectureAssignmentLists[index]) }))
          .filter(({ item }) => Number(item.courseVersionId) === versionId)

        const groupTitle = (item) => groupNames.get(Number(item.groupId)) ?? `Группа ${item.groupId}`

        next.set(versionId, {
          lectureCount,
          groups: issued.map(({ item, lectureAssignments }) => {
            const status = Number(item.status) === ACTIVE_TEACHING_ASSIGNMENT
              ? ''
              : `, ${assignmentStatusTitles[item.status] ?? 'неактивно'}`
            const source = studentLectureSource({
              courseVersionId: versionId,
              lectureAssignments,
              versionLectureCount: lectureCount,
              now,
            })
            const assigned = source.kind === 'assigned'
              ? `, назначено лекций отдельно: ${source.count}`
              : ''

            return `${groupTitle(item)} (${item.academicYear}/${Number(item.academicYear) + 1}, ${item.semester} сем.${status}${assigned})`
          }),
          emptyForGroups: issued
            .filter(({ item, lectureAssignments }) =>
              Number(item.status) === ACTIVE_TEACHING_ASSIGNMENT &&
              hidesAllLectures(studentLectureSource({
                courseVersionId: versionId,
                lectureAssignments,
                versionLectureCount: lectureCount,
                now,
              }))
            )
            .map(({ item }) => groupTitle(item)),
        })
      })

      usageByVersionId.value = next
    } catch {
      if (request.isCurrent(requestId)) {
        usageByVersionId.value = new Map()
      }
    }
  }

  function resetUsage() {
    request.invalidate()
    usageByVersionId.value = new Map()
  }

  return {
    usageByVersionId,
    usageOf,
    loadUsage,
    resetUsage,
  }
}
