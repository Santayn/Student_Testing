package org.santayn.testing.mobile.domain

import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.santayn.testing.mobile.core.util.nowInstant
import org.santayn.testing.mobile.data.api.FacultiesApi
import org.santayn.testing.mobile.data.api.GroupsApi
import org.santayn.testing.mobile.data.api.MembershipsApi
import org.santayn.testing.mobile.data.api.SubjectsApi
import org.santayn.testing.mobile.data.api.TeachingApi
import org.santayn.testing.mobile.data.model.EnrollmentDto
import org.santayn.testing.mobile.data.model.FacultyDto
import org.santayn.testing.mobile.data.model.GroupDto
import org.santayn.testing.mobile.data.model.MembershipDto
import org.santayn.testing.mobile.data.model.SubjectDto
import org.santayn.testing.mobile.data.model.TeachingAssignmentDto
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

/**
 * Кэш справочных данных с TTL (frontend/src/utils/learningContextCache.js).
 * Сбрасывается при смене сессии/рабочей роли и после изменений данных.
 */
class ContextCache {
    private class Entry(val value: Any?, val expiresAt: Instant)

    private val mutex = Mutex()
    private val entries = mutableMapOf<String, Entry>()

    @Suppress("UNCHECKED_CAST")
    suspend fun <T> load(key: String, ttl: Duration, loader: suspend () -> T): T {
        mutex.withLock {
            val cached = entries[key]
            if (cached != null && cached.expiresAt > nowInstant()) return cached.value as T
        }
        val value = loader()
        mutex.withLock { entries[key] = Entry(value, nowInstant() + ttl) }
        return value
    }

    suspend fun invalidate(prefix: String? = null) = mutex.withLock {
        if (prefix == null) entries.clear() else entries.keys.removeAll { it.startsWith(prefix) }
    }

    companion object {
        val LEARNING_CONTEXT_TTL = 30.seconds
        val REFERENCE_TTL = 5.minutes
    }
}

/** Ключи с одинаковым id оставляют последнее значение (uniqueEntitiesById). */
private fun <T> List<T>.uniqueBy(id: (T) -> Int): List<T> = associateBy(id).values.toList()

private val ruCollator: Comparator<String> = compareBy<String> { it.lowercase() }

fun <T> List<T>.sortedByName(name: (T) -> String): List<T> = sortedWith(compareBy(ruCollator) { name(it) })

// region Student context (utils/studentLearningContext.js)

const val STUDENT_GROUP_ROLE = 1
const val TEACHER_SUBJECT_ROLE = 1
const val ACTIVE_STATUS = 1
private val ACTIVE_ENROLLMENT_STATUSES = setOf(1, 2)

fun MembershipDto.isActiveStudentGroupMembership() =
    role == STUDENT_GROUP_ROLE && status == ACTIVE_STATUS && removedAtUtc == null

fun TeachingAssignmentDto.isActiveAssignment() = status == ACTIVE_STATUS

fun EnrollmentDto.isActiveEnrollment() = removedAtUtc == null && status in ACTIVE_ENROLLMENT_STATUSES

data class StudentLearningContext(
    val memberships: List<MembershipDto> = emptyList(),
    val groups: List<GroupDto> = emptyList(),
    val faculties: List<FacultyDto> = emptyList(),
    val assignments: List<TeachingAssignmentDto> = emptyList(),
    val enrollments: List<EnrollmentDto> = emptyList(),
    val subjectMemberships: List<MembershipDto> = emptyList(),
    val subjects: List<SubjectDto> = emptyList(),
) {
    val hasActiveGroup: Boolean get() = memberships.isNotEmpty()

    /** Факультет предмета — через назначение → группу → факультет. */
    fun facultyIdForSubject(subjectId: Int): Int? {
        val membershipIds = subjectMemberships.filter { it.subjectId == subjectId }.map { it.id }.toSet()
        val groupId = assignments.firstOrNull { it.subjectMembershipId in membershipIds }?.groupId
        return groups.firstOrNull { it.id == groupId }?.facultyId
    }
}

