package org.santayn.testing.mobile.core.platform

import platform.UIKit.UIDevice

/**
 * Симулятор на Mac ходит на localhost Mac'а. Бэкенд обычно запущен на другой машине —
 * адрес меняется на экране входа («Сервер»).
 */
actual fun defaultServerUrl(): String = "http://localhost:8080/api/v1"

actual val platformName: String =
    "${UIDevice.currentDevice.systemName} ${UIDevice.currentDevice.systemVersion}"
