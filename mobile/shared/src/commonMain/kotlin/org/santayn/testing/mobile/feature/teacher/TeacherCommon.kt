package org.santayn.testing.mobile.feature.teacher

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import org.santayn.testing.mobile.core.session.SessionManager
import org.santayn.testing.mobile.core.session.WorkspaceRole
import org.santayn.testing.mobile.core.ui.AppCard
import org.santayn.testing.mobile.core.ui.Banner
import org.santayn.testing.mobile.core.ui.SelectField
import org.santayn.testing.mobile.core.ui.SelectOption
import org.santayn.testing.mobile.core.ui.Tone
import org.santayn.testing.mobile.data.api.MembershipsApi
import org.santayn.testing.mobile.data.api.UsersApi
import org.santayn.testing.mobile.domain.ContextCache
import org.santayn.testing.mobile.domain.TeacherContextLoader
import org.santayn.testing.mobile.domain.TeacherMembershipOption
import org.santayn.testing.mobile.domain.TeacherSubjectSelection
import org.santayn.testing.mobile.domain.buildTeacherMembershipOptions
import org.santayn.testing.mobile.domain.isAssignableTeacherMembership
import org.santayn.testing.mobile.domain.resolveTeacherMembership

/** Состояние выбора «предмет преподавателя». */
data class TeacherPickerState(
    val loading: Boolean = true,
    val options: List<TeacherMembershipOption> = emptyList(),
    val selected: TeacherMembershipOption? = null,
    val error: String? = null,
) {
    val selectedMembershipId: Int? get() = selected?.membershipId
    val selectedSubjectId: Int? get() = selected?.subjectId
}

/**
 * Загрузка и выбор назначения преподавателя (composables/teacher/useTeacherSubjects.js).
 * В режиме администратора доступны все активные назначения преподавателей.
 */
class TeacherSubjectsHolder(
    role: WorkspaceRole,
    private val session: SessionManager,
    private val loader: TeacherContextLoader,
    private val selection: TeacherSubjectSelection,
    private val memberships: MembershipsApi,
    private val users: UsersApi,
    private val cache: ContextCache,
) {
    val isAdmin = role == WorkspaceRole.ADMIN
    private val _state = MutableStateFlow(TeacherPickerState())
    val state: StateFlow<TeacherPickerState> = _state.asStateFlow()

    suspend fun load(preferredSubjectId: Int? = null, force: Boolean = false) {
        _state.update { it.copy(loading = true, error = null) }
        try {
            if (force) cache.invalidate("teacher:")
            val context = loader.load(session.currentUser?.personId, isAdmin)
            val names = if (isAdmin) {
                cache.load("people:all", ContextCache.REFERENCE_TTL) { users.getPeople() }.associate { it.id to it.fullName }
            } else {
                emptyMap()
            }
            val options = buildTeacherMembershipOptions(context, isAdmin, names)
            val selected = resolveTeacherMembership(
                options,
                preferredMembershipId = if (preferredSubjectId == null) selection.membershipId.value else null,
                preferredSubjectId = preferredSubjectId,
            )
            selection.select(selected?.membershipId)
            _state.value = TeacherPickerState(loading = false, options = options, selected = selected)
        } catch (e: Exception) {
            _state.update { it.copy(loading = false, error = e.message ?: "Не удалось загрузить предметы преподавателя") }
            throw e
        }
    }

    /** Список предметов не загрузился (например, не было связи). */
    val needsLoad: Boolean get() = _state.value.options.isEmpty() || _state.value.error != null

    fun select(membershipId: Int?) {
        val option = _state.value.options.firstOrNull { it.membershipId == membershipId }
        selection.select(option?.membershipId)
        _state.update { it.copy(selected = option) }
    }

    /**
     * Перед изменениями проверяем, что назначение всё ещё активно
     * (ensureSelectedMembershipActive).
     */
    suspend fun ensureSelectedActive(): TeacherMembershipOption {
        val current = _state.value.selected ?: error("Выберите предмет преподавателя.")
        val fresh = memberships.getSubjectMembership(current.membershipId)
        val personOk = isAdmin || fresh.personId == session.currentUser?.personId
        if (!fresh.isAssignableTeacherMembership() || fresh.subjectId != current.subjectId || !personOk) {
            cache.invalidate("teacher:")
            select(null)
            error("Назначение преподавателя больше не активно. Выберите предмет преподавателя заново.")
        }
        return current
    }
}

/** Карточка выбора предмета преподавателя для верхней части экранов. */
@Composable
fun TeacherSubjectSelector(
    state: TeacherPickerState,
    onSelect: (Int?) -> Unit,
    isAdmin: Boolean,
    extra: @Composable () -> Unit = {},
) {
    AppCard {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            when {
                state.error != null -> Banner(state.error, Tone.DANGER)
                !state.loading && state.options.isEmpty() -> Banner(
                    if (isAdmin) {
                        "Нет активных назначений преподавателей на предметы. Создайте их в разделе «Преподаватели и предметы»."
                    } else {
                        "Вы пока не закреплены ни за одним предметом. Обратитесь к администратору."
                    },
                    Tone.WARNING,
                )
                else -> SelectField(
                    label = if (isAdmin) "Предмет и преподаватель" else "Предмет",
                    options = state.options.map { SelectOption(it.membershipId, it.label) },
                    selected = state.selectedMembershipId,
                    onSelect = onSelect,
                    enabled = !state.loading,
                    placeholder = if (state.loading) "Загрузка…" else "Выберите предмет",
                )
            }
            extra()
            if (isAdmin) {
                Text(
                    "Режим администратора: изменения выполняются от имени выбранного назначения преподавателя.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
