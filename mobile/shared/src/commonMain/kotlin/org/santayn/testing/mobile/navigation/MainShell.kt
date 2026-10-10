package org.santayn.testing.mobile.navigation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import org.santayn.testing.mobile.core.session.WorkspaceRole
import org.santayn.testing.mobile.feature.admin.AdminBackupsScreen
import org.santayn.testing.mobile.feature.admin.AdminFacultiesScreen
import org.santayn.testing.mobile.feature.admin.AdminFacultySubjectsScreen
import org.santayn.testing.mobile.feature.admin.AdminGroupsScreen
import org.santayn.testing.mobile.feature.admin.AdminRolesScreen
import org.santayn.testing.mobile.feature.admin.AdminSubjectsScreen
import org.santayn.testing.mobile.feature.admin.AdminTeacherSubjectsScreen
import org.santayn.testing.mobile.feature.admin.AdminTeachingScreen
import org.santayn.testing.mobile.feature.admin.AdminUsersScreen
import org.santayn.testing.mobile.feature.home.AboutScreen
import org.santayn.testing.mobile.feature.home.HomeScreen
import org.santayn.testing.mobile.feature.home.MenuHubScreen
import org.santayn.testing.mobile.feature.learning.LectureScreen
import org.santayn.testing.mobile.feature.learning.SubjectDetailsScreen
import org.santayn.testing.mobile.feature.learning.SubjectLecturesScreen
import org.santayn.testing.mobile.feature.learning.SubjectsScreen
import org.santayn.testing.mobile.feature.profile.ProfileScreen
import org.santayn.testing.mobile.feature.results.ResultsScreen
import org.santayn.testing.mobile.feature.teacher.CourseTemplatesScreen
import org.santayn.testing.mobile.feature.teacher.LectureManagementScreen
import org.santayn.testing.mobile.feature.teacher.QuestionsScreen
import org.santayn.testing.mobile.feature.teacher.TestEditorScreen
import org.santayn.testing.mobile.feature.teacher.TopicsScreen
import org.santayn.testing.mobile.feature.teacher.WorkloadScreen
import org.santayn.testing.mobile.feature.tests.TestScreen

/** Навигационные действия, доступные экранам. */
class AppNavigator(private val nav: NavHostController) {
    fun open(route: Any) = nav.navigate(route) { launchSingleTop = true }
    fun back() {
        if (!nav.popBackStack()) open(HomeRoute)
    }

    /** Переход на вкладку нижней панели с сохранением состояния вкладок. */
    fun openTab(route: Any) = nav.navigate(route) {
        popUpTo(nav.graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }

    /** Заменить текущий экран (например, после завершения теста). */
    fun replace(route: Any) {
        nav.popBackStack()
        open(route)
    }
}

@Composable
fun MainShell(role: WorkspaceRole) {
    val nav = rememberNavController()
    val navigator = AppNavigator(nav)
    val tabs = bottomTabsFor(role)
    val backStackEntry by nav.currentBackStackEntryAsState()
    val destination = backStackEntry?.destination
    val onTopLevel = tabs.any { tab -> destination?.hierarchy?.any { it.hasRoute(tab.route::class) } == true }
    // При крупном шрифте подписи всех вкладок не помещаются — показываем подпись только у активной.
    val showAllLabels = LocalDensity.current.fontScale <= 1.3f

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            AnimatedVisibility(onTopLevel, enter = expandVertically(), exit = shrinkVertically()) {
                NavigationBar {
                    tabs.forEach { tab ->
                        val selected = destination?.hierarchy?.any { it.hasRoute(tab.route::class) } == true
                        NavigationBarItem(
                            selected = selected,
                            onClick = { navigator.openTab(tab.route) },
                            // Подпись скрыта (крупный шрифт) — название вкладки передаём через иконку.
                            icon = { Icon(tab.icon, contentDescription = if (showAllLabels || selected) null else tab.label) },
                            label = { Text(tab.label, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                            alwaysShowLabel = showAllLabels,
                        )
                    }
                }
            }
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(bottom = padding.calculateBottomPadding())) {
            AppNavHost(nav, navigator, role)
        }
    }
}

@Composable
private fun AppNavHost(nav: NavHostController, navigator: AppNavigator, role: WorkspaceRole) {
    NavHost(navController = nav, startDestination = HomeRoute) {
        composable<HomeRoute> { HomeScreen(role, navigator) }
        composable<ProfileRoute> { ProfileScreen(navigator) }
        composable<AboutRoute> { AboutScreen(navigator) }
        composable<ResultsRoute> { ResultsScreen(role, navigator) }

        composable<SubjectsRoute> { SubjectsScreen(role, navigator) }
        composable<SubjectDetailsRoute> { SubjectDetailsScreen(it.toRoute(), role, navigator) }
        composable<SubjectLecturesRoute> { SubjectLecturesScreen(it.toRoute(), navigator) }
        composable<LectureRoute> { LectureScreen(it.toRoute(), navigator) }
        composable<TestRoute> { TestScreen(it.toRoute(), navigator) }

        composable<TeacherContentRoute> {
            MenuHubScreen(
                title = "Учебный контент",
                entries = Menus.teacherContent + if (role == WorkspaceRole.TEACHER) listOf(Menus.teacherWorkload) else emptyList(),
                navigator = navigator,
            )
        }
        composable<TeacherTopicsRoute> { TopicsScreen(it.toRoute(), role, navigator) }
        composable<TeacherQuestionsRoute> { QuestionsScreen(it.toRoute(), role, navigator) }
        composable<TeacherTestCreateRoute> { TestEditorScreen(it.toRoute(), role, navigator) }
        composable<TeacherLecturesRoute> { LectureManagementScreen(it.toRoute(), role, navigator) }
        composable<TeacherCoursesRoute> { CourseTemplatesScreen(it.toRoute(), role, navigator) }
        composable<TeacherWorkloadRoute> { WorkloadScreen(navigator) }

        composable<AdminStructureRoute> {
            MenuHubScreen("Академическая структура", Menus.adminStructure, navigator)
        }
        composable<AdminAccessRoute> {
            MenuHubScreen("Доступ и система", Menus.adminAccess, navigator)
        }
        composable<AdminUsersRoute> { AdminUsersScreen(navigator) }
        composable<AdminRolesRoute> { AdminRolesScreen(navigator) }
        composable<AdminFacultiesRoute> { AdminFacultiesScreen(navigator) }
        composable<AdminGroupsRoute> { AdminGroupsScreen(navigator) }
        composable<AdminSubjectsRoute> { AdminSubjectsScreen(navigator) }
        composable<AdminFacultySubjectsRoute> { AdminFacultySubjectsScreen(navigator) }
        composable<AdminTeacherSubjectsRoute> { AdminTeacherSubjectsScreen(navigator) }
        composable<AdminTeachingRoute> { AdminTeachingScreen(navigator) }
        composable<AdminBackupsRoute> { AdminBackupsScreen(navigator) }
    }
}
