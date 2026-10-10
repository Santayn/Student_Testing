package org.santayn.testing.mobile.feature.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import org.koin.compose.koinInject
import org.santayn.testing.mobile.core.storage.AppSettings
import org.santayn.testing.mobile.core.storage.TextScale
import org.santayn.testing.mobile.core.storage.ThemeMode
import org.santayn.testing.mobile.core.ui.RadioRow
import org.santayn.testing.mobile.core.ui.SwitchRow
import org.santayn.testing.mobile.core.ui.ThinDivider

/**
 * Оформление и специальные возможности: тема, размер текста, высокий контраст.
 * Используется в профиле и в диалоге на экране входа (аналог «версии для слабовидящих» на сайтах).
 */
@Composable
fun DisplaySettings(modifier: Modifier = Modifier) {
    val settings = koinInject<AppSettings>()
    val themeMode by settings.themeMode.collectAsState()
    val textScale by settings.textScale.collectAsState()
    val highContrast by settings.highContrast.collectAsState()

    Column(modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        GroupTitle("Тема")
        Column(Modifier.selectableGroup()) {
            listOf(ThemeMode.SYSTEM to "Как в системе", ThemeMode.LIGHT to "Светлая", ThemeMode.DARK to "Тёмная")
                .forEach { (mode, label) ->
                    RadioRow(label, selected = themeMode == mode, onClick = { settings.setThemeMode(mode) })
                }
        }
        ThinDivider()
        GroupTitle("Размер текста")
        Column(Modifier.selectableGroup()) {
            TextScale.entries.forEach { scale ->
                RadioRow(scale.title, selected = textScale == scale, onClick = { settings.setTextScale(scale) })
            }
        }
        Text(
            "Применяется вместе с размером шрифта из настроек телефона.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        ThinDivider()
        SwitchRow(
            title = "Высокий контраст",
            description = "Чёрный текст на белом фоне (в тёмной теме — белый на чёрном) и чёткие границы",
            checked = highContrast,
            onCheckedChange = settings::setHighContrast,
        )
        ThinDivider()
        GroupTitle("Озвучивание")
        Text(
            "Весь интерфейс озвучивают TalkBack (Android: Настройки → Спец. возможности) " +
                "и VoiceOver (iPhone: Настройки → Универсальный доступ). " +
                "В тестах и лекциях есть кнопка «Прочитать вслух».",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun GroupTitle(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.titleSmall,
        modifier = Modifier.semantics { heading() },
    )
}

/** Диалог настроек отображения (экран входа). */
@Composable
fun DisplaySettingsDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Специальные возможности") },
        text = { DisplaySettings(Modifier.verticalScroll(rememberScrollState())) },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Готово") } },
    )
}
