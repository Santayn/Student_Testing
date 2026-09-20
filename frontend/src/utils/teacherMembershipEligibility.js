export const TEACHER_SUBJECT_ROLE = 1
export const ACTIVE_SUBJECT_MEMBERSHIP_STATUS = 1
export const PAUSED_SUBJECT_MEMBERSHIP_STATUS = 2

export class TeacherMembershipEligibilityError extends Error {
  constructor(
    message =
      'Назначение преподавателя больше не активно. Выберите предмет преподавателя заново.',
    {
      membershipId = null,
      subjectId = null,
      personId = null,
    } = {}
  ) {
    super(message)
    this.name = 'TeacherMembershipEligibilityError'
    this.code = 'TEACHER_MEMBERSHIP_NOT_ASSIGNABLE'
    this.membershipId = membershipId
    this.subjectId = subjectId
    this.personId = personId
  }
}

function positiveNumber(value) {
  const number = Number(value)

  return Number.isFinite(number) && number > 0
    ? number
    : null
}

export function isAssignableTeacherMembership(membership) {
  return (
    Number(membership?.role) ===
      TEACHER_SUBJECT_ROLE &&
    Number(membership?.status) ===
      ACTIVE_SUBJECT_MEMBERSHIP_STATUS &&
    !membership?.removedAtUtc
  )
}

export function assertAssignableTeacherMembership(
  membership,
  {
    expectedMembershipId = null,
    expectedSubjectId = null,
    expectedPersonId = null,
  } = {}
) {
  const membershipId = positiveNumber(
    membership?.id
  )
  const subjectId = positiveNumber(
    membership?.subjectId
  )
  const personId = positiveNumber(
    membership?.personId
  )

  const expectedMembership = positiveNumber(
    expectedMembershipId
  )
  const expectedSubject = positiveNumber(
    expectedSubjectId
  )
  const expectedPerson = positiveNumber(
    expectedPersonId
  )

  const contextMatches =
    (!expectedMembership ||
      membershipId === expectedMembership) &&
    (!expectedSubject ||
      subjectId === expectedSubject) &&
    (!expectedPerson ||
      personId === expectedPerson)

  if (
    !isAssignableTeacherMembership(
      membership
    ) ||
    !contextMatches
  ) {
    throw new TeacherMembershipEligibilityError(
      'Назначение преподавателя больше не активно или изменилось. Выберите предмет преподавателя заново.',
      {
        membershipId:
          membershipId ??
          expectedMembership,
        subjectId:
          subjectId ?? expectedSubject,
        personId:
          personId ?? expectedPerson,
      }
    )
  }

  return membership
}

export async function revalidateAssignableTeacherMembership({
  api,
  membershipId,
  expectedSubjectId = null,
  expectedPersonId = null,
}) {
  const normalizedMembershipId =
    positiveNumber(membershipId)

  if (
    !normalizedMembershipId ||
    typeof api?.getSubjectMembership !==
      'function'
  ) {
    throw new TeacherMembershipEligibilityError(
      'Не удалось определить активное назначение преподавателя.',
      {
        membershipId:
          normalizedMembershipId,
        subjectId:
          positiveNumber(
            expectedSubjectId
          ),
        personId:
          positiveNumber(
            expectedPersonId
          ),
      }
    )
  }

  const response =
    await api.getSubjectMembership(
      normalizedMembershipId
    )

  return assertAssignableTeacherMembership(
    response?.data,
    {
      expectedMembershipId:
        normalizedMembershipId,
      expectedSubjectId,
      expectedPersonId,
    }
  )
}

export async function revalidateAssignableTeacherMembershipIds({
  api,
  membershipIds = [],
}) {
  const ids = [
    ...new Set(
      membershipIds
        .map(positiveNumber)
        .filter(Boolean)
    ),
  ]

  const memberships =
    await Promise.all(
      ids.map((membershipId) =>
        revalidateAssignableTeacherMembership({
          api,
          membershipId,
        })
      )
    )

  return new Set(
    memberships.map((membership) =>
      Number(membership.id)
    )
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
    Number(membership?.status) ===
      PAUSED_SUBJECT_MEMBERSHIP_STATUS &&
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
