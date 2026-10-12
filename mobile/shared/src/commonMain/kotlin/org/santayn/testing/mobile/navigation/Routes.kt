package org.santayn.testing.mobile.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.AccountTree
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Class
import androidx.compose.material.icons.filled.CollectionsBookmark
import androidx.compose.material.icons.filled.Domain
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LibraryBooks
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlaylistAddCheck
import androidx.compose.material.icons.filled.Quiz
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Topic
import androidx.compose.material.icons.filled.Work
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.ui.graphics.vector.ImageVector
import kotlinx.serialization.Serializable
import org.santayn.testing.mobile.core.session.WorkspaceRole

/*
 * Маршруты приложения (frontend/src/router/routes).
 */

// Общие
@Serializable data object HomeRoute
@Serializable data object ProfileRoute
@Serializable data object AboutRoute
@Serializable data object ResultsRoute

// Предметы и обучение студента
@Serializable data object SubjectsRoute
@Serializable data class SubjectDetailsRoute(val subjectId: Int, val facultyId: Int? = null)
@Serializable data class SubjectLecturesRoute(val subjectId: Int, val facultyId: Int? = null)
@Serializable data class LectureRoute(val lectureId: Int, val subjectId: Int? = null)
@Serializable data class TestRoute(
    val testId: Int,
    val assignmentId: Int,
    val lectureId: Int? = null,
    val subjectId: Int? = null,
)

// Преподаватель (и админ в режиме администратора)
@Serializable data object TeacherContentRoute
@Serializable data class TeacherTopicsRoute(val subjectId: Int? = null)
@Serializable data class TeacherQuestionsRoute(val subjectId: Int? = null, val topicId: Int? = null)
@Serializable data class TeacherTestCreateRoute(val subjectId: Int? = null, val topicId: Int? = null)
@Serializable data class TeacherLecturesRoute(val subjectId: Int? = null)
@Serializable data class TeacherCoursesRoute(val subjectId: Int? = null)
@Serializable data object TeacherWorkloadRoute

// Администратор
@Serializable data object AdminStructureRoute
@Serializable data object AdminAccessRoute
@Serializable data object AdminUsersRoute
@Serializable data object AdminRolesRoute
@Serializable data object AdminFacultiesRoute
@Serializable data object AdminGroupsRoute
@Serializable data object AdminSubjectsRoute
@Serializable data object AdminFacultySubjectsRoute
@Serializable data object AdminTeacherSubjectsRoute
@Serializable data object AdminTeachingRoute
@Serializable data object AdminBackupsRoute

/** Вкладка нижней панели. */
data class BottomTab(val route: Any, val label: String, val icon: ImageVector)

/**
 * Нижняя панель по рабочей роли (адаптация WORKSPACE_NAVIGATION из navigation.config.js).
 */
fun bottomTabsFor(role: WorkspaceRole): List<BottomTab> = when (role) {
    WorkspaceRole.STUDENT -> listOf(
        BottomTab(HomeRoute, "Главная", Icons.Default.Home),
        BottomTab(SubjectsRoute, "Предметы", Icons.AutoMirrored.Filled.MenuBook),
        BottomTab(ResultsRoute, "Результаты", Icons.Default.Assessment),
        BottomTab(ProfileRoute, "Профиль", Icons.Default.Person),
    )
    WorkspaceRole.TEACHER -> listOf(
        BottomTab(HomeRoute, "Главная", Icons.Default.Home),
        BottomTab(SubjectsRoute, "Предметы", Icons.AutoMirrored.Filled.MenuBook),
        BottomTab(TeacherContentRoute, "Контент", Icons.Default.Widgets),
        BottomTab(ResultsRoute, "Результаты", Icons.Default.Assessment),
        BottomTab(ProfileRoute, "Профиль", Icons.Default.Person),
    )
    WorkspaceRole.ADMIN -> listOf(
        BottomTab(HomeRoute, "Главная", Icons.Default.Home),
        BottomTab(AdminStructureRoute, "Структура", Icons.Default.AccountTree),
        BottomTab(TeacherContentRoute, "Контент", Icons.Default.Widgets),
        BottomTab(AdminAccessRoute, "Доступ", Icons.Default.Security),
        BottomTab(ProfileRoute, "Профиль", Icons.Default.Person),
    )
}

/** Пункт меню-хаба / главной страницы. */
data class MenuEntry(
    val title: String,
    val description: String,
    val icon: ImageVector,
    val route: Any,
)

object Menus {
    val teacherContent = listOf(
        MenuEntry("Темы предмета", "Библиотека тем выбранного предмета", Icons.Default.Topic, TeacherTopicsRoute()),
        MenuEntry("Вопросы", "Банк вопросов, импорт из Word", Icons.Default.Quiz, TeacherQuestionsRoute()),
        MenuEntry("Создать тест", "Мастер создания теста и назначения группам", Icons.Default.PlaylistAddCheck, TeacherTestCreateRoute()),
        MenuEntry("Лекции", "Лекции, материалы и привязка тестов", Icons.Default.LibraryBooks, TeacherLecturesRoute()),
        MenuEntry("Шаблоны курса", "Шаблоны и версии курса", Icons.Default.CollectionsBookmark, TeacherCoursesRoute()),
    )

    val teacherWorkload = MenuEntry("Моя нагрузка", "Назначенная учебная нагрузка", Icons.Default.Work, TeacherWorkloadRoute)

    val adminStructure = listOf(
        MenuEntry("Факультеты", "Справочник факультетов", Icons.Default.Domain, AdminFacultiesRoute),
        MenuEntry("Группы", "Группы и их студенты", Icons.Default.Groups, AdminGroupsRoute),
        MenuEntry("Справочник предметов", "Каталог учебных предметов", Icons.AutoMirrored.Filled.MenuBook, AdminSubjectsRoute),
        MenuEntry("Предметы факультетов", "Какие предметы читаются на факультете", Icons.Default.Class, AdminFacultySubjectsRoute),
        MenuEntry("Преподаватели и предметы", "Закрепление преподавателей за предметами", Icons.Default.School, AdminTeacherSubjectsRoute),
        MenuEntry("Учебная нагрузка", "Назначения преподавателей группам", Icons.Default.Work, AdminTeachingRoute),
    )

    val adminAccess = listOf(
        MenuEntry("Пользователи", "Учётные записи, персоны и роли", Icons.Default.Badge, AdminUsersRoute),
        MenuEntry("Роли и права", "Роли и набор прав", Icons.Default.Security, AdminRolesRoute),
        MenuEntry("Резервные копии", "Выгрузка и восстановление базы данных", Icons.Default.Backup, AdminBackupsRoute),
    )

    val results = MenuEntry("Результаты", "Результаты прохождения тестов", Icons.Default.Assessment, ResultsRoute)
    val subjects = MenuEntry("Предметы", "Учебные предметы", Icons.AutoMirrored.Filled.MenuBook, SubjectsRoute)
    val profile = MenuEntry("Профиль", "Учётная запись и смена пароля", Icons.Default.Person, ProfileRoute)
}