class StudentContextLoader(
    private val memberships: MembershipsApi,
    private val groups: GroupsApi,
    private val faculties: FacultiesApi,
    private val teaching: TeachingApi,
    private val subjects: SubjectsApi,
    private val cache: ContextCache,
) {
    suspend fun load(personId: Int?): StudentLearningContext {
        if (personId == null || personId <= 0) return StudentLearningContext()
        return cache.load("student:context:$personId", ContextCache.LEARNING_CONTEXT_TTL) { fetch(personId) }
    }

    private suspend fun fetch(personId: Int): StudentLearningContext = coroutineScope {
        val activeMemberships = memberships
            .getGroupMemberships(personId = personId, status = ACTIVE_STATUS, activeOnly = true)
            .filter { it.isActiveStudentGroupMembership() }
        if (activeMemberships.isEmpty()) return@coroutineScope StudentLearningContext()

        val groupIds = activeMemberships.mapNotNull { it.groupId }.distinct()

        val groupsDeferred = groupIds.map { id ->
            async { cache.load("group:$id", ContextCache.REFERENCE_TTL) { groups.getById(id) } }
        }
        val assignmentsDeferred = groupIds.map { id ->
            async { teaching.getAssignments(groupId = id, status = ACTIVE_STATUS) }
        }
        val enrollmentsDeferred = activeMemberships.map { m ->
            async { teaching.getEnrollments(groupMembershipId = m.id) }
        }

        val loadedGroups = groupsDeferred.awaitAll().uniqueBy { it.id }
        val groupAssignments = assignmentsDeferred.awaitAll().flatten()
            .filter { it.isActiveAssignment() }.uniqueBy { it.id }
        val enrollments = enrollmentsDeferred.awaitAll().flatten()
            .filter { it.isActiveEnrollment() }.uniqueBy { it.id }

        val knownAssignmentIds = groupAssignments.map { it.id }.toSet()
        val missingAssignmentIds = enrollments.mapNotNull { it.teachingAssignmentId }
            .distinct().filter { it !in knownAssignmentIds }
        val facultyIds = loadedGroups.mapNotNull { it.facultyId }.distinct()

        val facultiesDeferred = facultyIds.map { id ->
            async { cache.load("faculty:$id", ContextCache.REFERENCE_TTL) { faculties.getById(id) } }
        }
        val enrolledAssignmentsDeferred = missingAssignmentIds.map { id -> async { teaching.getAssignment(id) } }

        val loadedFaculties = facultiesDeferred.awaitAll().uniqueBy { it.id }
        val enrolledAssignments = enrolledAssignmentsDeferred.awaitAll().filter { it.isActiveAssignment() }
        val assignments = (groupAssignments + enrolledAssignments).uniqueBy { it.id }

        val subjectMembershipIds = assignments.mapNotNull { it.subjectMembershipId }.distinct()
        val subjectMemberships = subjectMembershipIds.map { id ->
            async {
                cache.load("subject-membership:$id", ContextCache.REFERENCE_TTL) {
                    memberships.getSubjectMembership(id)
                }
            }
        }.awaitAll().uniqueBy { it.id }

        val subjectIds = subjectMemberships.mapNotNull { it.subjectId }.distinct()
        val loadedSubjects = subjectIds.map { id ->
            async { cache.load("subject:$id", ContextCache.REFERENCE_TTL) { subjects.getById(id) } }
        }.awaitAll().uniqueBy { it.id }

        StudentLearningContext(
            memberships = activeMemberships,
            groups = loadedGroups.sortedByName { it.name },
            faculties = loadedFaculties.sortedByName { it.name },
            assignments = assignments,
            enrollments = enrollments,
            subjectMemberships = subjectMemberships,
            subjects = loadedSubjects.sortedByName { it.name },
        )
    }
}

// endregion

// region Teacher context (utils/teacherSubjectContext.js)

data class TeacherSubjectContext(
    val memberships: List<MembershipDto> = emptyList(),
    val subjects: List<SubjectDto> = emptyList(),
) {
    /** Membership преподавателя для предмета (первая по id, как во фронте). */
    fun membershipFor(subjectId: Int?): MembershipDto? = memberships.firstOrNull { it.subjectId == subjectId }
}

class TeacherContextLoader(
    private val memberships: MembershipsApi,
    private val subjects: SubjectsApi,
    private val cache: ContextCache,
) {
    /**
     * @param isAdmin в режиме администратора берутся все активные teacher-membership'ы.
     */
    suspend fun load(personId: Int?, isAdmin: Boolean): TeacherSubjectContext {
        if (!isAdmin && personId == null) {
            error("Не удалось определить преподавателя по текущему профилю.")
        }
        val key = "teacher:context:${if (isAdmin) "admin" else personId}"
        return cache.load(key, ContextCache.LEARNING_CONTEXT_TTL) {
            val list = memberships
                .getSubjectMemberships(personId = if (isAdmin) null else personId, activeOnly = true)
                .filter { it.role == TEACHER_SUBJECT_ROLE }
                .sortedBy { it.id }
            val allowed = list.mapNotNull { it.subjectId }.toSet()
            val subjectList = if (allowed.isEmpty()) {
                emptyList()
            } else {
                subjectCatalog().filter { it.id in allowed }.sortedByName { it.name }
            }
            TeacherSubjectContext(list, subjectList)
        }
    }

    suspend fun subjectCatalog(): List<SubjectDto> =
        cache.load("subjects:catalog", ContextCache.REFERENCE_TTL) { subjects.getAll() }
}

// endregion
