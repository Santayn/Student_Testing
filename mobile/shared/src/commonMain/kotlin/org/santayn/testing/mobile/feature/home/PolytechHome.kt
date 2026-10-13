package org.santayn.testing.mobile.feature.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.santayn.testing.mobile.core.platform.LightStatusBarIcons
import org.santayn.testing.mobile.core.session.WorkspaceRole
import org.santayn.testing.mobile.core.ui.AppCard
import org.santayn.testing.mobile.core.ui.AppTheme
import org.santayn.testing.mobile.core.ui.BusyButton
import org.santayn.testing.mobile.core.ui.LoadState
import org.santayn.testing.mobile.core.ui.LoadingState
import org.santayn.testing.mobile.core.ui.NavCard
import org.santayn.testing.mobile.core.ui.NetworkBanner
import org.santayn.testing.mobile.core.ui.RetryBanner
import org.santayn.testing.mobile.core.ui.SectionTitle
import org.santayn.testing.mobile.core.ui.Banner
import org.santayn.testing.mobile.core.ui.Tone
import org.santayn.testing.mobile.core.ui.design.Design
import org.santayn.testing.mobile.core.util.formatDate
import org.santayn.testing.mobile.core.util.formatNumber
import org.santayn.testing.mobile.navigation.AboutRoute
import org.santayn.testing.mobile.navigation.AdminStructureRoute
import org.santayn.testing.mobile.navigation.AppNavigator
import org.santayn.testing.mobile.navigation.ResultsRoute
import org.santayn.testing.mobile.navigation.SubjectDetailsRoute
import org.santayn.testing.mobile.navigation.SubjectsRoute
import org.santayn.testing.mobile.navigation.TeacherContentRoute

/*
 * Главная в теме «Политех» (макет: ряд A, «Главная»):
 * синяя панель с приветствием и карточкой «последний тест», плитки-бенто, список предметов.
 * У преподавателя и админа вместо плиток — разделы меню.
 */

@Composable
internal fun PolytechHome(
    role: WorkspaceRole,
    name: String,
    student: LoadState<StudentHome>,
    onRetry: () -> Unit,
    navigator: AppNavigator,
) {
    val ready = (student as? LoadState.Ready)?.data
    LightStatusBarIcons()
    LazyColumn(
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            HeroPanel(role, name, ready, navigator)
        }
        item { NetworkBanner(Modifier.padding(horizontal = 16.dp).clip(Design.tokens.cardShape)) }

        if (role == WorkspaceRole.STUDENT) {
            when (student) {
                LoadState.Loading -> item { LoadingState(Modifier.heightIn(max = 200.dp)) }
                is LoadState.Error -> item {
                    RetryBanner(student.message, onRetry = onRetry, modifier = Modifier.padding(horizontal = 20.dp))
                }
                is LoadState.Ready -> {
                    val data = student.data
                    item {
                        Row(Modifier.padding(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            BentoTile(
                                icon = Icons.AutoMirrored.Filled.MenuBook,
                                value = data.subjects.size.toString(),
                                label = subjectsWord(data.subjects.size),
                                container = Design.brand.accent,
                                content = Design.brand.onAccent,
                                onClick = { navigator.openTab(SubjectsRoute) },
                                modifier = Modifier.weight(1f),
                            )
                            BentoTile(
                                icon = Icons.Default.Assessment,
                                value = data.lastAttempt?.let { "${formatNumber(it.percent)} %" } ?: "—",
                                label = "последний тест",
                                container = Design.brand.highlight,
                                content = Design.brand.onHighlight,
                                onClick = { navigator.openTab(ResultsRoute) },
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                    item {
                        SectionTitle(
                            "Мои предметы",
                            modifier = Modifier.padding(horizontal = 20.dp),
                            action = { TextButton(onClick = { navigator.openTab(SubjectsRoute) }) { Text("Все") } },
                        )
                    }
                    if (data.emptyReason != null) {
                        item { Banner(data.emptyReason, Tone.INFO, modifier = Modifier.padding(horizontal = 20.dp)) }
                    }
                    items(data.subjects.take(4), key = { it.id }) { subject ->
                        SubjectRow(
                            name = subject.name,
                            description = subject.description,
                            onClick = { navigator.open(SubjectDetailsRoute(subject.id, subject.facultyId)) },
                        )
                    }
                }
            }
        } else {
            homeSections(role).forEach { (title, entries) ->
                item { SectionTitle(title, modifier = Modifier.padding(horizontal = 20.dp)) }
                items(entries) { entry ->
                    NavCard(
                        title = entry.title,
                        description = entry.description,
                        icon = entry.icon,
                        modifier = Modifier.padding(horizontal = 20.dp),
                        onClick = { navigator.open(entry.route) },
                    )
                }
            }
        }
    }
}

@Composable
private fun HeroPanel(role: WorkspaceRole, name: String, data: StudentHome?, navigator: AppNavigator) {
    val brand = Design.brand
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp))
            .background(brand.hero)
            .statusBarsPadding()
            .padding(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            // Знак приложения — заглушка до утверждения логотипа вузом.
            Box(
                Modifier.size(40.dp).clip(RoundedCornerShape(12.dp)).background(Color.White),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Default.School, contentDescription = null, tint = brand.hero)
            }
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text("КубГТУ", style = MaterialTheme.typography.titleSmall, color = brand.onHero)
                Text("Обучение", style = MaterialTheme.typography.bodySmall, color = brand.onHeroMuted)
            }
            IconButton(
                onClick = { navigator.open(AboutRoute) },
                colors = IconButtonDefaults.iconButtonColors(containerColor = brand.heroChip, contentColor = brand.onHero),
            ) {
                Icon(Icons.AutoMirrored.Filled.HelpOutline, contentDescription = "О системе")
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                "Привет, $name",
                style = MaterialTheme.typography.headlineSmall.copy(fontSize = MaterialTheme.typography.headlineMedium.fontSize),
                color = brand.onHero,
                modifier = Modifier.semantics { heading() },
            )
            Text(
                data?.contextLine ?: role.title,
                style = MaterialTheme.typography.bodyLarge,
                color = brand.onHeroMuted,
            )
        }
        HeroCard(role, data, navigator)
    }
}

