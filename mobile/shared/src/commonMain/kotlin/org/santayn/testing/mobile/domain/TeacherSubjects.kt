package org.santayn.testing.mobile.domain

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.santayn.testing.mobile.core.storage.KeyValueStore
import org.santayn.testing.mobile.data.model.MembershipDto
import org.santayn.testing.mobile.data.model.MembershipStatus
import org.santayn.testing.mobile.data.model.SubjectDto

/**
 * Выбранное назначение преподавателя (subject membership), общее для всех
 * преподавательских экранов: Темы, Вопросы, Лекции, Шаблоны курса, Создание теста.
 * Аналог selectedMembershipId из composables/teacher/useTeacherSubjects.js.
 */
class TeacherSubjectSelection(private val store: KeyValueStore) {
    private val _membershipId = MutableStateFlow(store.getString(KEY)?.toIntOrNull())
    val membershipId: StateFlow<Int?> = _membershipId.asStateFlow()

    fun select(membershipId: Int?) {
        _membershipId.value = membershipId
        store.putString(KEY, membershipId?.toString())
    }

    private companion object {
        const val KEY = "teacher_selected_membership"
    }
}

/** utils/teacherMembershipEligibility.js: isAssignableTeacherMembership. */
fun MembershipDto.isAssignableTeacherMembership(): Boolean =
    role == TEACHER_SUBJECT_ROLE && status == MembershipStatus.ACTIVE && removedAtUtc == null

/** Вариант выбора «предмет преподавателя». */
data class TeacherMembershipOption(
    val membership: MembershipDto,
    val subject: SubjectDto?,
    val label: String,
) {
    val membershipId: Int get() = membership.id
    val subjectId: Int? get() = membership.subjectId
}

/**
 * Подписи вариантов как в useTeacherSubjects.membershipOptions:
 * в режиме админа — с номером преподавателя и назначения,
 * у преподавателя — «назначение N» при дублях.
 */
fun buildTeacherMembershipOptions(
    context: TeacherSubjectContext,
    isAdmin: Boolean,
    personNames: Map<Int, String> = emptyMap(),
): List<TeacherMembershipOption> {
    val active = context.memberships.filter { it.isAssignableTeacherMembership() }
    val countBySubject = active.groupingBy { it.subjectId }.eachCount()
    val indexBySubject = mutableMapOf<Int?, Int>()
    return active.map { membership ->
        val subject = context.subjects.firstOrNull { it.id == membership.subjectId }
        val base = subject?.name ?: "Предмет без названия"
        val index = (indexBySubject[membership.subjectId] ?: 0) + 1
        indexBySubject[membership.subjectId] = index
        val label = when {
            isAdmin -> {
                val teacher = membership.personId?.let { personNames[it] } ?: "преподаватель #${membership.personId ?: "?"}"
                "$base — $teacher"
            }
            (countBySubject[membership.subjectId] ?: 0) > 1 -> "$base — назначение $index"
            else -> base
        }
        TeacherMembershipOption(membership, subject, label)
    }
}

/**
 * Выбор назначения при загрузке: предпочитаемое → единственное по предмету → единственное вообще.
 */
fun resolveTeacherMembership(
    options: List<TeacherMembershipOption>,
    preferredMembershipId: Int?,
    preferredSubjectId: Int?,
): TeacherMembershipOption? {
    options.firstOrNull { it.membershipId == preferredMembershipId }?.let { return it }
    if (preferredSubjectId != null) {
        val bySubject = options.filter { it.subjectId == preferredSubjectId }
        if (bySubject.isNotEmpty()) return bySubject.first()
    }
    return options.singleOrNull() ?: options.firstOrNull()
}
