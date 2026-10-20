package org.santayn.testing.mobile.data.model

import kotlinx.serialization.Serializable

/*
 * DTO бэкенда (backend/.../web/dto/platform/ApiResponses.java и records контроллеров).
 * Даты передаются строками: Instant — ISO-8601 UTC, LocalDate — YYYY-MM-DD, LocalTime — HH:mm[:ss].
 * BigDecimal приходит JSON-числом и читается как Double.
 */

// region Academic structure

@Serializable
data class FacultyDto(
    val id: Int,
    val name: String = "",
    val code: String? = null,
    val description: String? = null,
)

@Serializable
data class FacultyRequest(val name: String, val code: String?, val description: String?)

@Serializable
data class GroupDto(
    val id: Int,
    val name: String = "",
    val code: String? = null,
    val facultyId: Int? = null,
)

@Serializable
data class GroupRequest(val name: String, val code: String?, val facultyId: Int?)

@Serializable
data class SubjectDto(
    val id: Int,
    val name: String = "",
    val description: String? = null,
)

@Serializable
data class SubjectRequest(val name: String, val description: String?)

// endregion

// region Users & roles

@Serializable
data class PersonDto(
    val id: Int,
    val firstName: String? = null,
    val lastName: String? = null,
    val dateOfBirth: String? = null,
    val email: String? = null,
    val phone: String? = null,
) {
    val fullName: String
        get() = listOfNotNull(lastName, firstName).filter { it.isNotBlank() }.joinToString(" ")
            .ifBlank { email ?: "Персона #$id" }
}

@Serializable
data class PersonRequest(
    val firstName: String,
    val lastName: String,
    val dateOfBirth: String?,
    val email: String?,
    val phone: String?,
)

@Serializable
data class UserDto(
    val id: Int,
    val login: String = "",
    val active: Boolean = true,
    val personId: Int? = null,
    val roles: List<String> = emptyList(),
    val permissions: List<String> = emptyList(),
)

@Serializable
data class UserActiveRequest(val active: Boolean)

@Serializable
data class UserPersonRequest(val personId: Int?)

@Serializable
data class UserRolesRequest(val roleIds: List<Int>)

@Serializable
data class RoleDto(
    val id: Int,
    val name: String = "",
    val description: String? = null,
    val permissions: List<PermissionDto> = emptyList(),
)

@Serializable
data class PermissionDto(
    val id: Int,
    val name: String = "",
    val description: String? = null,
)

@Serializable
data class RoleRequest(val name: String, val description: String?)

@Serializable
data class PermissionRequest(val name: String, val description: String?)

@Serializable
data class RolePermissionsRequest(val permissionIds: List<Int>)

// endregion

// region Memberships

/** status: 1 — активно, 2 — приостановлено, 3 — удалено. role = 1: студент в группе / преподаватель в предмете. */
@Serializable
data class MembershipDto(
    val id: Int,
    val groupId: Int? = null,
    val subjectId: Int? = null,
    val facultyId: Int? = null,
    val personId: Int? = null,
    val role: Int = 1,
    val status: Int = 1,
    val assignedAtUtc: String? = null,
    val removedAtUtc: String? = null,
    val notes: String? = null,
) {
    val isActive: Boolean get() = status == MembershipStatus.ACTIVE
}

object MembershipStatus {
    const val ACTIVE = 1
    const val PAUSED = 2
    const val REMOVED = 3

    fun title(status: Int): String = when (status) {
        ACTIVE -> "Активно"
        PAUSED -> "Приостановлено"
        REMOVED -> "Удалено"
        else -> "Статус $status"
    }
}

@Serializable
data class MembershipRequest(val personId: Int, val role: Int = 1, val notes: String? = null)

@Serializable
data class MembershipStatusRequest(val status: Int)

@Serializable
data class MembershipUpdateRequest(val status: Int, val notes: String?)

// endregion

// region Teaching

@Serializable
data class LoadTypeDto(
    val id: Int,
    val name: String = "",
    val description: String? = null,
)

@Serializable
data class LoadTypeRequest(val name: String, val description: String?)

@Serializable
data class SubjectLoadTypeRequest(val teachingLoadTypeId: Int, val notes: String? = null)

/** status: 1 — активно, 2 — черновик, 3 — закрыто, 4 — в паузе. */
@Serializable
data class TeachingAssignmentDto(
    val id: Int,
    val subjectMembershipId: Int? = null,
    val groupId: Int? = null,
    val loadTypeId: Int? = null,
    val courseVersionId: Int? = null,
    val semester: Int = 1,
    val studyCourse: Int? = null,
    val academicYear: Int = 0,
    val hoursPerWeek: Double? = null,
    val status: Int = 1,
    val notes: String? = null,
)

