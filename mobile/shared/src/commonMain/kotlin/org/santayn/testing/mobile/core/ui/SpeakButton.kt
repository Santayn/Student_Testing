package org.santayn.testing.mobile.core.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.StopCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
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
