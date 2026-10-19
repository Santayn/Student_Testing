/*
 * Какие лекции видят студенты группы по учебной нагрузке — повторяет
 * PublicLearningRestController.accessibleLecturesForSubject: правила проверяются
 * по порядку, срабатывает первое.
 *  1. У нагрузки есть активные назначения лекций (LectureAssignment) — только они.
 *  2. У нагрузки выбрана версия курса — только лекции этой версии.
 *  3. Иначе — лекции предмета преподавателя (по subjectMembershipId).
 */

const LECTURE_ASSIGNMENT_ACTIVE = 1

function parseInstant(value) {
  if (!value) {
    return null
  }

  const time = Date.parse(value)
  return Number.isNaN(time) ? null : time
}

/** Назначение лекции действует сейчас: статус «активно», период начался и не закрыт. */
export function isLectureAssignmentActive(item, now = Date.now()) {
  if (Number(item?.status) !== LECTURE_ASSIGNMENT_ACTIVE) {
    return false
  }

  const from = parseInstant(item.availableFromUtc)
  const closed = parseInstant(item.closedAtUtc)

  return (from === null || now >= from) && (closed === null || now < closed)
}

/**
 * Откуда берутся лекции студентов группы:
 * { kind: 'assigned', count } | { kind: 'version', count } | { kind: 'subject' }.
 * count у версии — null, если число лекций неизвестно.
 */
export function studentLectureSource({
  courseVersionId,
  lectureAssignments = [],
  versionLectureCount = null,
  now = Date.now(),
}) {
  const active = lectureAssignments.filter((item) => isLectureAssignmentActive(item, now)).length

  if (active > 0) {
    return { kind: 'assigned', count: active }
  }

  if (courseVersionId) {
    return { kind: 'version', count: versionLectureCount }
  }

  return { kind: 'subject' }
}

/** Студенты группы не увидят ни одной лекции: выдана пустая версия и отдельных лекций нет. */
export function hidesAllLectures(source) {
  return source?.kind === 'version' && source.count === 0
}

/** Подпись версии для списков: «Шаблон · vN». */
export function courseVersionLabel(version, templateName = '') {
  const number = `v${version?.versionNumber ?? '?'}`
  return templateName ? `${templateName} · ${number}` : number
}

/** Пояснение к версии в выпадающем списке: название и статус публикации. */
export function courseVersionDescription(version) {
  const status = version?.published ? 'опубликована' : 'черновик'
  return [version?.title, status].filter(Boolean).join(' · ')
}
