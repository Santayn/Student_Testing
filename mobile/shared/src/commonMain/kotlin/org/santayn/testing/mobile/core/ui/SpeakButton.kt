package org.santayn.testing.mobile.core.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.StopCircle
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
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
import org.santayn.testing.mobile.core.platform.TextSpeaker

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
