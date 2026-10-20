package org.santayn.testing.mobile.core.platform

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions
import com.google.mlkit.vision.documentscanner.GmsDocumentScanning
import com.google.mlkit.vision.documentscanner.GmsDocumentScanningResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.santayn.testing.mobile.core.network.UploadFile

/** ML Kit Document Scanner (Google Play services): интерфейс съёмки предоставляет система. */
@Composable
actual fun rememberDocumentScanner(
    onResult: (List<UploadFile>) -> Unit,
    onError: (String) -> Unit,
): (() -> Unit)? {
    val context = LocalContext.current
    val activity = context.findActivity() ?: return null
    val scope = rememberCoroutineScope()
    val scanner = remember {
        GmsDocumentScanning.getClient(
            GmsDocumentScannerOptions.Builder()
                .setGalleryImportAllowed(true)
                .setPageLimit(MAX_PAGES)
                .setResultFormats(GmsDocumentScannerOptions.RESULT_FORMAT_JPEG)
                .setScannerMode(GmsDocumentScannerOptions.SCANNER_MODE_FULL)
                .build(),
        )
    }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
        if (result.resultCode != Activity.RESULT_OK) return@rememberLauncherForActivityResult
        val pages = GmsDocumentScanningResult.fromActivityResultIntent(result.data)?.pages.orEmpty()
        scope.launch {
            val files = runCatching {
                withContext(Dispatchers.IO) {
                    pages.mapIndexed { index, page ->
                        val bytes = context.contentResolver.openInputStream(page.imageUri)?.use { it.readBytes() }
                            ?: error("Страница ${index + 1} не прочиталась")
                        UploadFile(name = "scan-${index + 1}.jpg", bytes = bytes, contentType = "image/jpeg")
                    }
                }
            }
            files.onSuccess(onResult).onFailure { onError(it.message ?: "Не удалось прочитать сканы") }
        }
    }
    return {
        scanner.getStartScanIntent(activity)
            .addOnSuccessListener { sender -> launcher.launch(IntentSenderRequest.Builder(sender).build()) }
            .addOnFailureListener { onError("Сканер документов недоступен: нужны сервисы Google Play.") }
    }
}

private const val MAX_PAGES = 40

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
