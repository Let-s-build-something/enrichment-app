plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.jetbrains.kotlin.android)
    alias(libs.plugins.gms.google.services)
    alias(libs.plugins.compose.compiler)
    id("com.google.dagger.hilt.android")
    id("org.jetbrains.kotlin.kapt")

    kotlin("plugin.serialization") version "2.0.0"
}

android {
    namespace = "lets.build.chatenrichment"
    compileSdk = 34

    packaging.run {
        resources.excludes.addAll(
            listOf(
                "META-INF/gradle/incremental.annotation.processors",
                "META-INF/DEPENDENCIES",
                "META-INF/LICENSE",
                "META-INF/LICENSE.txt",
                "META-INF/license.txt",
                "META-INF/NOTICE",
                "META-INF/NOTICE.txt",
                "META-INF/notice.txt",
                "META-INF/ASL2.0",
                "META-INF/*",
                "META-INF/*.kotlin_module"
            )
        )
    }

    defaultConfig {
        applicationId = "lets.build.chatenrichment"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        multiDexEnabled = true

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    buildTypes {
        debug {
            isMinifyEnabled = false
            isShrinkResources = false
            applicationIdSuffix = ".test"
        }
        release {
            // Enables code shrinking, obfuscation, and optimization for only
            // your project's release build type.
            isMinifyEnabled = true

            // Enables resource shrinking, which is performed by the
            // Android Gradle plugin.
            isShrinkResources = true

            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )

            signingConfig = signingConfigs["debug"]
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
    buildFeatures {
        compose = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.1"
    }
    // Allow references to generated code
    kapt {
        correctErrorTypes = true
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    implementation(project(":Squadris"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)

    // Firebase
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.auth)
    implementation(libs.firebase.play.services)
    implementation(libs.firebase.firestore.ktx)

    //Credentials
    implementation(libs.androidx.credentials)
    implementation(libs.androidx.credentials.auth)
    implementation(libs.google.identity)

    //Compose
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material)
    implementation(libs.androidx.material3)
    implementation(libs.material3.window.size)
    implementation(libs.material.icons)

    //Dagger, Hilt, Navigation, ViewModel
    implementation(libs.hilt.android)
    kapt(libs.hilt.compiler)
    implementation(libs.androidx.hilt.navigation)
    implementation(libs.kotlinx.metadata.jvm)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.viewmodel.compose)

    implementation(libs.coil.compose)
    implementation(libs.coil.gif)
    implementation(libs.coil.svg)
    implementation(libs.lottie)

    implementation(libs.kotlinx.serialization.json)
    implementation(libs.androidx.multidex)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)

    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}