package org.santayn.testing.mobile.feature.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.santayn.testing.mobile.core.session.WorkspaceRole
import org.santayn.testing.mobile.core.ui.AppCard
import org.santayn.testing.mobile.core.ui.Banner
import org.santayn.testing.mobile.core.ui.BusyButton
import org.santayn.testing.mobile.core.ui.LoadState
import org.santayn.testing.mobile.core.ui.LoadingState
import org.santayn.testing.mobile.core.ui.NetworkBanner
import org.santayn.testing.mobile.core.ui.RetryBanner
import org.santayn.testing.mobile.core.ui.SectionTitle
import org.santayn.testing.mobile.core.ui.Tone
import org.santayn.testing.mobile.core.ui.design.Design
import org.santayn.testing.mobile.core.ui.design.KafedraType
import org.santayn.testing.mobile.core.ui.design.kafedraSubjectAccents
import org.santayn.testing.mobile.core.util.formatDate
import org.santayn.testing.mobile.core.util.formatNumber
import org.santayn.testing.mobile.core.util.formatTodayRu
import org.santayn.testing.mobile.navigation.AppNavigator
import org.santayn.testing.mobile.navigation.ProfileRoute
import org.santayn.testing.mobile.navigation.ResultsRoute
import org.santayn.testing.mobile.navigation.SubjectDetailsRoute
import org.santayn.testing.mobile.navigation.SubjectsRoute

/*
 * Главная в теме «Кафедра» (макет: ряд B, «Главная»):
 * дата и крупный заголовок «Сегодня», карточка последнего теста, предметы одним сгруппированным списком.
 * У преподавателя и админа под шапкой — разделы меню такими же списками.
 *
 * Срока теста, ФИО преподавателя и числа лекций из макета нет в данных главной — они не показываются
 * (recommendations/mobile-design-themes/THEME_B_KAFEDRA.md, «Требует backend»).
 */

@Composable
internal fun KafedraHome(
    role: WorkspaceRole,
    name: String,
    student: LoadState<StudentHome>,
    onRetry: () -> Unit,
    navigator: AppNavigator,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item { Header(name, onProfile = { navigator.openTab(ProfileRoute) }) }
        item { NetworkBanner(Modifier.padding(horizontal = 16.dp).clip(Design.tokens.cardShape)) }

        if (role == WorkspaceRole.STUDENT) {
            when (student) {
                LoadState.Loading -> item { LoadingState(Modifier.heightIn(max = 200.dp)) }
                is LoadState.Error -> item {
                    RetryBanner(student.message, onRetry = onRetry, modifier = Modifier.padding(horizontal = 16.dp))
                }
                is LoadState.Ready -> {
                    val data = student.data
                    item { LastAttemptCard(data.lastAttempt, navigator) }
                    item { SectionTitle("Предметы", modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 8.dp)) }
                    if (data.emptyReason != null) {
                        item { Banner(data.emptyReason, Tone.INFO, modifier = Modifier.padding(horizontal = 16.dp)) }
                    }
                    if (data.subjects.isNotEmpty()) {
                        item {
                            val accents = kafedraSubjectAccents()
                            GroupedList(
                                rows = data.subjects.map { subject ->
                                    GroupedRow(
                                        title = subject.name,
                                        description = subject.description,
                                        leading = {
                                            Box(
                                                Modifier
                                                    .size(width = 6.dp, height = 36.dp)
                                                    .clip(RoundedCornerShape(3.dp))
                                                    .background(accents[subject.id.mod(accents.size)])
                                            )
                                        },
                                        onClick = { navigator.open(SubjectDetailsRoute(subject.id, subject.facultyId)) },
                                    )
                                },
                                leadingWidth = 6.dp,
                                descriptionLines = 1,
                            )
                        }
                    }
                }
            }
        } else {
            homeSections(role).forEach { (title, entries) ->
                item { SectionTitle(title, modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 8.dp)) }
                item {
                    GroupedList(
                        rows = entries.map { entry ->
                            GroupedRow(
                                title = entry.title,
                                description = entry.description,
                                leading = {
                                    Icon(
                                        entry.icon,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(22.dp),
                                    )
                                },
                                onClick = { navigator.open(entry.route) },
                            )
                        },
                        leadingWidth = 22.dp,
                        descriptionLines = 2,
                    )
                }
            }
        }
    }
}

