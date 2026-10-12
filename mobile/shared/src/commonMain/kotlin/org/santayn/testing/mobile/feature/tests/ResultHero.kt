package org.santayn.testing.mobile.feature.tests

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.santayn.testing.mobile.core.platform.LightStatusBarIcons
import org.santayn.testing.mobile.core.ui.AppCard
import org.santayn.testing.mobile.core.ui.AppIconButton
import org.santayn.testing.mobile.core.ui.AppTheme
import org.santayn.testing.mobile.core.ui.design.Design
import org.santayn.testing.mobile.core.ui.design.DesignTheme
import org.santayn.testing.mobile.core.ui.design.KafedraType
import org.santayn.testing.mobile.core.util.formatNumber
import org.santayn.testing.mobile.data.model.SubmitAttemptResponse

/** Доля верных ответов, 0..100. */
internal fun SubmitAttemptResponse.percent(): Double =
    if (totalCount == 0) 0.0 else correctCount * 100.0 / totalCount

/**
 * Шапка итога теста. У каждой темы оформления своя (макеты, столбец «Результат»);
 * разбор ответов под ней общий (TestResultScreen).
 */
@Composable
internal fun ResultHero(title: String, result: SubmitAttemptResponse, onBack: () -> Unit) {
    when (Design.tokens.theme) {
        DesignTheme.POLYTECH -> PolytechResultHero(title, result, onBack)
        DesignTheme.KAFEDRA -> KafedraResultHero(title, result, onBack)
    }
}

/** «Политех»: синяя панель, кольцо процента зелёным, чипы с баллами. */
@Composable
private fun PolytechResultHero(title: String, result: SubmitAttemptResponse, onBack: () -> Unit) {
    LightStatusBarIcons()
    val brand = Design.brand
    val percent = result.percent()
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp))
            .background(brand.hero)
            .statusBarsPadding()
            .padding(start = 8.dp, end = 20.dp, top = 4.dp, bottom = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            IconButton(
                onClick = onBack,
                colors = IconButtonDefaults.iconButtonColors(containerColor = brand.heroChip, contentColor = brand.onHero),
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "К лекции")
            }
            Text(
                title,
                style = MaterialTheme.typography.titleSmall,
                color = brand.onHeroMuted,
                maxLines = 1,
                modifier = Modifier.weight(1f).padding(start = 8.dp),
            )
        }
        ScoreRing(
            percent = percent,
            track = brand.heroChip,
            progress = brand.accent,
            label = "${formatNumber(percent)} %",
            textColor = brand.onHero,
        )
        Text(
            "${result.correctCount} из ${result.totalCount} верно",
            style = MaterialTheme.typography.headlineSmall,
            color = brand.onHero,
            textAlign = TextAlign.Center,
            modifier = Modifier.semantics {
                heading()
                liveRegion = LiveRegionMode.Polite
            },
        )
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)) {
            result.score?.let { HeroResultChip("Баллы: ${formatNumber(it)}") }
            HeroResultChip("Тест завершён")
        }
    }
}

/** «Кафедра»: без цветной панели — крупный заголовок на фоне и карточка с числом, полосой по вопросам и итогами. */
@Composable
private fun KafedraResultHero(title: String, result: SubmitAttemptResponse, onBack: () -> Unit) {
    val wrongCount = (result.totalCount - result.correctCount).coerceAtLeast(0)
    Column(Modifier.fillMaxWidth().statusBarsPadding().padding(top = 4.dp)) {
        AppIconButton(Icons.AutoMirrored.Filled.ArrowBack, "К лекции", onClick = onBack)
        Column(Modifier.padding(start = 20.dp, end = 20.dp, top = 8.dp)) {
            Text(
                title,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                "Тест завершён",
                style = KafedraType.largeTitle,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.semantics {
                    heading()
                    liveRegion = LiveRegionMode.Polite
                },
            )
        }
        AppCard(modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp)) {
            Column(Modifier.padding(4.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                // «2/4 правильных ответов» — одно сообщение для TalkBack.
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.clearAndSetSemantics {
                        contentDescription = "${result.correctCount} из ${result.totalCount} правильных ответов"
                    },
                ) {
                    val big = MaterialTheme.typography.displayMedium.copy(
                        fontSize = 56.sp, lineHeight = 60.sp, fontWeight = FontWeight.Bold,
                    )
                    Row(Modifier.alignByBaseline()) {
                        Text(result.correctCount.toString(), style = big, modifier = Modifier.alignByBaseline())
                        Text(
                            "/${result.totalCount}",
                            style = big,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.alignByBaseline(),
                        )
                    }
                    Text(
                        "правильных ответов",
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.alignByBaseline(),
                    )
                }
                QuestionSegments(result, wrongCount)
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    ResultFigure("Баллы", formatNumber(result.score), Modifier.weight(1f))
                    ResultFigure("Верно", "${formatNumber(result.percent())} %", Modifier.weight(1f))
                }
            }
        }
    }
}

/**
 * Полоса по вопросам: сегмент на вопрос. Порядок — как в разборе ответов; если разбора нет,
 * сначала верные, затем неверные (только соотношение).
 */
@Composable
private fun QuestionSegments(result: SubmitAttemptResponse, wrongCount: Int) {
    val flags = if (result.details.isNotEmpty()) {
        result.details.map { it.correct }
    } else {
        List(result.correctCount) { true } + List(wrongCount) { false }
    }
    if (flags.isEmpty()) return
    // Яркие success/danger на белом ниже 3:1 — берём «текстовые» оттенки.
    val right = AppTheme.status.successText
    val wrong = AppTheme.status.dangerText
    Row(
        Modifier.fillMaxWidth().clearAndSetSemantics {
            contentDescription = "${result.correctCount} верно, $wrongCount неверно"
        },
        horizontalArrangement = Arrangement.spacedBy(if (flags.size > 40) 2.dp else 4.dp),
    ) {
        flags.forEach { correct ->
            Box(Modifier.weight(1f).height(8.dp).clip(RoundedCornerShape(2.dp)).background(if (correct) right else wrong))
        }
    }
}

@Composable
private fun ResultFigure(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier.semantics(mergeDescendants = true) {}) {
        Text(label, style = KafedraType.rowCaption, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.titleLarge)
    }
}

/** Кольцо результата. Для TalkBack — одна подпись «Результат N процентов». */
@Composable
private fun ScoreRing(percent: Double, track: Color, progress: Color, label: String, textColor: Color) {
    Box(
        Modifier.size(140.dp).clearAndSetSemantics { contentDescription = "Результат ${formatNumber(percent)} процентов" },
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.size(140.dp)) {
            val stroke = 14.dp.toPx()
            val inset = stroke / 2
            val arcSize = Size(size.width - stroke, size.height - stroke)
            drawArc(track, startAngle = 0f, sweepAngle = 360f, useCenter = false, topLeft = Offset(inset, inset), size = arcSize, style = Stroke(stroke))
            drawArc(
                progress,
                startAngle = -90f,
                sweepAngle = (percent / 100.0 * 360.0).toFloat(),
                useCenter = false,
                topLeft = Offset(inset, inset),
                size = arcSize,
                style = Stroke(stroke, cap = StrokeCap.Round),
            )
        }
        Text(label, style = MaterialTheme.typography.headlineSmall, color = textColor)
    }
}

@Composable
private fun HeroResultChip(text: String) {
    Surface(color = Design.brand.heroChip, contentColor = Design.brand.onHero, shape = Design.tokens.chipShape) {
        Text(text, style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp))
    }
}
