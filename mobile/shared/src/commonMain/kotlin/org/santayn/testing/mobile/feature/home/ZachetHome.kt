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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.santayn.testing.mobile.core.session.WorkspaceRole
import org.santayn.testing.mobile.core.ui.AppCard
import org.santayn.testing.mobile.core.ui.AppTheme
import org.santayn.testing.mobile.core.ui.Banner
import org.santayn.testing.mobile.core.ui.BusyButton
import org.santayn.testing.mobile.core.ui.LoadState
import org.santayn.testing.mobile.core.ui.LoadingState
import org.santayn.testing.mobile.core.ui.NavCard
import org.santayn.testing.mobile.core.ui.NetworkBanner
import org.santayn.testing.mobile.core.ui.RetryBanner
import org.santayn.testing.mobile.core.ui.ScoreRing
import org.santayn.testing.mobile.core.ui.SectionTitle
import org.santayn.testing.mobile.core.ui.Tone
import org.santayn.testing.mobile.core.ui.design.Design
import org.santayn.testing.mobile.core.ui.design.ZachetStreakIcon
import org.santayn.testing.mobile.core.ui.design.zachetStreakTextColor
import org.santayn.testing.mobile.core.util.formatDate
import org.santayn.testing.mobile.core.util.formatNumber
import org.santayn.testing.mobile.navigation.AppNavigator
import org.santayn.testing.mobile.navigation.ResultsRoute
import org.santayn.testing.mobile.navigation.SubjectDetailsRoute
import org.santayn.testing.mobile.navigation.SubjectsRoute

/*
 * Главная в теме «Зачёт» (макет: ряд C, «Главная»):
 * шапка с инициалами, группой, именем и серией дней, карточка последнего теста с кольцом результата,
 * предметы «объёмными» карточками. У преподавателя и админа под шапкой — разделы меню.
 *
 * Прогресса по предмету, сроков заданий и блокировки предметов из макета нет в API — они не показываются
 * (recommendations/mobile-design-themes/THEME_C_ZACHET.md, «Требует backend»).
 */

