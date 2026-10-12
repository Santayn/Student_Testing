package org.santayn.testing.mobile.core.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.error
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import org.santayn.testing.mobile.core.ui.design.Design

/*
 * Собственные элементы управления вместо стандартных Material: поля, переключатель, флажок, радио, чипы, ползунок.
 * Вид берётся из токенов темы (формы полей и чипов, цвета палитры), поэтому новые темы получают их сами.
 * Семантика (роли, состояния, ошибки) задаётся явно — TalkBack/VoiceOver читают их так же, как стандартные.
 */

// region Поля

/** Цвета «плашки» поля: фон, рамка и её толщина в зависимости от состояния. */
@Composable
private fun fieldColors(focused: Boolean, isError: Boolean, enabled: Boolean): Triple<Color, Color, Dp> {
    val scheme = MaterialTheme.colorScheme
    val border = when {
        !enabled -> scheme.outlineVariant
        isError -> scheme.error
        focused -> scheme.primary
        else -> scheme.outline
    }
    val width by animateDpAsState(if (focused || isError) 2.dp else 1.dp)
    val color by animateColorAsState(border)
    val container = if (enabled) scheme.surfaceContainerHigh else scheme.surfaceContainer
    return Triple(container, color, width)
}

/** Подпись поля над «плашкой» (как в макетах: «Логин», «Предмет»). */
@Composable
internal fun FieldLabel(text: String, required: Boolean, isError: Boolean, enabled: Boolean, modifier: Modifier = Modifier) {
    Text(
        if (required) "$text *" else text,
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.SemiBold,
        color = when {
            isError -> MaterialTheme.colorScheme.error
            enabled -> MaterialTheme.colorScheme.onSurfaceVariant
            else -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
        },
        modifier = modifier.padding(start = 4.dp, bottom = 6.dp),
    )
}