/** Дата, «Сегодня» и круг с инициалами — переход в профиль. */
@Composable
private fun Header(name: String, onProfile: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().statusBarsPadding().padding(start = 20.dp, end = 18.dp, top = 16.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                formatTodayRu(),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                "Сегодня",
                style = KafedraType.largeTitle,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.semantics { heading() },
            )
        }
        Spacer(Modifier.width(12.dp))
        // Круг 44dp в области касания 48dp.
        Box(
            Modifier
                .size(48.dp)
                .clip(CircleShape)
                .clickable(role = Role.Button, onClick = onProfile)
                .semantics { contentDescription = "Профиль" },
            contentAlignment = Alignment.Center,
        ) {
            Box(
                Modifier.size(44.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    initials(name),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onPrimary,
                    maxLines = 1,
                    modifier = Modifier.clearAndSetSemantics {},
                )
            }
        }
    }
}

/** Карточка «последний тест»; без попыток — «С чего начать». */
@Composable
private fun LastAttemptCard(last: LastAttempt?, navigator: AppNavigator) {
    val modifier = Modifier.padding(horizontal = 16.dp)
    if (last == null) {
        AppCard(modifier = modifier) {
            Text("С ЧЕГО НАЧАТЬ", style = KafedraType.overline.copy(fontSize = 12.sp), color = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(8.dp))
            Text(
                "Откройте предмет и пройдите первый тест",
                style = MaterialTheme.typography.titleMedium.copy(fontSize = 18.sp, lineHeight = 24.sp),
            )
            Spacer(Modifier.height(14.dp))
            BusyButton("Мои предметы", onClick = { navigator.openTab(SubjectsRoute) }, modifier = Modifier.fillMaxWidth())
        }
        return
    }
    val percent = last.percent.coerceIn(0.0, 100.0)
    AppCard(modifier = modifier, onClick = { navigator.openTab(ResultsRoute) }) {
        Text(
            "ПОСЛЕДНИЙ ТЕСТ" + (last.completedAt?.let { " · ${formatDate(it)}" } ?: ""),
            style = KafedraType.overline.copy(fontSize = 12.sp),
            color = MaterialTheme.colorScheme.primary,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            last.testName,
            style = MaterialTheme.typography.titleMedium.copy(fontSize = 18.sp, lineHeight = 24.sp),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(Modifier.height(6.dp))
        // Строка с процентом и полоса — одно сообщение для TalkBack.
        Column(Modifier.clearAndSetSemantics { contentDescription = "${formatNumber(last.percent)} процентов верно" }) {
            Text(
                "${formatNumber(last.percent)} % верно",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(10.dp))
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(MaterialTheme.colorScheme.outlineVariant)
            ) {
                Box(
                    Modifier
                        .fillMaxWidth((percent / 100.0).toFloat())
                        .height(6.dp)
                        .background(MaterialTheme.colorScheme.primary)
                )
            }
        }
    }
}

private class GroupedRow(
    val title: String,
    val description: String?,
    val leading: @Composable () -> Unit,
    val onClick: () -> Unit,
)

/**
 * Сгруппированный список: строки в одной карточке, между ними — тонкая линия от начала текста.
 * Каждая строка — один кликабельный элемент для TalkBack.
 */
@Composable
private fun GroupedList(rows: List<GroupedRow>, leadingWidth: Dp, descriptionLines: Int) {
    val tokens = Design.tokens
    Surface(
        shape = tokens.cardShape,
        color = MaterialTheme.colorScheme.surface,
        border = if (tokens.cardBorder) BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant) else null,
        modifier = Modifier.padding(horizontal = 16.dp).fillMaxWidth(),
    ) {
        Column {
            rows.forEachIndexed { index, row ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clickable(role = Role.Button, onClick = row.onClick)
                        .heightIn(min = 64.dp)
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                        .semantics(mergeDescendants = true) {},
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    row.leading()
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text(row.title, style = KafedraType.rowTitle, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        if (!row.description.isNullOrBlank()) {
                            Text(
                                row.description,
                                style = KafedraType.rowCaption,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = descriptionLines,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                    Spacer(Modifier.width(8.dp))
                    Icon(
                        Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (index < rows.lastIndex) {
                    HorizontalDivider(
                        modifier = Modifier.padding(start = 16.dp + leadingWidth + 14.dp),
                        color = MaterialTheme.colorScheme.outlineVariant,
                    )
                }
            }
        }
    }
}
