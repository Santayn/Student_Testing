package org.santayn.testing.mobile.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Instant
import org.santayn.testing.mobile.data.model.LectureAssignmentDto

/** Правила видимости лекций студентам — как в PublicLearningRestController.accessibleLecturesForSubject. */
class CourseAccessTests {
    private val now = Instant.parse("2026-10-11T12:00:00Z")

    private fun assignment(
        status: Int = 1,
        from: String? = null,
        closed: String? = null,
    ) = LectureAssignmentDto(id = 1, teachingAssignmentId = 10, courseLectureId = 5, availableFromUtc = from, closedAtUtc = closed, status = status)

    @Test
    fun explicitLectureAssignmentsWinOverCourseVersion() {
        val source = studentLectureSource(courseVersionId = 3, lectureAssignments = listOf(assignment()), versionLectureCount = 0, now = now)
        assertEquals(StudentLectureSource.Assigned(1), source)
        assertFalse(source.hidesAllLectures)
    }

    @Test
    fun emptyCourseVersionHidesAllLectures() {
        val source = studentLectureSource(courseVersionId = 3, lectureAssignments = emptyList(), versionLectureCount = 0, now = now)
        assertEquals(StudentLectureSource.CourseVersion(0), source)
        assertTrue(source.hidesAllLectures)
    }

    @Test
    fun courseVersionWithLecturesOrUnknownCountIsNotFlagged() {
        assertFalse(studentLectureSource(3, emptyList(), versionLectureCount = 4, now = now).hidesAllLectures)
        assertFalse(studentLectureSource(3, emptyList(), versionLectureCount = null, now = now).hidesAllLectures)
    }

    @Test
    fun withoutVersionStudentsSeeSubjectLectures() {
        assertEquals(StudentLectureSource.Subject, studentLectureSource(null, emptyList(), null, now))
    }

    @Test
    fun inactiveOrClosedOrFutureLectureAssignmentsAreIgnored() {
        val ignored = listOf(
            assignment(status = 2),
            assignment(closed = "2026-10-01T00:00:00Z"),
            assignment(from = "2026-12-01T00:00:00Z"),
        )
        ignored.forEach { assertFalse(it.isActiveAt(now), "$it") }
        assertEquals(StudentLectureSource.CourseVersion(0), studentLectureSource(3, ignored, 0, now))
        assertTrue(assignment(from = "2026-10-01T00:00:00Z", closed = "2026-12-01T00:00:00Z").isActiveAt(now))
    }
}
