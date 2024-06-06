plugins {
    //alias(libs.plugins.android.application)
    id("com.android.library")
    alias(libs.plugins.jetbrains.kotlin.android)
}

android {
    namespace = "com.squadris.squadris"
    compileSdk = 34

    packaging.run {
        resources.excludes.add("META-INF/gradle/incremental.annotation.processors")
    }

    defaultConfig {
        minSdk = 24
        targetSdk = 34

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
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
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)

    //Dagger, Hilt
    implementation(libs.hilt.android)
    implementation(libs.hilt.compiler)
    implementation(libs.kotlinx.metadata.jvm)

    implementation(libs.androidx.navigation.compose)
    implementation(libs.coil.compose)

    //Jetpack compose
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.material)
    implementation(libs.androidx.ui.tooling.preview.android)
    implementation(libs.material3)
    implementation(libs.material.icons)
    implementation(libs.lottie)
    implementation(libs.androidx.constraintlayout.compose)
    implementation(libs.material3.window.size)
    implementation(libs.androidx.multidex)
}