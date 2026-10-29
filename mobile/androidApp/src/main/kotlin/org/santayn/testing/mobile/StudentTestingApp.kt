package org.santayn.testing.mobile

import android.app.Application
import org.koin.android.ext.koin.androidContext
import org.santayn.testing.mobile.di.initKoin

class StudentTestingApp : Application() {
    override fun onCreate() {
        super.onCreate()
        initKoin { androidContext(this@StudentTestingApp) }
    }
}