/** Назначение лекции на учебную нагрузку (TeachingRestController /teaching/lecture-assignments). */
@Serializable
data class LectureAssignmentDto(
    val id: Int,
    val teachingAssignmentId: Int? = null,
    val courseLectureId: Int? = null,
    val availableFromUtc: String? = null,
    val dueToUtc: String? = null,
    val closedAtUtc: String? = null,
    val required: Boolean = false,
    val status: Int = 1,
)

object TeachingAssignmentStatus {
    const val ACTIVE = 1
    const val DRAFT = 2
    const val CLOSED = 3
    const val PAUSED = 4

    val all = listOf(ACTIVE, DRAFT, CLOSED, PAUSED)

    fun title(status: Int): String = when (status) {
        ACTIVE -> "Активно"
        DRAFT -> "Черновик"
        CLOSED -> "Закрыто"
        PAUSED -> "В паузе"
        else -> "Статус $status"
    }
}

@Serializable
data class TeachingAssignmentRequest(
    val subjectMembershipId: Int,
    val groupId: Int,
    val loadTypeId: Int?,
    val courseVersionId: Int?,
    val semester: Int,
    val studyCourse: Int?,
    val academicYear: Int,
    val hoursPerWeek: Double?,
    val status: Int?,
    val notes: String?,
)

@Serializable
data class EnrollmentDto(
    val id: Int,
    val teachingAssignmentId: Int? = null,
    val groupMembershipId: Int? = null,
    val groupId: Int? = null,
    val status: Int = 1,
    val enrolledAtUtc: String? = null,
    val removedAtUtc: String? = null,
)

// endregion

// region Courses

@Serializable
data class CourseTemplateDto(
    val id: Int,
    val subjectId: Int? = null,
    val authorPersonId: Int? = null,
    val name: String = "",
    val publicVisible: Boolean = false,
    val createdAt: String? = null,
)

@Serializable
data class CourseTemplateRequest(val subjectId: Int, val name: String, val publicVisible: Boolean)

@Serializable
data class CourseVersionDto(
    val id: Int,
    val courseTemplateId: Int? = null,
    val versionNumber: Int = 1,
    val title: String = "",
    val description: String? = null,
    val published: Boolean = false,
    val createdByPersonId: Int? = null,
    val publishedAtUtc: String? = null,
    val publishedByPersonId: Int? = null,
    val changeNotes: String? = null,
    val createdAt: String? = null,
)

@Serializable
data class CourseVersionRequest(
    val versionNumber: Int,
    val title: String,
    val description: String?,
    val published: Boolean,
    val changeNotes: String?,
)

@Serializable
data class CourseVersionUpdateRequest(
    val versionNumber: Int,
    val title: String,
    val description: String?,
    val changeNotes: String?,
)

// endregion

// region Lectures & topics

@Serializable
data class LectureDto(
    val id: Int,
    val subjectId: Int? = null,
    val subjectMembershipId: Int? = null,
    val courseVersionId: Int? = null,
    val ordinal: Int = 0,
    val title: String = "",
    val description: String? = null,
    val contentFolderKey: String? = null,
    val linkedTestId: Int? = null,
    val publicVisible: Boolean = false,
    /** Текст лекции в Markdown. */
    val body: String? = null,
)

@Serializable
data class LectureRequest(
    val subjectId: Int?,
    val subjectMembershipId: Int?,
    val courseVersionId: Int?,
    val ordinal: Int,
    val title: String,
    val description: String?,
    val contentFolderKey: String,
    val linkedTestId: Int?,
    val publicVisible: Boolean,
    /** null — не менять текст лекции, пустая строка — удалить. */
    val body: String? = null,
)

@Serializable
data class LectureTestsRequest(val testIds: List<Int>)

@Serializable
data class LectureMaterialDto(
    val id: Int,
    val courseLectureId: Int? = null,
    val fileName: String = "",
    val contentType: String? = null,
    val sizeBytes: Long = 0,
    val uploadedAtUtc: String? = null,
)

@Serializable
data class TopicDto(
    val id: Int,
    val subjectId: Int? = null,
    val subjectMembershipId: Int? = null,
    val courseLectureId: Int? = null,
    val ordinal: Int = 0,
    val name: String = "",
    val description: String? = null,
)

@Serializable
data class TopicRequest(
    val subjectId: Int?,
    val courseLectureId: Int?,
    val subjectMembershipId: Int?,
    val ordinal: Int,
    val name: String,
    val description: String?,
)

