package org.santayn.testing.mobile.core.platform

import androidx.compose.runtime.Composable
import org.santayn.testing.mobile.core.network.UploadFile

/** На iPhone страницы снимаются камерой или выбираются из «Фото»/«Файлов» (FileKit). */
@Composable
actual fun rememberDocumentScanner(
    onResult: (List<UploadFile>) -> Unit,
    onError: (String) -> Unit,
): (() -> Unit)? = null
