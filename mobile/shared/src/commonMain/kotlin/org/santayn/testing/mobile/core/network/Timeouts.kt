package org.santayn.testing.mobile.core.network

/** Таймауты запросов (frontend/src/api/timeouts.js). */
object ApiTimeouts {
    const val STANDARD_MS = 15_000L
    const val SUBMIT_ATTEMPT_MS = 180_000L

    /** Передача файлов — без ограничения общего времени. */
    const val FILE_TRANSFER_MS = Long.MAX_VALUE
}
