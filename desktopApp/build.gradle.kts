import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.compose.compiler)
}

kotlin {
    jvm("desktop")

    sourceSets {
        val desktopMain by getting {
            dependencies {
                implementation(project(":shared"))
                implementation(compose.desktop.currentOs)
                implementation(libs.kotlinx.coroutines.swing)
            }
        }
    }
}

compose.desktop {
    application {
        mainClass = "org.caexplorer.desktop.MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "CA Explorer"
            packageVersion = "1.0.0"
            description = "Cellular Automaton Explorer"

            macOS {
                iconFile.set(project.file("icons/icon.icns"))
                bundleID = "org.caexplorer.desktop"
            }

            windows {
                iconFile.set(project.file("icons/icon.ico"))
                menuGroup = "CA Explorer"
            }

            linux {
                iconFile.set(project.file("icons/icon.png"))
            }
        }
    }
}
