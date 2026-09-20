import {
  listFromResponse,
  uniqueNumbers,
} from '@/utils/apiData'

export const STUDENT_GROUP_ROLE = 1
export const ACTIVE_GROUP_MEMBERSHIP_STATUS = 1
export const ACTIVE_TEACHING_ASSIGNMENT_STATUS = 1
export const ACTIVE_ENROLLMENT_STATUSES = Object.freeze([1, 2])

export function isActiveStudentGroupMembership(membership) {
  return (
    Number(membership?.role) === STUDENT_GROUP_ROLE &&
    Number(membership?.status) === ACTIVE_GROUP_MEMBERSHIP_STATUS &&
    !membership?.removedAtUtc
  )
}

export function isActiveTeachingAssignment(assignment) {
  return (
    Number(assignment?.status) ===
    ACTIVE_TEACHING_ASSIGNMENT_STATUS
  )
}

export function isActiveTeachingEnrollment(enrollment) {
  return (
    !enrollment?.removedAtUtc &&
    ACTIVE_ENROLLMENT_STATUSES.includes(
      Number(enrollment?.status)
    )
  )
}

function uniqueEntitiesById(items = []) {
  const byId = new Map()

  for (const item of items) {
    const id = Number(item?.id)

    if (Number.isFinite(id) && id > 0) {
      byId.set(id, item)
    }
  }

  return [...byId.values()]
}

export function emptyStudentLearningContext() {
  return {
    memberships: [],
    groups: [],
    faculties: [],
    assignments: [],
    enrollments: [],
    subjectMemberships: [],
    subjects: [],
  }
}

export async function loadStudentLearningContext({
  personId,
  membershipsApi,
  groupsApi,
  facultiesApi,
  teachingApi,
  subjectsApi,
}) {
  const normalizedPersonId = Number(personId)

  if (
    !Number.isFinite(normalizedPersonId) ||
    normalizedPersonId <= 0
  ) {
    return emptyStudentLearningContext()
  }

  const membershipsResponse =
    await membershipsApi.getGroupMemberships({
      personId: normalizedPersonId,
      status: ACTIVE_GROUP_MEMBERSHIP_STATUS,
      activeOnly: true,
    })

  const memberships = listFromResponse(
    membershipsResponse
  ).filter(isActiveStudentGroupMembership)

  if (!memberships.length) {
    return emptyStudentLearningContext()
  }

  const groupIds = uniqueNumbers(
    memberships.map((membership) => membership.groupId)
  )

  const groups = uniqueEntitiesById(
    await Promise.all(
      groupIds.map(async (groupId) => {
        const response = await groupsApi.getById(groupId)
        return response.data
      })
    )
  )

  const facultyIds = uniqueNumbers(
    groups.map((group) => group.facultyId)
  )

  const faculties = uniqueEntitiesById(
    await Promise.all(
      facultyIds.map(async (facultyId) => {
        const response = await facultiesApi.getById(facultyId)
        return response.data
      })
    )
  )

  const [
    groupAssignmentResponses,
    enrollmentResponses,
  ] = await Promise.all([
    Promise.all(
      groupIds.map((groupId) =>
        teachingApi.getAssignments({
          groupId,
          status: ACTIVE_TEACHING_ASSIGNMENT_STATUS,
        })
      )
    ),
    Promise.all(
      memberships.map((membership) =>
        teachingApi.getEnrollments({
          groupMembershipId: membership.id,
        })
      )
    ),
  ])

  const groupAssignments = uniqueEntitiesById(
    groupAssignmentResponses
      .flatMap(listFromResponse)
      .filter(isActiveTeachingAssignment)
  )

  const enrollments = uniqueEntitiesById(
    enrollmentResponses
      .flatMap(listFromResponse)
      .filter(isActiveTeachingEnrollment)
  )

  const enrolledAssignmentIds = uniqueNumbers(
    enrollments.map(
      (enrollment) => enrollment.teachingAssignmentId
    )
  )

  const enrolledAssignments = uniqueEntitiesById(
    (
      await Promise.all(
        enrolledAssignmentIds.map(async (assignmentId) => {
          const response = await teachingApi.getAssignment(
            assignmentId
          )

          return response.data
        })
      )
    ).filter(isActiveTeachingAssignment)
  )

  const assignments = uniqueEntitiesById([
    ...groupAssignments,
    ...enrolledAssignments,
  ])

  const subjectMembershipIds = uniqueNumbers(
    assignments.map(
      (assignment) => assignment.subjectMembershipId
    )
  )

  const subjectMemberships = uniqueEntitiesById(
    await Promise.all(
      subjectMembershipIds.map(async (membershipId) => {
        const response =
          await membershipsApi.getSubjectMembership(
            membershipId
          )

        return response.data
      })
    )
  )

  const subjectIds = uniqueNumbers(
    subjectMemberships.map(
      (membership) => membership.subjectId
    )
  )

  const subjects = uniqueEntitiesById(
    await Promise.all(
      subjectIds.map(async (subjectId) => {
        const response = await subjectsApi.getById(subjectId)
        return response.data
      })
    )
  )

  return {
    memberships,
    groups,
    faculties,
    assignments,
    enrollments,
    subjectMemberships,
    subjects,
  }
}