@Composable
internal fun ZachetHome(
    role: WorkspaceRole,
    name: String,
    student: LoadState<StudentHome>,
    onRetry: () -> Unit,
    navigator: AppNavigator,
) {
    val ready = (student as? LoadState.Ready)?.data
    LazyColumn(
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            Header(
                name = name,
                caption = ready?.contextLine?.substringBefore(" · ") ?: role.title,
                streakDays = if (role == WorkspaceRole.STUDENT) ready?.streakDays ?: 0 else 0,
            )
        }
        item { NetworkBanner(Modifier.padding(horizontal = 20.dp).clip(Design.tokens.cardShape)) }

        if (role == WorkspaceRole.STUDENT) {
            when (student) {
                LoadState.Loading -> item { LoadingState(Modifier.heightIn(max = 200.dp)) }
                is LoadState.Error -> item {
                    RetryBanner(student.message, onRetry = onRetry, modifier = Modifier.padding(horizontal = 20.dp))
                }
                is LoadState.Ready -> {
                    val data = student.data
                    item { LastAttemptCard(data.lastAttempt, navigator) }
                    item { SectionTitle("Мои предметы", modifier = Modifier.padding(horizontal = 20.dp)) }
                    if (data.emptyReason != null) {
                        item { Banner(data.emptyReason, Tone.INFO, modifier = Modifier.padding(horizontal = 20.dp)) }
                    }
                    itemsIndexed(data.subjects, key = { _, subject -> subject.id }) { index, subject ->
                        SubjectCard(
                            name = subject.name,
                            description = subject.description,
                            even = index % 2 == 0,
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

/** Квадрат с инициалами, группа и имя; справа — серия дней (если она есть). */
@Composable
private fun Header(name: String, caption: String, streakDays: Int) {
    val brand = Design.brand
    Row(
        Modifier.fillMaxWidth().statusBarsPadding().padding(start = 20.dp, end = 20.dp, top = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.size(48.dp).clip(RoundedCornerShape(16.dp)).background(brand.highlight).clearAndSetSemantics {},
            contentAlignment = Alignment.Center,
        ) {
            Text(
                initials(name),
                style = MaterialTheme.typography.titleMedium.copy(fontSize = 18.sp, fontWeight = FontWeight.Bold),
                color = brand.onHighlight,
                maxLines = 1,
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                caption,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                name,
                style = MaterialTheme.typography.headlineSmall.copy(fontSize = 22.sp, lineHeight = 28.sp, fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.semantics { heading() },
            )
        }
        if (streakDays >= 1) {
            Spacer(Modifier.width(12.dp))
            Surface(shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.surface) {
                Row(
                    Modifier.heightIn(min = 40.dp).padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        Icons.Default.LocalFireDepartment,
                        contentDescription = null,
                        tint = ZachetStreakIcon,
                        modifier = Modifier.size(20.dp),
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        streakLabel(streakDays),
                        style = MaterialTheme.typography.labelLarge.copy(fontSize = 15.sp, fontWeight = FontWeight.SemiBold),
                        color = zachetStreakTextColor(),
                    )
                }
            }
        }
    }
}

/** Карточка последнего теста с кольцом результата; без попыток — «Начни с первого предмета». */
@Composable
private fun LastAttemptCard(last: LastAttempt?, navigator: AppNavigator) {
    val modifier = Modifier.padding(horizontal = 20.dp)
    if (last == null) {
        AppCard(modifier = modifier) {
            Text(
                "Начни с первого предмета",
                style = MaterialTheme.typography.titleMedium.copy(fontSize = 19.sp, lineHeight = 25.sp, fontWeight = FontWeight.Bold),
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "Открой предмет, посмотри лекцию и пройди тест",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(14.dp))
            BusyButton("Мои предметы", onClick = { navigator.openTab(SubjectsRoute) }, accent = true, modifier = Modifier.fillMaxWidth())
        }
        return
    }
    val percent = last.percent.coerceIn(0.0, 100.0)
    AppCard(modifier = modifier, onClick = { navigator.openTab(ResultsRoute) }) {
        // Вся карточка — одно сообщение для TalkBack.
        Row(
            Modifier.clearAndSetSemantics {
                contentDescription = "Последний тест: ${last.testName}, ${formatNumber(last.percent)} процентов верно"
            },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ScoreRing(
                percent = percent,
                track = MaterialTheme.colorScheme.outlineVariant,
                progress = Design.brand.accentOnSurface,
                label = "${formatNumber(last.percent)}%",
                textColor = MaterialTheme.colorScheme.onSurface,
                size = 76.dp,
                stroke = 8.dp,
                labelStyle = MaterialTheme.typography.titleMedium.copy(fontSize = 17.sp, fontWeight = FontWeight.Bold),
            )
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    "ПОСЛЕДНИЙ ТЕСТ",
                    style = MaterialTheme.typography.labelLarge.copy(fontSize = 13.sp, fontWeight = FontWeight.SemiBold),
                    color = AppTheme.status.successText,
                )
                Text(
                    last.testName,
                    style = MaterialTheme.typography.titleMedium.copy(fontSize = 19.sp, lineHeight = 25.sp, fontWeight = FontWeight.Bold),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    "${formatNumber(last.percent)} % верно" + (last.completedAt?.let { " · ${formatDate(it)}" } ?: ""),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

/** Предмет «объёмной» карточкой: плитка с инициалами (цвета чередуются), название и описание. */
@Composable
private fun SubjectCard(name: String, description: String?, even: Boolean, onClick: () -> Unit) {
    val brand = Design.brand
    AppCard(onClick = onClick, modifier = Modifier.padding(horizontal = 20.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(44.dp).clip(Design.tokens.tileShape).background(if (even) brand.accent else brand.highlight),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    initials(name),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = if (even) brand.onAccent else brand.onHighlight,
                    modifier = Modifier.clearAndSetSemantics {},
                )
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    name,
                    style = MaterialTheme.typography.titleMedium.copy(fontSize = 16.sp, fontWeight = FontWeight.SemiBold),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                if (!description.isNullOrBlank()) {
                    Text(
                        description,
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}
