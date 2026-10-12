package org.santayn.testing.mobile.feature.home

import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.santayn.testing.mobile.core.network.userMessage
import org.santayn.testing.mobile.core.session.SessionManager
import org.santayn.testing.mobile.core.session.WorkspaceRole
import org.santayn.testing.mobile.core.ui.BaseViewModel
import org.santayn.testing.mobile.core.ui.LoadState
import org.santayn.testing.mobile.core.util.parseInstantOrNull
import org.santayn.testing.mobile.data.api.ResultsApi
import org.santayn.testing.mobile.domain.StudentContextLoader
import org.santayn.testing.mobile.domain.scoreSummary
import org.santayn.testing.mobile.feature.learning.SubjectItem
import org.santayn.testing.mobile.navigation.MenuEntry
import org.santayn.testing.mobile.navigation.Menus

/*
 * Данные главной — общие для всех тем оформления. Каждая тема рисует их по-своему
 * (PolytechHome.kt и т. д.), но загружает одинаково.
 */

/** Последняя завершённая попытка студента. */
data class LastAttempt(
    val testName: String,
    val percent: Double,
    val completedAt: String?,
)

/** Данные главной студента. */
data class StudentHome(
    /** «ПИ-201 · Software Engineering». */
    val contextLine: String?,
    val subjects: List<SubjectItem>,
    val lastAttempt: LastAttempt?,
    /** Пояснение, если предметов нет (нет группы / не назначены). */
    val emptyReason: String?,
)

class HomeViewModel(
    private val role: WorkspaceRole,
    private val session: SessionManager,
    private val studentLoader: StudentContextLoader,
    private val results: ResultsApi,
) : BaseViewModel() {

    /** Только для студента: у преподавателя и админа главная — меню разделов. */
    private val _student = MutableStateFlow<LoadState<StudentHome>>(LoadState.Loading)
    val student = _student.asStateFlow()

    init {
        load()
    }

    fun load() {
        if (role != WorkspaceRole.STUDENT) return
        if (_student.value !is LoadState.Ready) _student.value = LoadState.Loading
        launchSafe(onError = { e ->
            if (_student.value !is LoadState.Ready) _student.value = LoadState.Error(e.userMessage())
        }) {
            coroutineScope {
                val context = async { studentLoader.load(session.currentUser?.personId) }
                // Результаты — дополнительная информация: их ошибка не должна ломать главную.
                val attempts = async { runCatching { results.getStudentData() }.getOrNull()?.attempts.orEmpty() }
                val ctx = context.await()
                val last = attempts.await()
                    .maxByOrNull { parseInstantOrNull(it.completedAt)?.toEpochMilliseconds() ?: Long.MIN_VALUE }
                _student.value = LoadState.Ready(
                    StudentHome(
                        contextLine = listOf(
                            ctx.groups.joinToString(", ") { it.name },
                            ctx.faculties.joinToString(", ") { it.name },
                        ).filter { it.isNotBlank() }.joinToString(" · ").ifBlank { null },
                        subjects = ctx.subjects.map { SubjectItem(it.id, it.name, it.description, ctx.facultyIdForSubject(it.id)) },
                        lastAttempt = last?.let {
                            LastAttempt(it.testName ?: "Тест", it.scoreSummary().percent, it.completedAt)
                        },
                        emptyReason = when {
                            !ctx.hasActiveGroup -> "Вы пока не состоите в учебной группе. Обратитесь к администратору."
                            ctx.subjects.isEmpty() -> "Для вашей группы пока не назначены предметы."
                            else -> null
                        },
                    )
                )
            }
        }
    }
}

/** Разделы меню главной для преподавателя и администратора (HomeView.vue). */
fun homeSections(role: WorkspaceRole): List<Pair<String, List<MenuEntry>>> = when (role) {
    WorkspaceRole.STUDENT -> emptyList()
    WorkspaceRole.TEACHER -> listOf(
        "Учебный контент" to Menus.teacherContent,
        "Работа" to listOf(
            Menus.teacherWorkload,
            Menus.subjects.copy(title = "Мои предметы", description = "Предметы, за которыми вы закреплены"),
            Menus.results.copy(description = "Результаты студентов по вашим тестам"),
        ),
    )
    WorkspaceRole.ADMIN -> listOf(
        "Обзор" to listOf(
            Menus.results.copy(description = "Результаты по всем предметам"),
            Menus.subjects.copy(title = "Доступные предметы", description = "Каталог предметов"),
        ),
        "Академическая структура" to Menus.adminStructure,
        "Учебный контент" to Menus.teacherContent,
        "Управление доступом" to Menus.adminAccess,
    )
}

/** Имя для приветствия: имя персоны, иначе первое слово отображаемого имени. */
fun greetingName(firstName: String?, displayName: String): String =
    firstName?.takeIf { it.isNotBlank() } ?: displayName.split(' ').lastOrNull { it.isNotBlank() } ?: displayName
