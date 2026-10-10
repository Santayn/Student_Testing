package org.santayn.testing.mobile.feature.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import org.santayn.testing.mobile.core.files.OfflineFiles
import org.santayn.testing.mobile.core.files.formatFileSize
import org.santayn.testing.mobile.core.storage.AppSettings
import org.santayn.testing.mobile.core.storage.OfflineFilesLimit
import org.santayn.testing.mobile.core.storage.ResponseCache
import org.santayn.testing.mobile.core.ui.BusyButton
import org.santayn.testing.mobile.core.ui.ConfirmDialog
import org.santayn.testing.mobile.core.ui.InfoRow
import org.santayn.testing.mobile.core.ui.ThinDivider
import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.roundToInt

/*
 * Ползунок лимита — логарифмический: на линейной шкале 100 МБ…10 ГБ весь диапазон до 1 ГБ
 * занимал бы десятую часть ползунка.
 */
private val LOG_RANGE = ln(OfflineFilesLimit.MAX_MB.toDouble() / OfflineFilesLimit.MIN_MB)

private fun positionOf(mb: Int): Float = (ln(mb.toDouble() / OfflineFilesLimit.MIN_MB) / LOG_RANGE).toFloat()

/** Позиция ползунка → МБ, округлённые до «красивых» значений: 10 МБ до гигабайта, 0.1 ГБ дальше. */
private fun mbAt(position: Float): Int {
    val raw = OfflineFilesLimit.MIN_MB * exp(position * LOG_RANGE)
    val snapped = if (raw < 1024) (raw / 10).roundToInt() * 10.0 else (raw / 102.4).roundToInt() * 102.4
    return OfflineFilesLimit.clamp(snapped.roundToInt())
}

/**
 * Данные для работы без сети: сколько занято, лимит для материалов лекций, очистка.
 * Ответы сервера (JSON) занимают мало — до 20 МБ; основное место — скачанные материалы (видео, PDF).
 */
@Composable
fun OfflineStorageSettings(modifier: Modifier = Modifier) {
    val settings = koinInject<AppSettings>()
    val responses = koinInject<ResponseCache>()
    val files = koinInject<OfflineFiles>()
    val limitMb by settings.offlineFilesLimitMb.collectAsState()
    val scope = rememberCoroutineScope()
    val focus = LocalFocusManager.current

    var responsesBytes by remember { mutableStateOf<Long?>(null) }
    var filesBytes by remember { mutableStateOf<Long?>(null) }
    var reloadKey by remember { mutableIntStateOf(0) }
    var confirmClear by remember { mutableStateOf(false) }
    var clearing by remember { mutableStateOf(false) }

    // Ползунок и поле редактируются локально, сохраняются по окончании перетаскивания / вводу.
    var sliderPosition by remember(limitMb) { mutableFloatStateOf(positionOf(limitMb)) }
    var input by remember(limitMb) { mutableStateOf(OfflineFilesLimit.title(limitMb)) }
    var inputError by remember { mutableStateOf<String?>(null) }
    var inputEdited by remember { mutableStateOf(false) }

    LaunchedEffect(reloadKey, limitMb) {
        responsesBytes = responses.totalBytes()
        filesBytes = files.totalBytes()
    }

    fun apply(mb: Int) {
        settings.setOfflineFilesLimitMb(mb)
        scope.launch {
            files.trimToLimit()
            reloadKey++
        }
    }

    fun applyInput() {
        inputEdited = false
        val parsed = OfflineFilesLimit.parse(input)
        if (parsed == null) {
            inputError = "Введите размер, например 500 или 2 ГБ"
            return
        }
        val clamped = OfflineFilesLimit.clamp(parsed)
        inputError = if (clamped != parsed) {
            "Допустимо от ${OfflineFilesLimit.title(OfflineFilesLimit.MIN_MB)} до ${OfflineFilesLimit.title(OfflineFilesLimit.MAX_MB)}"
        } else {
            null
        }
        input = OfflineFilesLimit.title(clamped)
        sliderPosition = positionOf(clamped)
        if (clamped != limitMb) apply(clamped)
    }

    Column(modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        InfoRow("Данные экранов", responsesBytes?.let(::formatFileSize) ?: "…")
        InfoRow("Материалы лекций", filesBytes?.let { "${formatFileSize(it)} из ${OfflineFilesLimit.title(limitMb)}" } ?: "…")
        Text(
            "Открытые экраны и скачанные материалы доступны без интернета. " +
                "Когда место заканчивается, удаляются самые давние материалы.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        ThinDivider()
        Text(
            "Место под материалы",
            style = MaterialTheme.typography.titleSmall,
            modifier = Modifier.semantics { heading() },
        )
        val shownMb = mbAt(sliderPosition)
        Slider(
            value = sliderPosition,
            onValueChange = {
                sliderPosition = it
                input = OfflineFilesLimit.title(mbAt(it))
                inputError = null
            },
            onValueChangeFinished = { if (shownMb != limitMb) apply(shownMb) },
            modifier = Modifier.fillMaxWidth().semantics {
                contentDescription = "Место под материалы"
                stateDescription = OfflineFilesLimit.title(shownMb)
            },
        )
        Row(Modifier.fillMaxWidth()) {
            Text(
                OfflineFilesLimit.title(OfflineFilesLimit.MIN_MB),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
            )
            Text(
                OfflineFilesLimit.title(OfflineFilesLimit.MAX_MB),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        OutlinedTextField(
            value = input,
            onValueChange = {
                input = it
                inputError = null
                inputEdited = true
            },
            label = { Text("Лимит") },
            supportingText = { Text(inputError ?: "Например: 500, 500 МБ, 2 ГБ") },
            isError = inputError != null,
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text, imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = {
                applyInput()
                focus.clearFocus()
            }),
            shape = MaterialTheme.shapes.small,
            // Ввод применяется по «Готово» или при уходе из поля — только если его меняли.
            modifier = Modifier.fillMaxWidth().onFocusChanged { if (!it.isFocused && inputEdited) applyInput() },
        )
        BusyButton(
            "Удалить сохранённые данные",
            onClick = { confirmClear = true },
            outlined = true,
            busy = clearing,
            icon = Icons.Default.DeleteSweep,
            modifier = Modifier.fillMaxWidth(),
        )
    }

    if (confirmClear) {
        ConfirmDialog(
            title = "Удалить сохранённые данные?",
            message = "Без интернета будут недоступны ранее открытые экраны и скачанные материалы. " +
                "При следующем открытии с интернетом всё загрузится заново.",
            confirmText = "Удалить",
            destructive = true,
            onConfirm = {
                confirmClear = false
                clearing = true
                scope.launch {
                    responses.clear()
                    files.clear()
                    clearing = false
                    reloadKey++
                }
            },
            onDismiss = { confirmClear = false },
        )
    }
}
