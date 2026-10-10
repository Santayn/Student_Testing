package org.santayn.testing.mobile.core.platform

import androidx.compose.runtime.Composable

/** iOS: стиль статус-бара задаёт хост-контроллер; Compose-экран его не меняет. */
@Composable
actual fun LightStatusBarIcons(enabled: Boolean) = Unit
