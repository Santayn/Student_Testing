package org.santayn.testing.mobile.di

import org.koin.core.context.startKoin
import org.koin.dsl.KoinAppDeclaration

/** Запуск DI. Android передаёт androidContext, iOS вызывает без параметров. */
fun initKoin(appDeclaration: KoinAppDeclaration = {}) {
    startKoin {
        appDeclaration()
        modules(appModules())
    }
}
