import {
  findReactivatableTeacherMembership,
  TEACHER_SUBJECT_ROLE,
} from '@/utils/teacherMembershipEligibility'

export async function assignSubjectsToTeacher({
  api,
  memberships = [],
  personId,
  subjectIds = [],
  notes = '',
}) {
  const normalizedPersonId = Number(personId)
  const normalizedNotes = String(notes ?? '').trim() || null
  const results = []

  for (const rawSubjectId of subjectIds) {
    const subjectId = Number(rawSubjectId)
    const pausedMembership = findReactivatableTeacherMembership(
      memberships,
      normalizedPersonId,
      subjectId
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

      results.push({
        subjectId,
        membershipId: Number(pausedMembership.id),
        action: 'reactivated',
      })
      continue
    }

    const response = await api.addPersonToSubject(
      subjectId,
      {
        personId: normalizedPersonId,
        role: TEACHER_SUBJECT_ROLE,
        notes: normalizedNotes,
      }
    )

    results.push({
      subjectId,
      membershipId: Number(response?.data?.id ?? 0) || null,
      action: 'created',
    })
  }

  return results
}