// endregion

// region Tests & questions

@Serializable
data class TestDto(
    val id: Int,
    val title: String = "",
    val description: String? = null,
    val duration: String? = null,
    val attemptsAllowed: Int = 1,
    val questionCount: Int = 0,
    val showAiFeedback: Boolean = true,
)

@Serializable
data class TestRequest(
    val title: String,
    val description: String?,
    val duration: String?,
    val attemptsAllowed: Int,
    val questionCount: Int,
    val selectionRules: List<SelectionRuleRequest>,
    val showAiFeedback: Boolean? = null,
)

@Serializable
data class SelectionRuleRequest(
    val courseLectureId: Int?,
    val topicId: Int?,
    val questionCount: Int,
    val textQuestionCount: Int,
    val singleAnswerQuestionCount: Int,
    val multipleAnswerQuestionCount: Int,
    val matchingQuestionCount: Int,
    val ordinal: Int,
)

/** scope: 1 — глобально, 2 — версия курса, 3 — лекция, 4 — учебное назначение (группа). */
@Serializable
data class TestAssignmentRequest(
    val scope: Int,
    val courseVersionId: Int?,
    val courseLectureId: Int?,
    val teachingAssignmentId: Int?,
    val availableFromUtc: String?,
    val availableUntilUtc: String?,
    val status: Int,
)

@Serializable
data class TestAssignmentDto(
    val id: Int,
    val testId: Int? = null,
    val scope: Int = 4,
    val courseVersionId: Int? = null,
    val courseLectureId: Int? = null,
    val teachingAssignmentId: Int? = null,
    val availableFromUtc: String? = null,
    val availableUntilUtc: String? = null,
    val status: Int = 1,
)

/** Статус назначения теста: 1 — черновик, 2 — активно, 3 — закрыто, 4 — приостановлено. */
object TestAssignmentStatus {
    const val DRAFT = 1
    const val ACTIVE = 2
    const val CLOSED = 3
    const val PAUSED = 4

    val all = listOf(DRAFT, ACTIVE, CLOSED, PAUSED)

    fun title(status: Int): String = when (status) {
        DRAFT -> "Черновик"
        ACTIVE -> "Активно"
        CLOSED -> "Закрыто"
        PAUSED -> "Приостановлено"
        else -> "Статус $status"
    }
}

/** Тип вопроса: 1 — один ответ, 2 — несколько, 3 — сопоставление, 4 — текст. */
object QuestionType {
    const val SINGLE = 1
    const val MULTIPLE = 2
    const val MATCHING = 3
    const val TEXT = 4

    val all = listOf(SINGLE, MULTIPLE, MATCHING, TEXT)

    fun title(type: Int): String = when (type) {
        SINGLE -> "Один вариант"
        MULTIPLE -> "Несколько вариантов"
        MATCHING -> "Сопоставление"
        TEXT -> "Текстовый ответ"
        else -> "Тип $type"
    }

    fun usesOptions(type: Int) = type == SINGLE || type == MULTIPLE
}

@Serializable
data class QuestionDto(
    val id: Long,
    val testId: Int? = null,
    val courseLectureId: Int? = null,
    val topicId: Int? = null,
    val type: Int = QuestionType.SINGLE,
    val question: String = "",
    val points: Double? = null,
    val ordinal: Int = 0,
    val correctAnswer: String? = null,
    val matchingPairs: List<MatchingPairDto> = emptyList(),
    val active: Boolean = true,
    /** Критерии оценивания свободного ответа. */
    val gradingCriteria: String? = null,
)

@Serializable
data class MatchingPairDto(
    val ordinal: Int = 0,
    val left: String = "",
    val right: String = "",
)

@Serializable
data class QuestionRequest(
    val testId: Int?,
    val courseLectureId: Int?,
    val topicId: Int?,
    val type: Int,
    val question: String,
    val points: Double?,
    val ordinal: Int,
    val correctAnswer: String?,
    val matchingPairs: List<MatchingPairDto>?,
    val gradingCriteria: String? = null,
)

@Serializable
data class QuestionUpdateRequest(
    val courseLectureId: Int?,
    val topicId: Int?,
    val type: Int,
    val question: String,
    val points: Double?,
    val ordinal: Int,
    val correctAnswer: String?,
    val matchingPairs: List<MatchingPairDto>?,
    val active: Boolean,
    /** null — не менять критерии, пустая строка — удалить. */
    val gradingCriteria: String? = null,
)

@Serializable
data class QuestionActiveRequest(val active: Boolean)

