package org.santayn.testing.mobile.core.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.backhandler.BackHandler

/**
 * Защита экрана с несохранёнными изменениями: системная «Назад» и возвращаемое действие
 * (для стрелки в шапке) сначала спрашивают, бросить ли изменения. Тексты — как в вебе.
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun rememberUnsavedChangesGuard(
    dirty: Boolean,
    onLeave: () -> Unit,
    title: String = "Есть несохранённые изменения",
    message: String = "Если уйти сейчас, внесённые изменения будут потеряны.",
    leaveText: String = "Уйти без сохранения",
): () -> Unit {
    val leave by rememberUpdatedState(onLeave)
    var confirm by remember { mutableStateOf(false) }
    BackHandler(enabled = dirty) { confirm = true }
    if (confirm) {
        ConfirmDialog(
            title = title,
            message = message,
            confirmText = leaveText,
            dismissText = "Продолжить редактирование",
            destructive = true,
            onConfirm = {
                confirm = false
                leave()
            },
            onDismiss = { confirm = false },
        )
    }
    return { if (dirty) confirm = true else leave() }
}
