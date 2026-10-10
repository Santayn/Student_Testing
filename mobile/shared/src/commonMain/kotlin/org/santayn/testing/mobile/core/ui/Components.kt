package org.santayn.testing.mobile.core.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.error
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

// region Global snackbar

/** Глобальный хост уведомлений (аналог UiToastHost). */
val LocalSnackbarHostState = staticCompositionLocalOf { SnackbarHostState() }

// endregion

// region Screen scaffold

/**
 * Каркас экрана: верхняя панель с заголовком, кнопкой «Назад» и действиями.
 */
@Composable
fun AppScreen(
    title: String,
    onBack: (() -> Unit)? = null,
    subtitle: String? = null,
    actions: @Composable RowScope.() -> Unit = {},
    floatingActionButton: @Composable () -> Unit = {},
    bottomBar: @Composable () -> Unit = {},
    content: @Composable (PaddingValues) -> Unit,
) {
    Scaffold(
        topBar = {
            Column {
            TopAppBar(
                title = {
                    // Заголовок экрана — heading: по заголовкам можно перемещаться жестами TalkBack/VoiceOver.
                    Column(Modifier.semantics(mergeDescendants = true) { heading() }) {
                        Text(title, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        if (!subtitle.isNullOrBlank()) {
                            Text(
                                subtitle,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                },
                navigationIcon = {
                    if (onBack != null) {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                        }
                    }
                },
                actions = actions,
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
            )
            NetworkBanner()
            }
        },
        snackbarHost = { SnackbarHost(LocalSnackbarHostState.current) },
        floatingActionButton = floatingActionButton,
        bottomBar = bottomBar,
        containerColor = MaterialTheme.colorScheme.background,
        content = content,
    )
}

// endregion

// region Load states

@Composable
fun LoadingState(modifier: Modifier = Modifier, message: String? = null) {
    Box(modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator()
            if (message != null) {
                Spacer(Modifier.height(12.dp))
                Text(message, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
            }
        }
    }
}

/**
 * Ошибка загрузки. Если причина — связь, показывает «Нет подключения» / «Сервер недоступен»
 * с подсказкой; данные перезагрузятся сами при восстановлении связи (см. [LoadStateContent]).
 */
@Composable
fun ErrorState(
    message: String,
    modifier: Modifier = Modifier,
    title: String = "Не удалось загрузить данные",
    onRetry: (() -> Unit)? = null,
) {
    val offline = offlineCopy(currentNetworkState())
    val shownTitle = offline?.title ?: title
    val shownMessage = if (offline != null && (message == offline.title || message.startsWith("Сервер недоступен"))) {
        offline.hint
    } else {
        message
    }
    Box(modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
        ) {
            Icon(
                offline?.icon ?: Icons.Default.ErrorOutline,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(40.dp),
            )
            Spacer(Modifier.height(12.dp))
            Text(
                shownTitle,
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier.semantics { heading() },
            )
            Spacer(Modifier.height(4.dp))
            Text(
                shownMessage,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            if (onRetry != null) {
                Spacer(Modifier.height(16.dp))
                Button(onClick = onRetry) { Text("Повторить") }
            }
        }
    }
}

@Composable
fun EmptyState(
    title: String,
    modifier: Modifier = Modifier,
    message: String? = null,
    icon: ImageVector = Icons.Default.Inbox,
    action: (@Composable () -> Unit)? = null,
) {
    Box(modifier.fillMaxWidth().padding(vertical = 40.dp, horizontal = 24.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                Modifier.size(64.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center,
            ) {
                Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(Modifier.height(12.dp))
            Text(
                title,
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier.semantics { heading() },
            )
            if (message != null) {
                Spacer(Modifier.height(4.dp))
                Text(message, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
            }
            if (action != null) {
                Spacer(Modifier.height(16.dp))
                action()
            }
        }
    }
}

/** Состояние загрузки данных экрана. */
sealed interface LoadState<out T> {
    data object Loading : LoadState<Nothing>
    data class Error(val message: String) : LoadState<Nothing>
    data class Ready<T>(val data: T) : LoadState<T>
}

val <T> LoadState<T>.dataOrNull: T? get() = (this as? LoadState.Ready<T>)?.data

/**
 * Отрисовка [LoadState] с pull-to-refresh.
 *
 * Когда связь с сервером восстанавливается, ошибка перезагружается автоматически,
 * а уже показанные данные обновляются (если [refreshOnReconnect]).
 */
@Composable
fun <T> LoadStateContent(
    state: LoadState<T>,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
    refreshing: Boolean = false,
    onRefresh: (() -> Unit)? = null,
    refreshOnReconnect: Boolean = true,
    content: @Composable (T) -> Unit,
) {
    val current by rememberUpdatedState(state)
    ReloadOnReconnect {
        when (current) {
            is LoadState.Error -> onRetry()
            is LoadState.Ready -> if (refreshOnReconnect) (onRefresh ?: onRetry)()
            LoadState.Loading -> Unit
        }
    }
    when (state) {
        LoadState.Loading -> LoadingState(modifier)
        is LoadState.Error -> ErrorState(state.message, modifier, onRetry = onRetry)
        is LoadState.Ready -> {
            if (onRefresh != null) {
                PullToRefreshBox(
                    isRefreshing = refreshing,
                    onRefresh = onRefresh,
                    modifier = modifier.fillMaxSize(),
                ) { content(state.data) }
            } else {
                Box(modifier.fillMaxSize()) { content(state.data) }
            }
        }
    }
}

/** Стандартный список экрана с отступами. */
@Composable
fun ScreenList(
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(16.dp),
    content: LazyListScope.() -> Unit,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = contentPadding,
        verticalArrangement = Arrangement.spacedBy(12.dp),
        content = content,
    )
}

// endregion

// region Cards & text blocks

@Composable
fun AppCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    containerColor: Color = MaterialTheme.colorScheme.surface,
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = CardDefaults.cardColors(containerColor = containerColor)
    val border = CardDefaults.outlinedCardBorder()
    if (onClick != null) {
        Card(
            onClick = onClick,
            modifier = modifier.fillMaxWidth(),
            colors = colors,
            border = border,
        ) { Column(Modifier.padding(16.dp), content = content) }
    } else {
        Card(modifier = modifier.fillMaxWidth(), colors = colors, border = border) {
            Column(Modifier.padding(16.dp), content = content)
        }
    }
}

/** Карточка-ссылка: иконка, заголовок, описание, стрелка. */
@Composable
fun NavCard(
    title: String,
    modifier: Modifier = Modifier,
    description: String? = null,
    icon: ImageVector? = null,
    trailing: @Composable (() -> Unit)? = null,
    onClick: () -> Unit,
) {
    AppCard(modifier = modifier, onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (icon != null) {
                Box(
                    Modifier.size(40.dp).clip(RoundedCornerShape(10.dp))
                        .background(AppTheme.status.primarySoft),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(icon, contentDescription = null, tint = AppTheme.status.primarySoftText)
                }
                Spacer(Modifier.width(12.dp))
            }
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                if (!description.isNullOrBlank()) {
                    Spacer(Modifier.height(2.dp))
                    Text(
                        description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            if (trailing != null) {
                trailing()
            } else {
                Icon(
                    Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier, action: (@Composable () -> Unit)? = null) {
    Row(
        modifier.fillMaxWidth().padding(top = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f).semantics { heading() },
        )
        action?.invoke()
    }
}

/** Пара «подпись: значение». */
@Composable
fun InfoRow(label: String, value: String?, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxWidth().padding(vertical = 4.dp).semantics(mergeDescendants = true) {}) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value?.takeIf { it.isNotBlank() } ?: "—", style = MaterialTheme.typography.bodyLarge)
    }
}

enum class Tone { NEUTRAL, PRIMARY, SUCCESS, WARNING, DANGER, INFO }

@Composable
fun StatusChip(text: String, tone: Tone = Tone.NEUTRAL, modifier: Modifier = Modifier) {
    val s = AppTheme.status
    val (bg, fg) = when (tone) {
        Tone.NEUTRAL -> MaterialTheme.colorScheme.surfaceVariant to MaterialTheme.colorScheme.onSurfaceVariant
        Tone.PRIMARY -> s.primarySoft to s.primarySoftText
        Tone.SUCCESS -> s.successSoft to s.successText
        Tone.WARNING -> s.warningSoft to s.warningText
        Tone.DANGER -> s.dangerSoft to s.dangerText
        Tone.INFO -> s.infoSoft to s.infoText
    }
    Surface(color = bg, contentColor = fg, shape = RoundedCornerShape(999.dp), modifier = modifier) {
        Text(
            text,
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            maxLines = 1,
        )
    }
}

/** Плашка-уведомление (UiAlert). */
@Composable
fun Banner(text: String, tone: Tone = Tone.INFO, modifier: Modifier = Modifier, title: String? = null) {
    val s = AppTheme.status
    val (bg, fg) = when (tone) {
        Tone.SUCCESS -> s.successSoft to s.successText
        Tone.WARNING -> s.warningSoft to s.warningText
        Tone.DANGER -> s.dangerSoft to s.dangerText
        Tone.PRIMARY -> s.primarySoft to s.primarySoftText
        else -> s.infoSoft to s.infoText
    }
    val announce = tone == Tone.DANGER || tone == Tone.WARNING
    Surface(
        color = bg,
        contentColor = fg,
        shape = MaterialTheme.shapes.medium,
        modifier = modifier.fillMaxWidth().semantics(mergeDescendants = true) {
            // Ошибки и предупреждения зачитываются сразу при появлении.
            if (announce) liveRegion = LiveRegionMode.Polite
        },
    ) {
        Column(Modifier.padding(12.dp)) {
            if (title != null) {
                Text(title, style = MaterialTheme.typography.titleSmall)
                Spacer(Modifier.height(2.dp))
            }
            Text(text, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

/**
 * Ошибка загрузки части экрана с кнопкой «Повторить».
 * Нужна, чтобы при сбое не показывать пустой список («нет данных»), которого на самом деле нет.
 */
@Composable
fun RetryBanner(message: String, onRetry: () -> Unit, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Banner(message, Tone.DANGER, title = "Не удалось загрузить данные")
        OutlinedButton(onClick = onRetry, modifier = Modifier.heightIn(min = 48.dp)) { Text("Повторить") }
    }
}

@Composable
fun StatTile(label: String, value: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surface,
        border = CardDefaults.outlinedCardBorder(),
    ) {
        Column(Modifier.padding(12.dp).semantics(mergeDescendants = true) {}) {
            Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(4.dp))
            Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        }
    }
}

/** Сетка статистики по 2 в ряд. */
@Composable
fun StatGrid(items: List<Pair<String, String>>, modifier: Modifier = Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items.chunked(2).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { (label, value) -> StatTile(label, value, Modifier.weight(1f)) }
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

// endregion

// region Inputs

@Composable
fun SearchField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "Поиск",
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.fillMaxWidth(),
        placeholder = { Text(placeholder) },
        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
        trailingIcon = {
            if (value.isNotEmpty()) {
                IconButton(onClick = { onValueChange("") }) {
                    Icon(Icons.Default.Clear, contentDescription = "Очистить")
                }
            }
        },
        singleLine = true,
        shape = MaterialTheme.shapes.small,
    )
}

@Composable
fun FormField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    error: String? = null,
    supportingText: String? = null,
    singleLine: Boolean = true,
    minLines: Int = 1,
    maxLength: Int? = null,
    keyboardType: KeyboardType = KeyboardType.Text,
    password: Boolean = false,
    enabled: Boolean = true,
    required: Boolean = false,
    trailingIcon: @Composable (() -> Unit)? = null,
) {
    OutlinedTextField(
        value = value,
        onValueChange = { if (maxLength == null || it.length <= maxLength) onValueChange(it) },
        label = { Text(if (required) "$label *" else label) },
        modifier = modifier.fillMaxWidth(),
        isError = error != null,
        supportingText = when {
            error != null -> ({ Text(error) })
            supportingText != null || maxLength != null -> ({
                Row(Modifier.fillMaxWidth()) {
                    Text(supportingText.orEmpty(), Modifier.weight(1f))
                    if (maxLength != null && !singleLine) Text("${value.length}/$maxLength")
                }
            })
            else -> null
        },
        singleLine = singleLine,
        minLines = minLines,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        visualTransformation = if (password) PasswordVisualTransformation() else androidx.compose.ui.text.input.VisualTransformation.None,
        enabled = enabled,
        trailingIcon = trailingIcon,
        shape = MaterialTheme.shapes.small,
    )
}

@Composable
fun SwitchRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    description: String? = null,
    enabled: Boolean = true,
) {
    // Вся строка — один переключатель: TalkBack читает «название, вкл/выкл, переключатель».
    Row(
        modifier.fillMaxWidth().heightIn(min = 48.dp).clip(MaterialTheme.shapes.small)
            .toggleable(value = checked, enabled = enabled, role = Role.Switch, onValueChange = onCheckedChange)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            if (description != null) {
                Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Spacer(Modifier.width(12.dp))
        Switch(checked = checked, onCheckedChange = null, enabled = enabled)
    }
}

@Composable
fun CheckRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    description: String? = null,
    enabled: Boolean = true,
) {
    Row(
        modifier.fillMaxWidth().heightIn(min = 48.dp).clip(MaterialTheme.shapes.small)
            .toggleable(value = checked, enabled = enabled, role = Role.Checkbox, onValueChange = onCheckedChange)
            .padding(vertical = 4.dp, horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(checked = checked, onCheckedChange = null, enabled = enabled)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            if (description != null) {
                Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
fun RadioRow(
    title: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    Row(
        modifier.fillMaxWidth().heightIn(min = 48.dp).clip(MaterialTheme.shapes.small)
            .selectable(selected = selected, enabled = enabled, role = Role.RadioButton, onClick = onClick)
            .padding(vertical = 4.dp, horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = selected, onClick = null, enabled = enabled)
        Spacer(Modifier.width(12.dp))
        Text(title, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
    }
}

/** Вариант для [SelectField]. */
data class SelectOption<T>(val value: T, val label: String, val description: String? = null)

/**
 * Поле выбора: по нажатию открывает диалог со списком и поиском.
 * Заменяет UiSelect / PrimeVue Select.
 */
@Composable
fun <T> SelectField(
    label: String,
    options: List<SelectOption<T>>,
    selected: T?,
    onSelect: (T?) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "Не выбрано",
    allowClear: Boolean = false,
    enabled: Boolean = true,
    error: String? = null,
    required: Boolean = false,
    searchable: Boolean = options.size > 7,
    /** Подпись для программы чтения с экрана, если видимой [label] недостаточно. */
    accessibilityLabel: String? = null,
) {
    var open by rememberSaveable { mutableStateOf(false) }
    val selectedOption = options.firstOrNull { it.value == selected }
    Box(modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = selectedOption?.label ?: "",
            onValueChange = {},
            readOnly = true,
            enabled = enabled,
            label = { Text(if (required) "$label *" else label) },
            placeholder = { Text(placeholder) },
            trailingIcon = { Icon(Icons.Default.ArrowDropDown, contentDescription = null) },
            isError = error != null,
            supportingText = error?.let { { Text(it) } },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().clearAndSetSemantics {},
            shape = MaterialTheme.shapes.small,
        )
        // Прозрачный слой поверх поля: перехватывает нажатие и описывает поле для программы чтения с экрана.
        Box(
            Modifier.matchParentSize().padding(top = 8.dp)
                .clip(MaterialTheme.shapes.small)
                .clickable(enabled = enabled, role = Role.DropdownList, onClickLabel = "выбрать") { open = true }
                .semantics {
                    val spoken = accessibilityLabel ?: label
                    contentDescription = if (required) "$spoken, обязательное поле" else spoken
                    stateDescription = selectedOption?.label ?: placeholder
                    if (error != null) error(error)
                }
        )
    }
    if (open) {
        SelectDialog(
            title = label,
            options = options,
            selected = selected,
            allowClear = allowClear,
            searchable = searchable,
            onDismiss = { open = false },
            onSelect = {
                onSelect(it)
                open = false
            },
        )
    }
}

@Composable
private fun <T> SelectDialog(
    title: String,
    options: List<SelectOption<T>>,
    selected: T?,
    allowClear: Boolean,
    searchable: Boolean,
    onDismiss: () -> Unit,
    onSelect: (T?) -> Unit,
) {
    var query by remember { mutableStateOf("") }
    val filtered = remember(query, options) {
        if (query.isBlank()) options
        else options.filter {
            it.label.contains(query.trim(), ignoreCase = true) ||
                it.description?.contains(query.trim(), ignoreCase = true) == true
        }
    }
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(
            shape = MaterialTheme.shapes.large,
            modifier = Modifier.fillMaxWidth(0.92f).heightIn(max = 560.dp),
        ) {
            Column(Modifier.padding(vertical = 16.dp)) {
                Text(
                    title,
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(horizontal = 20.dp).semantics { heading() },
                )
                if (searchable) {
                    SearchField(query, { query = it }, Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
                }
                LazyColumn(Modifier.weight(1f, fill = false)) {
                    if (allowClear) {
                        item {
                            SelectRow(label = "Не выбрано", description = null, checked = selected == null) { onSelect(null) }
                        }
                    }
                    items(filtered) { option ->
                        SelectRow(option.label, option.description, option.value == selected) { onSelect(option.value) }
                    }
                    if (filtered.isEmpty()) {
                        item {
                            Text(
                                "Ничего не найдено",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(20.dp),
                            )
                        }
                    }
                }
                Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text("Отмена") }
                }
            }
        }
    }
}

@Composable
private fun SelectRow(label: String, description: String?, checked: Boolean, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().heightIn(min = 48.dp)
            .selectable(selected = checked, role = Role.RadioButton, onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.bodyLarge)
            if (description != null) {
                Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        // «Выбрано» озвучивается через selectable; галочка — только визуальная.
        if (checked) Icon(Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
    }
}

/** Множественный выбор: поле со списком выбранных + диалог с чекбоксами. */
@Composable
fun <T> MultiSelectField(
    label: String,
    options: List<SelectOption<T>>,
    selected: Set<T>,
    onChange: (Set<T>) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "Не выбрано",
    enabled: Boolean = true,
    error: String? = null,
) {
    var open by rememberSaveable { mutableStateOf(false) }
    val summary = options.filter { it.value in selected }.joinToString(", ") { it.label }
    Box(modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = summary,
            onValueChange = {},
            readOnly = true,
            enabled = enabled,
            label = { Text(label) },
            placeholder = { Text(placeholder) },
            trailingIcon = { Icon(Icons.Default.ArrowDropDown, contentDescription = null) },
            isError = error != null,
            supportingText = error?.let { { Text(it) } },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().clearAndSetSemantics {},
            shape = MaterialTheme.shapes.small,
        )
        Box(
            Modifier.matchParentSize().padding(top = 8.dp).clip(MaterialTheme.shapes.small)
                .clickable(enabled = enabled, role = Role.DropdownList, onClickLabel = "выбрать") { open = true }
                .semantics {
                    contentDescription = label
                    stateDescription = summary.ifBlank { placeholder }
                    if (error != null) error(error)
                }
        )
    }
    if (open) {
        var draft by remember { mutableStateOf(selected) }
        var query by remember { mutableStateOf("") }
        val filtered = options.filter { query.isBlank() || it.label.contains(query.trim(), ignoreCase = true) }
        Dialog(onDismissRequest = { open = false }, properties = DialogProperties(usePlatformDefaultWidth = false)) {
            Surface(shape = MaterialTheme.shapes.large, modifier = Modifier.fillMaxWidth(0.92f).heightIn(max = 600.dp)) {
                Column(Modifier.padding(vertical = 16.dp)) {
                    Text(
                        label,
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.padding(horizontal = 20.dp).semantics { heading() },
                    )
                    if (options.size > 7) {
                        SearchField(query, { query = it }, Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
                    }
                    LazyColumn(Modifier.weight(1f, fill = false).padding(horizontal = 8.dp)) {
                        items(filtered) { option ->
                            CheckRow(
                                title = option.label,
                                description = option.description,
                                checked = option.value in draft,
                                onCheckedChange = { checked ->
                                    draft = if (checked) draft + option.value else draft - option.value
                                },
                            )
                        }
                    }
                    Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp), horizontalArrangement = Arrangement.End) {
                        TextButton(onClick = { open = false }) { Text("Отмена") }
                        TextButton(onClick = {
                            onChange(draft)
                            open = false
                        }) { Text("Готово") }
                    }
                }
            }
        }
    }
}

// endregion

// region Dialogs & buttons

@Composable
fun ConfirmDialog(
    title: String,
    message: String,
    confirmText: String = "Подтвердить",
    dismissText: String = "Отмена",
    destructive: Boolean = false,
    busy: Boolean = false,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = { if (!busy) onDismiss() },
        title = { Text(title) },
        text = { Text(message) },
        confirmButton = {
            Button(
                onClick = onConfirm,
                enabled = !busy,
                colors = if (destructive) {
                    ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError,
                    )
                } else {
                    ButtonDefaults.buttonColors()
                },
            ) {
                if (busy) {
                    CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp, color = LocalContentColorOnButton())
                } else {
                    Text(confirmText)
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !busy) { Text(dismissText) }
        },
    )
}

@Composable
private fun LocalContentColorOnButton(): Color = androidx.compose.material3.LocalContentColor.current

/** Кнопка с индикатором выполнения. */
@Composable
fun BusyButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    busy: Boolean = false,
    enabled: Boolean = true,
    outlined: Boolean = false,
    icon: ImageVector? = null,
) {
    val content: @Composable RowScope.() -> Unit = {
        if (busy) {
            CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp, color = androidx.compose.material3.LocalContentColor.current)
            Spacer(Modifier.width(8.dp))
        } else if (icon != null) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
        }
        Text(text)
    }
    // Пока идёт операция, кнопка озвучивается как «выполняется».
    val buttonModifier = modifier.heightIn(min = 48.dp).semantics { if (busy) stateDescription = "выполняется" }
    if (outlined) {
        OutlinedButton(onClick = onClick, modifier = buttonModifier, enabled = enabled && !busy, content = content)
    } else {
        Button(onClick = onClick, modifier = buttonModifier, enabled = enabled && !busy, content = content)
    }
}

