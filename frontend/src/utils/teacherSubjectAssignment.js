import {
  findReactivatableTeacherMembership,
  TEACHER_SUBJECT_ROLE,
} from '@/utils/teacherMembershipEligibility'

export async function assignSubjectToTeacher({
  api,
  memberships = [],
  personId,
  subjectId,
  notes = '',
}) {
  const normalizedPersonId = Number(personId)
  const normalizedSubjectId = Number(subjectId)
  const normalizedNotes = String(notes ?? '').trim() || null

  const pausedMembership = findReactivatableTeacherMembership(
    memberships,
    normalizedPersonId,
    normalizedSubjectId
  )

  if (pausedMembership) {
    await api.updateSubjectMembership(
      pausedMembership.id,
      {
        status: 1,
        notes:
          normalizedNotes ??
          pausedMembership.notes ??
          null,
      }
    )

    return {
      subjectId: normalizedSubjectId,
      membershipId: Number(pausedMembership.id),
      action: 'reactivated',
    }
  }

  const response = await api.addPersonToSubject(
    normalizedSubjectId,
    {
      personId: normalizedPersonId,
      role: TEACHER_SUBJECT_ROLE,
      notes: normalizedNotes,
    }
  )

  return {
    subjectId: normalizedSubjectId,
    membershipId: Number(response?.data?.id ?? 0) || null,
    action: 'created',
  }
}

export async function assignSubjectsToTeacher({
  api,
  memberships = [],
  personId,
  subjectIds = [],
  notes = '',
}) {
  const results = []

  for (const subjectId of subjectIds) {
    results.push(
      await assignSubjectToTeacher({
        api,
        memberships,
        personId,
        subjectId,
        notes,
      })
    )
  }

  return results
}
