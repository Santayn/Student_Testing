package org.santayn.testing.mobile.core.platform

/** Адрес API по умолчанию для текущей платформы/сборки. */
expect fun defaultServerUrl(): String

/** Название платформы для User-Agent и экрана «О приложении». */
expect val platformName: String
