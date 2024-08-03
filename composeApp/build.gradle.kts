import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.targets.js.yarn.YarnLockMismatchReport
import org.jetbrains.kotlin.gradle.targets.js.yarn.YarnRootExtension

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.jetbrainsCompose)
    alias(libs.plugins.compose.compiler)

    kotlin("plugin.serialization") version "2.0.0"
}

kotlin {
    androidTarget {
        @OptIn(ExperimentalKotlinGradlePluginApi::class)
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
        }
    }
    
    jvm("desktop")
    
    listOf(
        iosX64(),
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "Chat Enrichment"
            isStatic = true
        }
    }
    
    sourceSets {
        val desktopMain by getting
        
        androidMain.dependencies {
            implementation(compose.preview)
            implementation(libs.androidx.activity.compose)

            implementation(libs.ktor.client.android)
            implementation(libs.kotlinx.coroutines.android)
        }
        nativeMain.dependencies {
            implementation(libs.ktor.client.darwin)
        }
        jvmMain.dependencies {
            implementation(libs.kotlinx.coroutines.javafx)
        }
        desktopMain.dependencies {
            implementation(compose.desktop.currentOs)

            implementation(libs.ktor.client.apache5)
            implementation(libs.kotlinx.coroutines.swing)
        }

        commonMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.ui)
            implementation(compose.material)
            implementation(compose.material3)
            implementation(compose.components.resources)
            implementation(compose.components.uiToolingPreview)
            implementation(compose.materialIconsExtended)

            implementation(libs.navigation.compose)
            implementation(libs.material3.window.size)

            implementation(libs.coil.compose)
            implementation(libs.coil.network)
            implementation(libs.kotlinx.coroutines)
            implementation(libs.kotlinx.serialization)
            implementation(libs.bundles.ktor.common)

            implementation(libs.lifecycle.runtime)
            implementation(libs.lifecycle.viewmodel)
        }
        desktopMain.dependencies {
            implementation(compose.desktop.currentOs)
        }
    }
}

rootProject.plugins.withType(org.jetbrains.kotlin.gradle.targets.js.yarn.YarnPlugin::class.java) {
    rootProject.the<YarnRootExtension>().yarnLockMismatchReport = YarnLockMismatchReport.WARNING // NONE | FAIL
    rootProject.the<YarnRootExtension>().reportNewYarnLock = false // true
    rootProject.the<YarnRootExtension>().yarnLockAutoReplace = false // true
}

android {
    namespace = "chat.enrichment.eu"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    sourceSets["main"].manifest.srcFile("src/androidMain/AndroidManifest.xml")
    sourceSets["main"].res.srcDirs("src/androidMain/res")
    sourceSets["main"].resources.srcDirs("src/commonMain/resources")

    defaultConfig {
        applicationId = "chat.enrichment.eu"
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        versionCode = 1
        versionName = "1.0"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
            setProguardFiles(listOf(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro"))
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
        compose = true
    }
    dependencies {
        debugImplementation(compose.uiTooling)
    }
}

compose.desktop {
    application {
        mainClass = "MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "chat.enrichment.eu"
            packageVersion = "1.0.0"
        }
    }
}

kotlin.sourceSets.all {
    languageSettings.optIn("kotlin.experimental.ExperimentalObjCName")
}