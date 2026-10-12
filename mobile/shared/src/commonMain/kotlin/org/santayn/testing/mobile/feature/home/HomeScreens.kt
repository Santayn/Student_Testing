package org.santayn.testing.mobile.feature.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import org.santayn.testing.mobile.core.ui.CollectMessages
import org.santayn.testing.mobile.core.ui.ReloadOnReconnect
import org.santayn.testing.mobile.core.ui.design.Design
import org.santayn.testing.mobile.core.ui.design.DesignTheme
import org.santayn.testing.mobile.core.platform.platformName
import org.santayn.testing.mobile.core.session.SessionManager
import org.santayn.testing.mobile.core.session.SessionState
import org.santayn.testing.mobile.core.session.WorkspaceRole
import org.santayn.testing.mobile.core.storage.AppSettings
import org.santayn.testing.mobile.core.ui.AppCard
import org.santayn.testing.mobile.core.ui.AppScreen
import org.santayn.testing.mobile.core.ui.InfoRow
import org.santayn.testing.mobile.core.ui.NavCard
import org.santayn.testing.mobile.core.ui.ScreenList
import org.santayn.testing.mobile.core.ui.screenPadding
import org.santayn.testing.mobile.core.ui.SectionTitle
import org.santayn.testing.mobile.core.ui.StatusChip
import org.santayn.testing.mobile.core.ui.Tone
import org.santayn.testing.mobile.navigation.AboutRoute
import org.santayn.testing.mobile.navigation.AppNavigator
import org.santayn.testing.mobile.navigation.MenuEntry
import org.santayn.testing.mobile.navigation.Menus

/**
 * Главная страница по рабочей роли (views/HomeView.vue).
 * Данные — [HomeViewModel] (общие для всех тем), компоновка — своя у каждой темы оформления.
 */
@Composable
fun HomeScreen(role: WorkspaceRole, navigator: AppNavigator) {
    val session = koinInject<SessionManager>()
    val state by session.state.collectAsState()
    val user = (state as? SessionState.LoggedIn)?.user
    val vm = koinViewModel<HomeViewModel>(key = "home-$role") { parametersOf(role) }
    CollectMessages(vm)
    val student by vm.student.collectAsState()
    ReloadOnReconnect { vm.load() }
    val name = greetingName(user?.person?.firstName, user?.displayName.orEmpty())

    when (Design.tokens.theme) {
        DesignTheme.POLYTECH -> PolytechHome(role, name, student, onRetry = vm::load, navigator = navigator)
        DesignTheme.KAFEDRA -> KafedraHome(role, name, student, onRetry = vm::load, navigator = navigator)
        DesignTheme.ZACHET -> ZachetHome(role, name, student, onRetry = vm::load, navigator = navigator)
    }
}

/** Экран-хаб с пунктами меню (вкладки «Контент», «Структура», «Доступ»). */
@Composable
fun MenuHubScreen(title: String, entries: List<MenuEntry>, navigator: AppNavigator) {
    AppScreen(title = title) { padding ->
        ScreenList(contentPadding = screenPadding(padding)) {
            items(entries) { entry ->
                NavCard(entry.title, description = entry.description, icon = entry.icon) { navigator.open(entry.route) }
            }
        }
    }
}

/** О системе (views/AboutView.vue). */
@Composable
fun AboutScreen(navigator: AppNavigator) {
    AppScreen(title = "О системе", onBack = navigator::back) { padding ->
        ScreenList(contentPadding = screenPadding(padding)) {
            item {
                AppCard {
                    Text("Student Testing", style = MaterialTheme.typography.titleLarge)
                    Text(
                        "Система обучения и тестирования студентов: предметы, лекции с материалами, " +
                            "тесты с разными типами вопросов и результаты.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            item { SectionTitle("Студентам") }
            item {
                AppCard {
                    Text("Изучайте лекции и материалы своих предметов, проходите тесты и смотрите результаты. " +
                        "Черновик ответов сохраняется на устройстве — можно продолжить после перезапуска.")
                }
            }
            item { SectionTitle("Преподавателям") }
            item {
                AppCard {
                    Text("Ведите темы и банк вопросов (в том числе импорт из Word), создавайте тесты и назначайте их группам, " +
                        "управляйте лекциями, материалами и шаблонами курса, смотрите результаты студентов.")
                }
            }
            item { SectionTitle("Администраторам") }
            item {
                AppCard {
                    Text("Настраивайте факультеты, группы, предметы, учебную нагрузку, пользователей, роли и права, " +
                        "делайте резервные копии базы данных.")
                }
            }
            item { SectionTitle("Приложение") }
            item {
                AppCard {
                    InfoRow("Версия", "1.0")
                    InfoRow("Платформа", platformName)
                }
            }
        }
    }
}
