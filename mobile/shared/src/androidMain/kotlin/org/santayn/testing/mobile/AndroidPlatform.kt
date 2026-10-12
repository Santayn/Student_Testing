package org.santayn.testing.mobile

import androidx.activity.ComponentActivity
import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.dialogs.init

/** Вызывается из MainActivity.onCreate: регистрирует launcher'ы диалогов выбора файлов. */
fun initPlatform(activity: ComponentActivity) {
    FileKit.init(activity)
}
