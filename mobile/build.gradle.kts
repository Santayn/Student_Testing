plugins {
    alias(libs.plugins.androidApplication) apply false
    alias(libs.plugins.androidMultiplatformLibrary) apply false
    alias(libs.plugins.composeCompiler) apply false
    alias(libs.plugins.composeMultiplatform) apply false
    alias(libs.plugins.kotlinMultiplatform) apply false
    alias(libs.plugins.kotlinxSerialization) apply false
}

// Windows + кириллица в пути проекта (например D:\Универ\...): тестовая JVM читает classpath
// в системной кодировке и не находит классы. Выносим build-каталоги в ASCII-путь.
val hasNonAsciiPath = rootDir.absolutePath.any { it.code > 127 }
if (hasNonAsciiPath && System.getProperty("os.name").startsWith("Windows")) {
    val asciiBuildRoot = File(System.getProperty("user.home"), ".student-testing-build")
    allprojects {
        layout.buildDirectory.set(File(asciiBuildRoot, project.name))
    }
}
