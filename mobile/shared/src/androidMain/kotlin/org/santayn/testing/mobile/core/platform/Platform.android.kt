package org.santayn.testing.mobile.core.platform

/** 10.0.2.2 — адрес хост-машины из эмулятора Android. */
actual fun defaultServerUrl(): String = "http://10.0.2.2:8080/api/v1"

actual val platformName: String = "Android ${android.os.Build.VERSION.RELEASE}"