/**
 * Полноэкранный редактор в диалоге (замена Drawer'ов веба).
 */
@Composable
fun EditorDialog(
    title: String,
    onDismiss: () -> Unit,
    onSave: () -> Unit,
    saveText: String = "Сохранить",
    busy: Boolean = false,
    saveEnabled: Boolean = true,
    error: String? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    Dialog(
        onDismissRequest = { if (!busy) onDismiss() },
        properties = DialogProperties(usePlatformDefaultWidth = false, dismissOnClickOutside = false),
    ) {
        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            Column(Modifier.fillMaxSize()) {
                Surface(color = MaterialTheme.colorScheme.surface, shadowElevation = 1.dp) {
                    Row(
                        Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        IconButton(onClick = onDismiss, enabled = !busy) {
                            Icon(Icons.Default.Clear, contentDescription = "Закрыть")
                        }
                        Text(
                            title,
                            style = MaterialTheme.typography.titleLarge,
                            modifier = Modifier.weight(1f).semantics { heading() },
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                        TextButton(onClick = onSave, enabled = saveEnabled && !busy) {
                            if (busy) {
                                CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                            } else {
                                Text(saveText)
                            }
                        }
                    }
                }
                Column(
                    Modifier.weight(1f).fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    if (error != null) Banner(error, Tone.DANGER)
                    content()
                    Spacer(Modifier.height(24.dp))
                }
            }
        }
    }
}

@Composable
fun ThinDivider(modifier: Modifier = Modifier) = HorizontalDivider(modifier, color = MaterialTheme.colorScheme.outlineVariant)

// endregion

/** Отступы содержимого списка с учётом паддинга Scaffold. */
fun screenPadding(inner: PaddingValues): PaddingValues = PaddingValues(
    start = 16.dp,
    end = 16.dp,
    top = inner.calculateTopPadding() + 8.dp,
    bottom = inner.calculateBottomPadding() + 24.dp,
)

/** Показ одноразовых сообщений ViewModel в общем снэкбаре. */
@Composable
fun CollectMessages(viewModel: BaseViewModel) {
    val host = LocalSnackbarHostState.current
    androidx.compose.runtime.LaunchedEffect(viewModel) {
        viewModel.messages.collect { host.showSnackbar(it) }
    }
}

/** Временная заглушка экрана. */
@Composable
fun PlaceholderScreen(title: String, onBack: () -> Unit) {
    AppScreen(title = title, onBack = onBack) { padding ->
        Box(Modifier.padding(padding)) { EmptyState(title = "Раздел в разработке") }
    }
}
