export const TEACHER_SUBJECT_ROLE = 1

export function isAssignableTeacherMembership(membership) {
  return (
    Number(membership?.role) ===
      TEACHER_SUBJECT_ROLE &&
    Number(membership?.status) === 1 &&
    !membership?.removedAtUtc
  )
}

export function assignableTeacherMembershipIds(memberships = []) {
  return new Set(
    memberships
      .filter(isAssignableTeacherMembership)
      .map((membership) =>
        Number(membership.id)
      )
  )
}

export function isReactivatableTeacherMembership(membership) {
  return (
    Number(membership?.role) ===
      TEACHER_SUBJECT_ROLE &&
    Number(membership?.status) === 2 &&
    !membership?.removedAtUtc
  )
}

export function findReactivatableTeacherMembership(
  memberships = [],
  personId,
  subjectId
) {
  return (
    memberships.find(
      (membership) =>
        Number(membership?.personId) === Number(personId) &&
        Number(membership?.subjectId) === Number(subjectId) &&
        isReactivatableTeacherMembership(membership)
    ) ?? null
  )
}
