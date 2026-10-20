package org.santayn.testing.mobile.core.platform

import androidx.compose.runtime.Composable
import org.santayn.testing.mobile.core.network.UploadFile

/**
 * Системный сканер документов: находит страницу в кадре, обрезает и выравнивает её,
 * снимает несколько страниц подряд. Возвращает запуск сканера или null, если на платформе
 * его нет — тогда остаются камера и выбор файлов.
 */
@Composable
expect fun rememberDocumentScanner(
    onResult: (List<UploadFile>) -> Unit,
    onError: (String) -> Unit,
): (() -> Unit)?
