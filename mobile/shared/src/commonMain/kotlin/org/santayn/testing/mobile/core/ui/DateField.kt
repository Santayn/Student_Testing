package org.santayn.testing.mobile.core.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Instant

/** Дата в формате дд.мм.гггг. */
fun LocalDate.toRu(): String =
    "${day.toString().padStart(2, '0')}.${(month.ordinal + 1).toString().padStart(2, '0')}.$year"

/** Начало дня в часовом поясе устройства → ISO UTC. */
fun LocalDate.startOfDayUtcIso(): String = atStartOfDayIn(TimeZone.currentSystemDefault()).toString()

/** Конец дня (23:59) в часовом поясе устройства → ISO UTC. */
fun LocalDate.endOfDayUtcIso(): String =
    LocalDateTime(this, LocalTime(23, 59)).toInstant(TimeZone.currentSystemDefault()).toString()

/** ISO-строка Instant → локальная дата. */
fun isoToLocalDate(iso: String?): LocalDate? =
    iso?.let { runCatching { Instant.parse(it).toLocalDateTime(TimeZone.currentSystemDefault()).date }.getOrNull() }

/** Поле выбора даты с календарём Material 3. */
@Composable
fun DateField(
    label: String,
    value: LocalDate?,
    onChange: (LocalDate?) -> Unit,
    modifier: Modifier = Modifier,
    allowClear: Boolean = false,
    error: String? = null,
    enabled: Boolean = true,
) {
    var open by rememberSaveable { mutableStateOf(false) }
    Box(modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = value?.toRu().orEmpty(),
            onValueChange = {},
            readOnly = true,
            enabled = enabled,
            label = { Text(label) },
            trailingIcon = { Icon(Icons.Default.CalendarMonth, contentDescription = null) },
            isError = error != null,
            supportingText = error?.let { { Text(it) } },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.small,
        )
        Box(
            Modifier.matchParentSize().padding(top = 8.dp).clip(MaterialTheme.shapes.small)
                .clickable(enabled = enabled) { open = true }
        )
    }
    if (open) {
        val initialMillis = value?.atStartOfDayIn(TimeZone.UTC)?.toEpochMilliseconds()
        val pickerState = rememberDatePickerState(initialSelectedDateMillis = initialMillis)
        DatePickerDialog(
            onDismissRequest = { open = false },
            confirmButton = {
                TextButton(onClick = {
                    val millis = pickerState.selectedDateMillis
                    onChange(millis?.let { Instant.fromEpochMilliseconds(it).toLocalDateTime(TimeZone.UTC).date })
                    open = false
                }) { Text("Выбрать") }
            },
            dismissButton = {
                if (allowClear) {
                    TextButton(onClick = {
                        onChange(null)
                        open = false
                    }) { Text("Очистить") }
                }
                TextButton(onClick = { open = false }) { Text("Отмена") }
            },
        ) {
            DatePicker(state = pickerState)
        }
    }
}
