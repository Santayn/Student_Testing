package org.santayn.testing.mobile.navigation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.santayn.testing.mobile.core.ui.design.Design
import org.santayn.testing.mobile.core.ui.design.NavBarStyle
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
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
import org.santayn.testing.mobile.feature.teacher.ReviewScreen
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
    val selectedIndex = tabs.indexOfFirst { tab -> destination?.hierarchy?.any { it.hasRoute(tab.route::class) } == true }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            AnimatedVisibility(onTopLevel, enter = expandVertically(), exit = shrinkVertically()) {
                AppNavigationBar(tabs, selectedIndex, onSelect = { navigator.openTab(it.route) })
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
        composable<TeacherReviewRoute> { ReviewScreen(navigator) }

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

/** Нижняя навигация главных вкладок в стиле темы (DesignTokens.navBar). */
@Composable
internal fun AppNavigationBar(tabs: List<BottomTab>, selectedIndex: Int, onSelect: (BottomTab) -> Unit) {
    // При крупном шрифте подписи не помещаются — показываем подпись только у активной вкладки.
    val roomyText = LocalDensity.current.fontScale <= 1.3f
    when (Design.tokens.navBar) {
        NavBarStyle.CLASSIC -> NavigationBar {
            tabs.forEachIndexed { index, tab ->
                val selected = index == selectedIndex
                NavigationBarItem(
                    selected = selected,
                    onClick = { onSelect(tab) },
                    // Подпись скрыта — название вкладки передаём через иконку.
                    icon = { Icon(tab.icon, contentDescription = if (roomyText || selected) null else tab.label) },
                    label = { Text(tab.label, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                    alwaysShowLabel = roomyText,
                )
            }
        }
        NavBarStyle.FLOATING_PILL -> FloatingPillBar(tabs, selectedIndex, onSelect, showSelectedLabel = true)
        NavBarStyle.FLOATING_ICONS -> FloatingIconsBar(tabs, selectedIndex, onSelect)
        NavBarStyle.TAB_BAR -> Column {
            // Подпись растёт со шрифтом только до 13dp: иначе «Результаты» не помещается в пятую часть экрана.
            val labelSize = with(LocalDensity.current) { minOf(11.sp.toDp(), 13.dp).toSp() }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            NavigationBar(containerColor = MaterialTheme.colorScheme.surface, tonalElevation = 0.dp) {
                tabs.forEachIndexed { index, tab ->
                    val selected = index == selectedIndex
                    NavigationBarItem(
                        selected = selected,
                        onClick = { onSelect(tab) },
                        // Подпись скрыта — название вкладки передаём через иконку.
                        icon = { Icon(tab.icon, contentDescription = if (roomyText || selected) null else tab.label) },
                        // Активная вкладка — цветом и жирностью: одного цвета для состояния мало.
                        label = {
                            Text(
                                tab.label,
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = labelSize,
                                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        },
                        alwaysShowLabel = roomyText,
                        colors = NavigationBarItemDefaults.colors(
                            indicatorColor = Color.Transparent,
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        ),
                    )
                }
            }
        }
    }
}

/**
 * Плавающая панель из иконок (тема «Зачёт»): активная вкладка — скруглённый квадрат цвета highlight.
 * Подписей на экране нет, поэтому у каждой вкладки есть название для TalkBack.
 */
@Composable
private fun FloatingIconsBar(tabs: List<BottomTab>, selectedIndex: Int, onSelect: (BottomTab) -> Unit) {
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surface,
        border = if (Design.tokens.cardBorder) BorderStroke(1.dp, MaterialTheme.colorScheme.outline) else null,
        modifier = Modifier
            .navigationBarsPadding()
            .padding(start = 16.dp, end = 16.dp, bottom = 12.dp, top = 4.dp)
            .fillMaxWidth()
            .heightIn(min = 68.dp),
    ) {
        Row(
            Modifier.selectableGroup().padding(horizontal = 8.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            tabs.forEachIndexed { index, tab ->
                val selected = index == selectedIndex
                Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    Box(
                        Modifier
                            .size(width = 56.dp, height = 48.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(if (selected) Design.brand.highlight else Color.Transparent)
                            .selectable(selected = selected, role = Role.Tab, onClick = { onSelect(tab) })
                            .semantics(mergeDescendants = true) { contentDescription = tab.label },
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            tab.icon,
                            contentDescription = null,
                            tint = if (selected) Design.brand.onHighlight else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(24.dp),
                        )
                    }
                }
            }
        }
    }
}

/**
 * Плавающая панель-«пилюля» (тема «Политех»): активная вкладка — цветная капсула с иконкой и подписью,
 * остальные — иконки. Каждая вкладка — один элемент с ролью «вкладка» и названием для TalkBack.
 */
@Composable
private fun FloatingPillBar(
    tabs: List<BottomTab>,
    selectedIndex: Int,
    onSelect: (BottomTab) -> Unit,
    showSelectedLabel: Boolean,
) {
    Surface(
        shape = RoundedCornerShape(percent = 50),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 10.dp,
        border = if (Design.tokens.cardBorder) BorderStroke(1.dp, MaterialTheme.colorScheme.outline) else null,
        modifier = Modifier
            .navigationBarsPadding()
            .padding(start = 16.dp, end = 16.dp, bottom = 12.dp, top = 4.dp)
            .fillMaxWidth()
            .heightIn(min = 64.dp),
    ) {
        Row(
            Modifier.selectableGroup().padding(horizontal = 8.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            tabs.forEachIndexed { index, tab ->
                val selected = index == selectedIndex
                val content = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                Row(
                    Modifier
                        .clip(RoundedCornerShape(percent = 50))
                        .background(if (selected) MaterialTheme.colorScheme.primary else Color.Transparent)
                        .selectable(selected = selected, role = Role.Tab, onClick = { onSelect(tab) })
                        .heightIn(min = 48.dp)
                        .widthIn(min = 48.dp)
                        .padding(horizontal = if (selected && showSelectedLabel) 16.dp else 12.dp)
                        .semantics(mergeDescendants = true) { contentDescription = tab.label },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                ) {
                    Icon(tab.icon, contentDescription = null, tint = content, modifier = Modifier.size(22.dp))
                    if (selected && showSelectedLabel) {
                        Spacer(Modifier.width(8.dp))
                        Text(
                            tab.label,
                            style = MaterialTheme.typography.labelLarge,
                            color = content,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }
    }
}
