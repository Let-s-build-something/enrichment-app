import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.jetbrainsCompose)
    alias(libs.plugins.compose.compiler)

    kotlin("native.cocoapods") version "2.0.20"
}

kotlin {
    androidTarget {
        // Android
        android.compileOptions.sourceCompatibility = JavaVersion.VERSION_17

        // Java/Kotlin
        java.sourceCompatibility = JavaVersion.VERSION_17

        @OptIn(ExperimentalKotlinGradlePluginApi::class)
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
        }
    }

    jvm()
    iosX64()
    iosArm64()
    iosSimulatorArm64()

    cocoapods {
        summary = "Some description for the Shared Module"
        homepage = "Link to the Shared Module homepage"
        version = "1.0"
        ios.deploymentTarget = "14.0"

        framework {
            baseName = "shared"
            isStatic = true
        }
    }
    
    sourceSets {
        commonMain.dependencies {
            //put your multiplatform dependencies here
            implementation(libs.navigation.compose)
            implementation(libs.material3.window.size)
            implementation(compose.ui)
            implementation(compose.material)
            implementation(compose.material3)
            implementation(compose.components.resources)
            implementation(compose.materialIconsExtended)
            implementation(compose.components.uiToolingPreview)
            implementation(libs.coil.compose)
        }
    }
}

android {
    namespace = "augmy.interactive.shared"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        minSdk = libs.versions.android.minSdk.get().toInt()
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}