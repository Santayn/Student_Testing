package org.santayn.testing.mobile.feature.learning

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import org.santayn.testing.mobile.core.ui.SpeakPillButton
import org.santayn.testing.mobile.core.ui.design.Design
import org.santayn.testing.mobile.core.ui.design.DesignTheme
import org.santayn.testing.mobile.data.model.PublicLectureDto

/**
 * Шапка экрана лекции: номер, название, описание и «Прочитать вслух».
 * У каждой темы оформления своя компоновка (макеты, столбец «Лекция»).
 */
@Composable
internal fun LectureHero(lecture: PublicLectureDto, modifier: Modifier = Modifier) {
    when (Design.tokens.theme) {
        DesignTheme.POLYTECH -> PolytechLectureHero(lecture, modifier)
    }
}

/** «Политех»: синяя карточка с чипами, крупный заголовок, белая кнопка-«пилюля». */
@Composable
private fun PolytechLectureHero(lecture: PublicLectureDto, modifier: Modifier) {
    val brand = Design.brand
    val description = lecture.description?.takeIf { it.isNotBlank() }
    Surface(
        color = brand.hero,
        contentColor = brand.onHero,
        shape = Design.tokens.cardShape,
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                HeroChip("Лекция ${lecture.ordinal}")
                if (lecture.versionNumber > 0) HeroChip("Версия ${lecture.versionNumber}")
            }
            Text(
                lecture.title,
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.semantics { heading() },
            )
            Text(
                description ?: "Описание лекции не указано.",
                style = MaterialTheme.typography.bodyLarge,
                color = brand.onHeroMuted,
            )
            if (description != null) {
                SpeakPillButton(
                    key = "lecture-${lecture.id}",
                    text = { "${lecture.title}. $description" },
                    container = MaterialTheme.colorScheme.surface,
                    content = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}

@Composable
private fun HeroChip(text: String) {
    Surface(color = Design.brand.heroChip, contentColor = Design.brand.onHero, shape = Design.tokens.chipShape) {
        Text(text, style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp))
    }
}