@Serializable
data class QuestionOptionDto(
    val id: Long,
    val testQuestionId: Long? = null,
    val text: String = "",
    val ordinal: Int = 0,
    val correct: Boolean = false,
)

@Serializable
data class OptionRequest(val text: String, val ordinal: Int, val correct: Boolean)

@Serializable
data class QuestionImportResponse(
    val importedQuestions: Int = 0,
    val importedOptions: Int = 0,
    val questions: List<QuestionDto> = emptyList(),
)

// endregion

// region Student learning (/public/learning)

@Serializable
data class PublicSubjectDto(
    val id: Int,
    val name: String = "",
    val description: String? = null,
)

@Serializable
data class PublicLectureDto(
    val id: Int,
    val courseVersionId: Int? = null,
    val subjectId: Int? = null,
    val courseTemplateId: Int? = null,
    val courseName: String? = null,
    val versionId: Int? = null,
    val versionNumber: Int = 0,
    val versionTitle: String? = null,
    val ordinal: Int = 0,
    val title: String = "",
    val description: String? = null,
    val publicVisible: Boolean = true,
    val body: String? = null,
)

@Serializable
data class PublicTestDto(
    val id: Int,
    val assignmentId: Int? = null,
    val title: String = "",
    val description: String? = null,
    val duration: String? = null,
    val attemptsAllowed: Int = 0,
    val questionCount: Int = 0,
    val available: Boolean = false,
    val statusMessage: String? = null,
    val attemptsRemaining: Int = 0,
    val canResume: Boolean = false,
)

@Serializable
data class PublicMaterialDto(
    val id: Int,
    val fileName: String = "",
    val contentType: String? = null,
    val sizeBytes: Long = 0,
    val uploadedAtUtc: String? = null,
)

@Serializable
data class AttemptLoadDto(
    val attemptId: Int,
    val assignmentId: Int? = null,
    val test: PublicTestDto? = null,
    val questions: List<PublicQuestionDto> = emptyList(),
)

@Serializable
data class PublicQuestionDto(
    val id: Long,
    val type: Int = QuestionType.SINGLE,
    val text: String? = null,
    val question: String? = null,
    val points: Double? = null,
    val ordinal: Int = 0,
    val options: List<PublicOptionDto> = emptyList(),
    val matchingPrompts: List<MatchingPromptDto> = emptyList(),
    val matchingOptions: List<String> = emptyList(),
) {
    val displayText: String get() = text?.takeIf { it.isNotBlank() } ?: question.orEmpty()
}

@Serializable
data class PublicOptionDto(val id: Long, val text: String = "", val ordinal: Int = 0)

@Serializable
data class MatchingPromptDto(val ordinal: Int, val text: String = "")

@Serializable
data class SubmitAttemptRequest(
    val questionIds: List<Long>,
    val answers: List<String>,
    val selectedOptionIds: List<List<Long>>,
)

@Serializable
data class SubmitAttemptResponse(
    val attemptId: Int,
    val score: Double? = null,
    val correctCount: Int = 0,
    val totalCount: Int = 0,
    val details: List<SubmitDetailDto> = emptyList(),
    /** Сколько ответов ещё проверяют ИИ или преподаватель: балл предварительный. */
    val pendingReviewCount: Int = 0,
)

@Serializable
data class SubmitDetailDto(
    val questionText: String? = null,
    val givenAnswer: String? = null,
    val correctAnswer: String? = null,
    val correct: Boolean = false,
    val pendingReview: Boolean = false,
)

// endregion

// region Results

@Serializable
data class ResultSubjectDto(
    val id: Int,
    val name: String = "",
    val description: String? = null,
    val membershipId: Int? = null,
)

@Serializable
data class ResultLectureDto(
    val id: Int,
    val courseVersionId: Int? = null,
    val subjectId: Int? = null,
    val courseName: String? = null,
    val versionNumber: Int = 0,
    val ordinal: Int = 0,
    val title: String = "",
)

@Serializable
data class ResultTestDto(
    val id: Int,
    val title: String = "",
    val description: String? = null,
    val questionCount: Int = 0,
)

@Serializable
data class ResultGroupDto(
    val id: Int,
    val name: String = "",
    val code: String? = null,
    val facultyId: Int? = null,
)

@Serializable
data class ResultPersonDto(
    val id: Int,
    val firstName: String? = null,
    val lastName: String? = null,
    val fullName: String? = null,
    val email: String? = null,
) {
    val displayName: String
        get() = fullName?.takeIf { it.isNotBlank() }
            ?: listOfNotNull(lastName, firstName).joinToString(" ").ifBlank { email ?: "#$id" }
}

