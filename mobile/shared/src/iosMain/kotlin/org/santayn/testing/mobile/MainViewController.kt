package org.santayn.testing.mobile

import androidx.compose.ui.window.ComposeUIViewController
import org.santayn.testing.mobile.di.initKoin
import platform.UIKit.UIViewController

/** Вызывается из Swift один раз при запуске (iOSApp.init). */
fun doInitKoin() = initKoin()

fun MainViewController(): UIViewController = ComposeUIViewController { App() }
