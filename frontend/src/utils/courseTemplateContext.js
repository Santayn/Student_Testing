export function buildCourseTemplateListParams({
  subjectId,
  isAdmin,
  currentPersonId,
  selectedMembership,
}) {
  const normalizedSubjectId =
    Number(subjectId)

  const authorPersonId = Number(
    isAdmin
      ? selectedMembership?.personId
      : currentPersonId
  )

  if (
    !normalizedSubjectId ||
    !authorPersonId
  ) {
    return null
  }

  return {
    subjectId: normalizedSubjectId,
    authorPersonId,
  }
}

export function canCreateCourseTemplate({
  isAdmin,
}) {
  return !isAdmin
}