/** Подсказка или ошибка под полем и счётчик символов справа. */
@Composable
internal fun FieldSupport(text: String?, isError: Boolean, counter: String? = null) {
    if (text == null && counter == null) return
    Row(Modifier.fillMaxWidth().padding(start = 4.dp, end = 4.dp, top = 6.dp)) {
        Text(
            text.orEmpty(),
            style = MaterialTheme.typography.bodySmall,
            color = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
        if (counter != null) {
            Text(counter, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

/** «Плашка» поля в форме темы: фон контейнера и рамка, усиливающаяся при фокусе и ошибке. */
@Composable
internal fun FieldBox(
    focused: Boolean,
    isError: Boolean,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    minHeight: Dp = 52.dp,
    contentPadding: PaddingValues = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
    content: @Composable () -> Unit,
) {
    val (container, border, width) = fieldColors(focused, isError, enabled)
    val shape = Design.tokens.fieldShape
    Box(
        modifier.fillMaxWidth().heightIn(min = minHeight).clip(shape).background(container).border(width, border, shape)
            .padding(contentPadding),
        contentAlignment = Alignment.CenterStart,
    ) { content() }
}

/**
 * Текстовое поле в стиле темы: подпись сверху, «плашка», подсказка/ошибка снизу.
 * Подпись и подсказка находятся внутри поля (decorationBox), поэтому программа чтения с экрана
 * называет поле его подписью, как у стандартного TextField.
 */
@Composable
fun AppTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String? = null,
    placeholder: String? = null,
    required: Boolean = false,
    error: String? = null,
    supportingText: String? = null,
    counter: String? = null,
    singleLine: Boolean = true,
    minLines: Int = 1,
    enabled: Boolean = true,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
) {
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    val isError = error != null
    val textColor = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.fillMaxWidth().semantics { if (error != null) error(error) },
        enabled = enabled,
        singleLine = singleLine,
        minLines = if (singleLine) 1 else minLines,
        maxLines = if (singleLine) 1 else Int.MAX_VALUE,
        textStyle = MaterialTheme.typography.bodyLarge.copy(color = textColor),
        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
        visualTransformation = visualTransformation,
        interactionSource = interaction,
        decorationBox = { inner ->
            Column {
                if (label != null) FieldLabel(label, required, isError, enabled)
                FieldBox(
                    focused, isError, enabled,
                    contentPadding = PaddingValues(
                        start = if (leadingIcon != null) 4.dp else 16.dp,
                        end = if (trailingIcon != null) 4.dp else 16.dp,
                        // С кнопкой-иконкой (48dp) поле той же высоты, что и без неё.
                        top = if (leadingIcon != null || trailingIcon != null) 2.dp else 14.dp,
                        bottom = if (leadingIcon != null || trailingIcon != null) 2.dp else 14.dp,
                    ),
                ) {
                    Row(verticalAlignment = if (singleLine) Alignment.CenterVertically else Alignment.Top) {
                        if (leadingIcon != null) {
                            Box(Modifier.size(40.dp), contentAlignment = Alignment.Center) { leadingIcon() }
                            Spacer(Modifier.width(4.dp))
                        }
                        Box(Modifier.weight(1f).padding(vertical = if (singleLine) 0.dp else 2.dp)) {
                            if (value.isEmpty() && placeholder != null) {
                                Text(
                                    placeholder,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = if (singleLine) 1 else Int.MAX_VALUE,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                            inner()
                        }
                        if (trailingIcon != null) trailingIcon()
                    }
                }
                FieldSupport(error ?: supportingText, isError, counter)
            }
        },
    )
}

/**
 * Поле, открывающее выбор (список, дата): выглядит как текстовое поле, но целиком — одна кнопка
 * с ролью «выпадающий список». Подпись над полем для программы чтения с экрана входит в описание кнопки.
 */
@Composable
internal fun PickerField(
    label: String,
    valueText: String?,
    placeholder: String,
    onClick: () -> Unit,
    semanticsModifier: Modifier,
    modifier: Modifier = Modifier,
    required: Boolean = false,
    error: String? = null,
    enabled: Boolean = true,
    trailing: @Composable () -> Unit = { PickerChevron(enabled) },
) {
    val isError = error != null
    Column(modifier.fillMaxWidth()) {
        FieldLabel(label, required, isError, enabled, Modifier.clearAndSetSemantics {})
        FieldBox(
            focused = false, isError = isError, enabled = enabled,
            modifier = Modifier
                .clip(Design.tokens.fieldShape)
                .clickable(enabled = enabled, role = Role.DropdownList, onClickLabel = "выбрать", onClick = onClick)
                .then(semanticsModifier),
            contentPadding = PaddingValues(start = 16.dp, end = 8.dp, top = 10.dp, bottom = 10.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    valueText ?: placeholder,
                    style = MaterialTheme.typography.bodyLarge,
                    color = when {
                        !enabled -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        valueText == null -> MaterialTheme.colorScheme.onSurfaceVariant
                        else -> MaterialTheme.colorScheme.onSurface
                    },
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f).clearAndSetSemantics {},
                )
                Spacer(Modifier.width(8.dp))
                trailing()
            }
        }
        if (isError) Box(Modifier.clearAndSetSemantics {}) { FieldSupport(error, true) }
    }
}

/** Значок раскрытия выбора: стрелка в «таблетке» мягкого цвета. */
@Composable
internal fun PickerChevron(enabled: Boolean, icon: androidx.compose.ui.graphics.vector.ImageVector = Icons.Default.KeyboardArrowDown) {
    Box(
        Modifier.size(32.dp).clip(CircleShape)
            .background(if (enabled) AppTheme.status.primarySoft else MaterialTheme.colorScheme.surfaceContainer),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = if (enabled) AppTheme.status.primarySoftText else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp),
        )
    }
}

// endregion

// region Индикаторы: переключатель, флажок, радио

/**
 * Переключатель: «таблетка» 52×32 с бегунком. Включённое состояние видно не только цветом:
 * в бегунке появляется галочка. Только отрисовка — нажатие и роль задаёт строка ([SwitchRow]).
 */
@Composable
fun AppSwitchIndicator(checked: Boolean, enabled: Boolean = true) {
    val scheme = MaterialTheme.colorScheme
    val track by animateColorAsState(
        when {
            !enabled -> scheme.surfaceContainerHighest
            checked -> scheme.primary
            else -> scheme.surfaceContainerHighest
        }
    )
    val thumbOffset by animateDpAsState(if (checked) 22.dp else 2.dp)
    Box(
        Modifier.width(52.dp).height(32.dp).clip(Design.tokens.chipShape.takeIfPill())
            .background(track)
            .border(if (checked) 0.dp else 1.5.dp, if (checked) Color.Transparent else scheme.outline, Design.tokens.chipShape.takeIfPill()),
        contentAlignment = Alignment.CenterStart,
    ) {
        Box(
            Modifier.offset(x = thumbOffset).size(28.dp).clip(CircleShape)
                .background(if (checked) scheme.onPrimary else scheme.outline),
            contentAlignment = Alignment.Center,
        ) {
            if (checked) Icon(Icons.Default.Check, null, tint = scheme.primary, modifier = Modifier.size(18.dp))
        }
    }
}

/** У переключателя всегда скруглённая дорожка, даже если чипы темы прямоугольные. */
private fun androidx.compose.foundation.shape.CornerBasedShape.takeIfPill() = RoundedCornerShape(percent = 50)

/** Флажок: квадрат со скруглением формы плиток темы; выбранный — заливка и галочка. */
@Composable
fun AppCheckboxIndicator(
    checked: Boolean,
    enabled: Boolean = true,
    size: Dp = 24.dp,
    color: Color = MaterialTheme.colorScheme.primary,
    checkColor: Color = MaterialTheme.colorScheme.onPrimary,
) {
    val scheme = MaterialTheme.colorScheme
    val fill by animateColorAsState(if (checked) (if (enabled) color else scheme.outlineVariant) else Color.Transparent)
    val shape = RoundedCornerShape(size / 4)
    Box(
        Modifier.size(size).clip(shape).background(fill)
            .border(if (checked) 0.dp else 2.dp, if (enabled) scheme.outline else scheme.outlineVariant, shape),
        contentAlignment = Alignment.Center,
    ) {
        if (checked) Icon(Icons.Default.Check, null, tint = checkColor, modifier = Modifier.size(size * 0.75f))
    }
}

/** Радио: кольцо; выбранное — толстое кольцо цвета темы с точкой. */
@Composable
fun AppRadioIndicator(
    selected: Boolean,
    enabled: Boolean = true,
    size: Dp = 24.dp,
    color: Color = MaterialTheme.colorScheme.primary,
) {
    val scheme = MaterialTheme.colorScheme
    val ring by animateColorAsState(if (selected) (if (enabled) color else scheme.outlineVariant) else if (enabled) scheme.outline else scheme.outlineVariant)
    val dot by animateDpAsState(if (selected) size * 0.42f else 0.dp)
    Box(Modifier.size(size).clip(CircleShape).border(2.dp, ring, CircleShape), contentAlignment = Alignment.Center) {
        Box(Modifier.size(dot).clip(CircleShape).background(ring))
    }
}

// endregion

// region Чипы-фильтры

/**
 * Чип-фильтр: в форме чипов темы; выбранный залит цветом темы и помечен галочкой (не только цветом).
 * Роль «флажок» и состояние «выбрано» — как у стандартного FilterChip.
 */
@Composable
fun AppFilterChip(selected: Boolean, onClick: () -> Unit, label: String, modifier: Modifier = Modifier, enabled: Boolean = true) {
    val scheme = MaterialTheme.colorScheme
    val container by animateColorAsState(if (selected) scheme.primary else Color.Transparent)
    val content = if (selected) scheme.onPrimary else scheme.onSurface
    Surface(
        shape = Design.tokens.chipShape,
        color = container,
        contentColor = content,
        border = if (selected) null else BorderStroke(1.dp, scheme.outline),
        modifier = modifier.heightIn(min = 48.dp).padding(vertical = 4.dp)
            .clip(Design.tokens.chipShape)
            .selectable(selected = selected, enabled = enabled, role = Role.Checkbox, onClick = onClick),
    ) {
        Row(
            Modifier.heightIn(min = 40.dp).padding(start = if (selected) 10.dp else 16.dp, end = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            if (selected) Icon(Icons.Default.Check, null, modifier = Modifier.size(18.dp))
            Text(label, style = MaterialTheme.typography.labelLarge, maxLines = 1)
        }
    }
}

// endregion

// region Ползунок

/** Ползунок: толстая скруглённая дорожка и круглый бегунок с обводкой (вместо «разрезанной» дорожки Material 3). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    onValueChangeFinished: (() -> Unit)? = null,
    enabled: Boolean = true,
) {
    val scheme = MaterialTheme.colorScheme
    Slider(
        value = value,
        onValueChange = onValueChange,
        onValueChangeFinished = onValueChangeFinished,
        enabled = enabled,
        modifier = modifier,
        thumb = {
            Box(
                Modifier.size(28.dp).clip(CircleShape).background(scheme.surface).border(4.dp, scheme.primary, CircleShape)
            )
        },
        track = { state: SliderState ->
            val fraction = ((state.value - state.valueRange.start) / (state.valueRange.endInclusive - state.valueRange.start)).coerceIn(0f, 1f)
            Box(Modifier.fillMaxWidth().height(10.dp).clip(CircleShape).background(scheme.surfaceContainerHighest)) {
                Box(Modifier.fillMaxWidth(fraction).height(10.dp).clip(CircleShape).background(scheme.primary))
            }
        },
    )
}

// endregion
