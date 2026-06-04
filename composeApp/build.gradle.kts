import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
}

kotlin {
    jvmToolchain(17)
    jvm()
    
    sourceSets {
        commonMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material)
            implementation(compose.materialIconsExtended) // Added for extended icons
            implementation(compose.material3)
            implementation(compose.ui)
            implementation(compose.components.resources)
            implementation(compose.components.uiToolingPreview)
            implementation(libs.androidx.lifecycle.viewmodelCompose)
            implementation(libs.androidx.lifecycle.runtimeCompose)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
        jvmMain.dependencies {
            implementation(compose.desktop.currentOs)
            implementation(libs.kotlinx.coroutinesSwing)
            implementation(libs.firebase.admin)
            implementation(libs.webcam.capture)
            implementation("org.openpnp:opencv:4.9.0-0")
            implementation(libs.zxing.core)
            implementation(libs.zxing.javase)
        }
    }
}


compose.desktop {
    application {
        mainClass = "org.bkkipmsemarang.kkp_scanner.MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "KKPScanner"
            packageVersion = "1.0.0"
            description = "Scanner KKP Application"
            copyright = "© 2024 Kementerian Kelautan dan Perikanan"
            
            windows {
                iconFile.set(project.file("src/jvmMain/resources/icon.ico"))
                shortcut = true
                menu = true
                menuGroup = "Kementerian Kelautan dan Perikanan"
            }
        }
    }
}
