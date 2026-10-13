package org.santayn.testing.mobile.core.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.StopCircle
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.TextButton
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import org.santayn.testing.mobile.core.ui.design.Design
import org.koin.compose.koinInject
import org.santayn.testing.mobile.core.platform.SpeechProblem
import org.santayn.testing.mobile.core.platform.TextSpeaker
import org.santayn.testing.mobile.core.platform.platformName

/**
 * Кнопка «Прочитать вслух» / «Остановить чтение».
 * [key] отличает тексты на экране: нажатие на другой кнопке прерывает текущее чтение.
 * Чтение останавливается, когда кнопка уходит с экрана.
 */
@Composable
fun SpeakButton(key: String, text: () -> String, modifier: Modifier = Modifier) {
    val speaker = koinInject<TextSpeaker>()
    val speakingKey by speaker.speakingKey.collectAsState()
    val speaking = speakingKey == key
    DisposableEffect(key) {
        onDispose { if (speaker.speakingKey.value == key) speaker.stop() }
    }
    IconButton(
        onClick = { if (speaking) speaker.stop() else speaker.speak(key, text()) },
        modifier = modifier,
    ) {
        Icon(
            if (speaking) Icons.Default.StopCircle else Icons.AutoMirrored.Filled.VolumeUp,
            contentDescription = if (speaking) "Остановить чтение" else "Прочитать вслух",
        )
    }
}

/** «Прочитать вслух» кнопкой-«пилюлей» с подписью (на фирменных панелях). */
@Composable
fun SpeakPillButton(
    key: String,
    text: () -> String,
    container: Color,
    content: Color,
    modifier: Modifier = Modifier,
) {
    val speaker = koinInject<TextSpeaker>()
    val speakingKey by speaker.speakingKey.collectAsState()
    val speaking = speakingKey == key
    DisposableEffect(key) {
        onDispose { if (speaker.speakingKey.value == key) speaker.stop() }
    }
    Button(
        onClick = { if (speaking) speaker.stop() else speaker.speak(key, text()) },
        shape = Design.tokens.buttonShape,
        colors = ButtonDefaults.buttonColors(containerColor = container, contentColor = content),
        modifier = modifier.heightIn(min = 48.dp),
    ) {
        Icon(
            if (speaking) Icons.Default.StopCircle else Icons.AutoMirrored.Filled.VolumeUp,
            contentDescription = null,
            modifier = Modifier.size(20.dp),
        )
        Spacer(Modifier.width(8.dp))
        Text(if (speaking) "Остановить чтение" else "Прочитать вслух")
    }
}

/**
 * Сообщение, почему «Прочитать вслух» не сработало, с переходом в нужные настройки.
 * Один на всё приложение (в корне App), а не у каждой кнопки: на экране теста кнопок несколько.
 */
@Composable
fun SpeechProblemHost() {
    val speaker = koinInject<TextSpeaker>()
    val problem by speaker.problem.collectAsState()
    val current = problem ?: return
    val android = platformName.startsWith("Android")
    val (title, message) = speechProblemText(current, android)
    AlertDialog(
        onDismissRequest = speaker::dismissProblem,
        title = { Text(title) },
        text = { Text(message) },
        confirmButton = {
            if (android) {
                AppButton(onClick = {
                    speaker.dismissProblem()
                    speaker.openSettings(current)
                }) { Text(if (current == SpeechProblem.NO_ENGINE) "Установить" else "Открыть настройки") }
            } else {
                AppButton(onClick = speaker::dismissProblem) { Text("Понятно") }
            }
        },
        dismissButton = if (android) {
            { AppOutlinedButton(onClick = speaker::dismissProblem) { Text("Закрыть") } }
        } else {
            null
        },
    )
}

/** Заголовок и текст сообщения: что случилось и как исправить на этой платформе. */
internal fun speechProblemText(problem: SpeechProblem, android: Boolean): Pair<String, String> = when (problem) {
    SpeechProblem.NO_ENGINE -> "Озвучка недоступна" to if (android) {
        "На телефоне нет синтезатора речи. Установите «Синтезатор речи Google» и выберите его: " +
            "Настройки → Специальные возможности → Синтез речи."
    } else {
        "Синтезатор речи не запустился. Перезапустите приложение."
    }
    SpeechProblem.NO_RUSSIAN_VOICE -> "Нет русского голоса" to if (android) {
        "Синтезатор речи на телефоне не читает по-русски. Скачайте русский голос в настройках синтеза речи " +
            "(Настройки → Специальные возможности → Синтез речи) или установите «Синтезатор речи Google»."
    } else {
        "Скачайте русский голос: Настройки → Универсальный доступ → Устный контент → Голоса → Русский."
    }
    SpeechProblem.MUTED -> "Звук выключен" to
        "Громкость на нуле, поэтому чтение не слышно. Прибавьте громкость кнопками на корпусе телефона."
    SpeechProblem.FAILED -> "Не удалось прочитать текст" to if (android) {
        "Синтезатор речи вернул ошибку. Попробуйте ещё раз или выберите другой синтезатор в настройках синтеза речи."
    } else {
        "Синтезатор речи вернул ошибку. Попробуйте ещё раз."
    }
}
