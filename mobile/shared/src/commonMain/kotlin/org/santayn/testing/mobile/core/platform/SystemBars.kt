package org.santayn.testing.mobile.core.platform

import androidx.compose.runtime.Composable

/**
 * Светлые значки статус-бара, пока экран показан (над тёмной/цветной фирменной панелью).
 * При уходе с экрана возвращается обычный вид по теме.
 */
@Composable
expect fun LightStatusBarIcons(enabled: Boolean = true)
