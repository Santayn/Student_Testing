package org.santayn.testing.mobile.feature.tests

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import org.santayn.testing.mobile.core.platform.LightStatusBarIcons
import org.santayn.testing.mobile.core.ui.design.Design
import org.santayn.testing.mobile.core.ui.design.DesignTheme
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