@Serializable
data class ResultDataDto(
    val stats: ResultStatsDto? = null,
    val selectedTestName: String? = null,
    val selectedGroupName: String? = null,
    val selectedStudentName: String? = null,
    val attemptCount: Int = 0,
    val attempts: List<ResultAttemptDto> = emptyList(),
)

@Serializable
data class ResultStatsDto(
    val total: Int = 0,
    val right: Long = 0,
    val percent: Double? = null,
)

@Serializable
data class ResultAttemptDto(
    val attemptId: Int,
    val testId: Int? = null,
    val testName: String? = null,
    val studentId: Int? = null,
    val studentName: String? = null,
    val attemptOrdinal: Int = 0,
    val completedAt: String? = null,
    val stats: ResultStatsDto? = null,
    val results: List<ResultItemDto> = emptyList(),
)

@Serializable
data class ResultItemDto(
    val questionText: String? = null,
    val givenAnswer: String? = null,
    val correctAnswer: String? = null,
    val correct: Boolean = false,
    val questionPoints: Double? = null,
    val awardedPoints: Double? = null,
    val gradingStatus: String? = null,
    val gradingNote: String? = null,
    val responseId: Long? = null,
    val questionType: Int = 0,
    /** none | ai_pending | needs_teacher | ai_graded | teacher_confirmed */
    val reviewStatus: String? = null,
    val gradedBy: String? = null,
    val aiFeedback: String? = null,
    val teacherComment: String? = null,
    val aiConfidence: Double? = null,
) {
    val reviewPending: Boolean
        get() = reviewStatus == ReviewStatus.AI_PENDING || reviewStatus == ReviewStatus.NEEDS_TEACHER
}

// endregion

// region Admin

@Serializable
data class RestoreResultDto(
    val status: String? = null,
    val fileName: String? = null,
    val sizeBytes: Long = 0,
    val restoredAtUtc: String? = null,
)

// endregion

// region AI

object ReviewStatus {
    const val AI_PENDING = "ai_pending"
    const val NEEDS_TEACHER = "needs_teacher"
    const val AI_GRADED = "ai_graded"
    const val TEACHER_CONFIRMED = "teacher_confirmed"
    const val ALL = "all"

    fun title(status: String?): String = when (status) {
        AI_PENDING -> "Проверяет ИИ"
        NEEDS_TEACHER -> "Нужна проверка"
        AI_GRADED -> "Оценено ИИ"
        TEACHER_CONFIRMED -> "Проверено"
        else -> "Без проверки"
    }
}

@Serializable
data class AiStatusDto(
    val enabled: Boolean = false,
    val provider: String? = null,
    val grading: Boolean = false,
    val lectureScan: Boolean = false,
    val ocrEngine: String? = null,
    val presentation: Boolean = false,
    val imageGeneration: Boolean = false,
    val imageSearch: Boolean = false,
)

@Serializable
data class AiJobDto(
    val id: Long,
    val type: String = "",
    /** queued | running | done | failed */
    val status: String = "queued",
    val progress: String? = null,
    val error: String? = null,
    val result: kotlinx.serialization.json.JsonObject? = null,
) {
    val active: Boolean get() = status == "queued" || status == "running"
}

@Serializable
data class CriteriaSuggestionRequest(val question: String, val correctAnswer: String?, val points: Double?)

@Serializable
data class CriteriaSuggestionDto(val criteria: String = "")

@Serializable
data class ReviewQueueDto(
    val items: List<ReviewItemDto> = emptyList(),
    val counts: Map<String, Long> = emptyMap(),
)

@Serializable
data class ReviewItemDto(
    val responseId: Long,
    val attemptId: Int? = null,
    val testId: Int? = null,
    val testName: String? = null,
    val studentName: String? = null,
    val questionType: Int = 0,
    val questionText: String? = null,
    val questionPoints: Double? = null,
    val correctAnswer: String? = null,
    val gradingCriteria: String? = null,
    val answerText: String? = null,
    val awardedPoints: Double? = null,
    val correct: Boolean = false,
    val reviewStatus: String? = null,
    val gradedBy: String? = null,
    val aiConfidence: Double? = null,
    val aiFeedback: String? = null,
    val matched: List<String> = emptyList(),
    val missing: List<String> = emptyList(),
    val suspicious: Boolean = false,
    val teacherComment: String? = null,
    val completedAt: String? = null,
)

@Serializable
data class GradeResponseRequest(val awardedPoints: Double, val comment: String?)

// endregion