/** Белая карточка внутри панели: что делать дальше. */
@Composable
private fun HeroCard(role: WorkspaceRole, data: StudentHome?, navigator: AppNavigator) {
    AppCard {
        when (role) {
            WorkspaceRole.STUDENT -> {
                val last = data?.lastAttempt
                if (last != null) {
                    Text(
                        "Последний тест" + (last.completedAt?.let { " · ${formatDate(it)}" } ?: ""),
                        style = MaterialTheme.typography.labelLarge,
                        color = AppTheme.status.primarySoftText,
                    )
                    Spacer(Modifier.size(6.dp))
                    Text(last.testName, style = MaterialTheme.typography.titleMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    Spacer(Modifier.size(12.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "${formatNumber(last.percent)} % верно",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.weight(1f),
                        )
                        BusyButton("Результаты", onClick = { navigator.openTab(ResultsRoute) })
                    }
                } else {
                    Text("С чего начать", style = MaterialTheme.typography.labelLarge, color = AppTheme.status.primarySoftText)
                    Spacer(Modifier.size(6.dp))
                    Text("Откройте предмет, посмотрите лекцию и пройдите тест", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.size(12.dp))
                    BusyButton("Мои предметы", onClick = { navigator.openTab(SubjectsRoute) }, modifier = Modifier.fillMaxWidth())
                }
            }
            WorkspaceRole.TEACHER -> {
                Text("Учебный контент", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.size(4.dp))
                Text(
                    "Темы, вопросы, лекции и тесты ваших предметов",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.size(12.dp))
                BusyButton("Открыть", onClick = { navigator.openTab(TeacherContentRoute) }, modifier = Modifier.fillMaxWidth())
            }
            WorkspaceRole.ADMIN -> {
                Text("Академическая структура", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.size(4.dp))
                Text(
                    "Факультеты, группы, предметы и учебная нагрузка",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.size(12.dp))
                BusyButton("Открыть", onClick = { navigator.openTab(AdminStructureRoute) }, modifier = Modifier.fillMaxWidth())
            }
        }
    }
}

/** Плитка-бенто: крупное число и подпись. Вся плитка — одна кнопка для TalkBack. */
@Composable
private fun BentoTile(
    icon: ImageVector,
    value: String,
    label: String,
    container: Color,
    content: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = onClick,
        shape = Design.tokens.cardShape,
        color = container,
        contentColor = content,
        modifier = modifier.heightIn(min = 116.dp),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(26.dp))
            Column {
                Text(value, style = MaterialTheme.typography.headlineSmall)
                Text(label, style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}

@Composable
private fun SubjectRow(name: String, description: String?, onClick: () -> Unit) {
    AppCard(onClick = onClick, modifier = Modifier.padding(horizontal = 20.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(44.dp).clip(Design.tokens.tileShape).background(AppTheme.status.primarySoft),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    initials(name),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = AppTheme.status.primarySoftText,
                )
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(name, style = MaterialTheme.typography.titleMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
                if (!description.isNullOrBlank()) {
                    Text(
                        description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

private fun subjectsWord(count: Int): String {
    val mod100 = count % 100
    val mod10 = count % 10
    return when {
        mod100 in 11..14 -> "предметов"
        mod10 == 1 -> "предмет"
        mod10 in 2..4 -> "предмета"
        else -> "предметов"
    }
}
