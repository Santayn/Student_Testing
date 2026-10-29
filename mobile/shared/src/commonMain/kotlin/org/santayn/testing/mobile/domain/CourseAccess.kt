package org.santayn.testing.mobile.domain

import kotlin.time.Instant
import org.santayn.testing.mobile.core.util.parseInstantOrNull
import org.santayn.testing.mobile.data.model.LectureAssignmentDto

/*
 * Какие лекции видят студенты группы по учебной нагрузке — повторяет PublicLearningRestController
 * (accessibleLecturesForSubject): правила проверяются по порядку, срабатывает первое.
 *  1. У нагрузки есть активные назначения лекций (LectureAssignment) — только они.
 *  2. У нагрузки выбрана версия курса — только лекции этой версии.
 *  3. Иначе — лекции предмета преподавателя (по subjectMembershipId).
 */

/** Откуда берутся лекции студентов группы. */
sealed interface StudentLectureSource {
    /** Отдельно назначенные лекции (правило 1). */
    data class Assigned(val count: Int) : StudentLectureSource

    /** Лекции версии курса (правило 2); [count] null — число не удалось узнать. */
    data class CourseVersion(val count: Int?) : StudentLectureSource

    /** Лекции предмета преподавателя (правило 3). */
    data object Subject : StudentLectureSource
}

/** Назначение лекции действует сейчас (статус «активно», период доступности начался и не закрыт). */
fun LectureAssignmentDto.isActiveAt(now: Instant): Boolean {
    if (status != LECTURE_ASSIGNMENT_ACTIVE) return false
    val from = parseInstantOrNull(availableFromUtc)
    val closed = parseInstantOrNull(closedAtUtc)
    return (from == null || now >= from) && (closed == null || now < closed)
}

fun studentLectureSource(
    courseVersionId: Int?,
    lectureAssignments: List<LectureAssignmentDto>,
    versionLectureCount: Int?,
    now: Instant,
): StudentLectureSource {
    val active = lectureAssignments.count { it.isActiveAt(now) }
    return when {
        active > 0 -> StudentLectureSource.Assigned(active)
        courseVersionId != null -> StudentLectureSource.CourseVersion(versionLectureCount)
        else -> StudentLectureSource.Subject
    }
}

/** Студенты группы не увидят ни одной лекции: выбрана пустая версия курса и нет отдельно назначенных лекций. */
val StudentLectureSource.hidesAllLectures: Boolean
    get() = this is StudentLectureSource.CourseVersion && count == 0

private const val LECTURE_ASSIGNMENT_ACTIVE = 1
