package org.santayn.testing.mobile.feature.learning

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.santayn.testing.mobile.core.ui.SpeakButton
import org.santayn.testing.mobile.core.ui.SpeakPillButton
import org.santayn.testing.mobile.core.ui.design.Design
import org.santayn.testing.mobile.core.ui.design.DesignTheme
import org.santayn.testing.mobile.core.ui.design.KafedraType
import org.santayn.testing.mobile.data.model.PublicLectureDto

/**
 * Шапка экрана лекции: номер, название, описание и «Прочитать вслух».
 * У каждой темы оформления своя компоновка (макеты, столбец «Лекция»).
 */
@Composable
internal fun LectureHero(lecture: PublicLectureDto, modifier: Modifier = Modifier) {
    when (Design.tokens.theme) {
        DesignTheme.POLYTECH -> PolytechLectureHero(lecture, modifier)
        DesignTheme.KAFEDRA -> KafedraLectureHero(lecture, modifier)
        DesignTheme.ZACHET -> ZachetLectureHero(lecture, modifier)
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

/** «Кафедра»: без цветной карточки, как начало статьи — надзаголовок, крупный заголовок, текст. */
@Composable
private fun KafedraLectureHero(lecture: PublicLectureDto, modifier: Modifier) {
    val description = lecture.description?.takeIf { it.isNotBlank() }
    val overline = "Лекция ${lecture.ordinal}" + if (lecture.versionNumber > 0) " · Версия ${lecture.versionNumber}" else ""
    Column(modifier.fillMaxWidth().padding(horizontal = 4.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                overline.uppercase(),
                style = KafedraType.overline,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
            )
            CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.primary) {
                SpeakButton(
                    key = "lecture-${lecture.id}",
                    text = { listOfNotNull(lecture.title, description).joinToString(". ") },
                )
            }
        }
        Text(
            lecture.title,
            style = KafedraType.lectureTitle,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.semantics { heading() },
        )
        Text(
            description ?: "Описание лекции не указано.",
            style = KafedraType.lectureBody,
            color = if (description != null) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** «Зачёт»: без панели — небесный надзаголовок, очень жирный заголовок, приглушённое описание. */
@Composable
private fun ZachetLectureHero(lecture: PublicLectureDto, modifier: Modifier) {
    val description = lecture.description?.takeIf { it.isNotBlank() }
    val overline = "Лекция ${lecture.ordinal}" + if (lecture.versionNumber > 0) " · версия ${lecture.versionNumber}" else ""
    Column(modifier.fillMaxWidth().padding(horizontal = 4.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                overline,
                style = MaterialTheme.typography.labelLarge.copy(fontSize = 14.sp, fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.weight(1f),
            )
            CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.primary) {
                SpeakButton(
                    key = "lecture-${lecture.id}",
                    text = { listOfNotNull(lecture.title, description).joinToString(". ") },
                )
            }
        }
        Text(
            lecture.title,
            style = MaterialTheme.typography.headlineMedium.copy(fontSize = 28.sp, lineHeight = 32.sp, fontWeight = FontWeight.ExtraBold),
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.semantics { heading() },
        )
        Text(
            description ?: "Описание лекции не указано.",
            style = MaterialTheme.typography.bodyLarge.copy(fontSize = 16.sp, lineHeight = 24.sp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun HeroChip(text: String) {
    Surface(color = Design.brand.heroChip, contentColor = Design.brand.onHero, shape = Design.tokens.chipShape) {
        Text(text, style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp))
    }
}
