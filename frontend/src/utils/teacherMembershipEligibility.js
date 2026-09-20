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
