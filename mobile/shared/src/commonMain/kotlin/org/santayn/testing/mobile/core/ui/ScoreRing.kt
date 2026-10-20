package org.santayn.testing.mobile.core.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import org.santayn.testing.mobile.core.util.formatNumber

/**
 * Кольцо результата с подписью в центре. Для TalkBack — одна подпись «Результат N процентов».
 *
 * @param size диаметр кольца.
 * @param stroke толщина линии.
 */
@Composable
fun ScoreRing(
    percent: Double,
    track: Color,
    progress: Color,
    label: String,
    textColor: Color,
    modifier: Modifier = Modifier,
    size: Dp = 140.dp,
    stroke: Dp = 14.dp,
    labelStyle: TextStyle = MaterialTheme.typography.headlineSmall,
) {
    Box(
        modifier.size(size).clearAndSetSemantics { contentDescription = "Результат ${formatNumber(percent)} процентов" },
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.size(size)) {
            val strokePx = stroke.toPx()
            val inset = strokePx / 2
            val arcSize = Size(this.size.width - strokePx, this.size.height - strokePx)
            drawArc(track, startAngle = 0f, sweepAngle = 360f, useCenter = false, topLeft = Offset(inset, inset), size = arcSize, style = Stroke(strokePx))
            drawArc(
                progress,
                startAngle = -90f,
                sweepAngle = (percent / 100.0 * 360.0).toFloat(),
                useCenter = false,
                topLeft = Offset(inset, inset),
                size = arcSize,
                style = Stroke(strokePx, cap = StrokeCap.Round),
            )
        }
        Text(label, style = labelStyle, color = textColor)
    }
}
