
import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.NativeBuildType
import org.jetbrains.kotlin.gradle.targets.js.yarn.YarnLockMismatchReport
import org.jetbrains.kotlin.gradle.targets.js.yarn.YarnRootExtension
import java.io.FileInputStream
import java.util.Properties

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.jetbrainsCompose)
    alias(libs.plugins.compose.compiler)

    id("com.google.gms.google-services")
    id("com.github.gmazzo.buildconfig") version "5.4.0"

    kotlin("plugin.serialization") version "2.0.20"
    kotlin("native.cocoapods") version "2.0.20"
}

kotlin {
    androidTarget {
        @OptIn(ExperimentalKotlinGradlePluginApi::class)
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
        }
    }
    @OptIn(ExperimentalKotlinGradlePluginApi::class)
    compilerOptions {
        freeCompilerArgs.add("-Xexpect-actual-classes")
    }

    jvm()
    iosX64()
    iosArm64()
    iosSimulatorArm64()

    cocoapods {
        version = libs.versions.version.name.get()
        summary = "Some description for a Kotlin/Native module"
        homepage = "Link to a Kotlin/Native module homepage"

        // Optional properties
        // Configure the Pod name here instead of changing the Gradle project name
        name = "ComposeApp"

        podfile = project.file("../iosApp/Podfile")
        ios.deploymentTarget = "14.0"

        pod("GoogleSignIn") { extraOpts += listOf("-compiler-option", "-fmodules") }
        pod("FirebaseCore") { extraOpts += listOf("-compiler-option", "-fmodules") }
        pod("FirebaseAuth") { extraOpts += listOf("-compiler-option", "-fmodules") }
        pod("FirebaseMessaging") { extraOpts += listOf("-compiler-option", "-fmodules") }

        framework {
            // Required properties
            // Framework name configuration. Use this property instead of deprecated 'frameworkName'
            baseName = "ComposeApp"

            // Optional properties
            // Specify the framework linking type. It's dynamic by default.
            isStatic = true

            binaryOption("bundleId", "chat.enrichment.eu")
            binaryOption("bundleVersion", "1")
        }

        // Maps custom Xcode configuration to NativeBuildType
        xcodeConfigurationToNativeBuildType["CUSTOM_DEBUG"] = NativeBuildType.DEBUG
        xcodeConfigurationToNativeBuildType["CUSTOM_RELEASE"] = NativeBuildType.RELEASE
    }
    
    sourceSets {
        androidMain.dependencies {
            implementation(compose.preview)
            implementation(libs.androidx.activity.compose)

            implementation(libs.ktor.client.okhttp)
            implementation(libs.kotlinx.coroutines.android)

            implementation(libs.koin.android)

            implementation(libs.androidx.splashscreen)

            //Credentials
            implementation(libs.androidx.credentials)
            implementation(libs.androidx.credentials.auth)
            implementation(libs.google.identity)
        }
        nativeMain.dependencies {
            implementation(libs.ktor.client.darwin)
        }
        jvmMain.dependencies {
            implementation(compose.desktop.currentOs)
            implementation(libs.firebase.java.sdk)

            implementation(libs.ktor.client.java)
            implementation(libs.kotlinx.coroutines.swing)
        }

        commonMain.dependencies {
            //put your multiplatform dependencies here
            implementation(project(":shared"))
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.ui)
            implementation(compose.material)
            implementation(compose.material3)
            implementation(compose.components.resources)
            implementation(compose.components.uiToolingPreview)
            implementation(compose.materialIconsExtended)

            implementation(libs.compottie.resources)
            implementation(libs.navigation.compose)
            implementation(libs.material3.window.size)

            api(libs.koin.core)
            implementation(libs.koin.compose)
            implementation(libs.koin.compose.view.model)

            implementation(libs.settings.no.arg)

            implementation(libs.kotlin.crypto.sha2)
            implementation(libs.kotlinx.datetime)
            implementation(libs.kotlinx.coroutines)
            implementation(libs.kotlinx.serialization)
            implementation(libs.bundles.ktor.common)
            implementation(libs.firebase.gitlive.auth)
            implementation(libs.firebase.gitlive.messaging)
            implementation(libs.firebase.gitlive.common)

            implementation(libs.coil)
            implementation(libs.coil.compose)
            implementation(libs.coil.compose.core)
            implementation(libs.coil.network.ktor)

            implementation(libs.lifecycle.runtime)
            implementation(libs.lifecycle.viewmodel)
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
        versionName = libs.versions.version.name.get()
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }

    val keystoreProperties = Properties()
    keystoreProperties.load(FileInputStream(rootProject.file("local.properties")))

    signingConfigs {
        getByName("debug") {
            keyAlias = "debug"
            keyPassword = keystoreProperties["keyPassword"] as String
            storeFile = file(keystoreProperties["storeFile"] as String)
            storePassword = keystoreProperties["storePassword"] as String
        }
        create("release") {
            keyAlias = "release"
            keyPassword = keystoreProperties["keyPassword"] as String
            storeFile = file(keystoreProperties["storeFile"] as String)
            storePassword = keystoreProperties["storePassword"] as String
        }
    }

    buildTypes {
        var isRelease = false

        debug {
            isMinifyEnabled = false
            isShrinkResources = false
            applicationIdSuffix = ".test"
            signingConfig = signingConfigs.getByName("debug")
        }
        release {
            isRelease = true
            isMinifyEnabled = true
            isShrinkResources = true
            setProguardFiles(listOf(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro"))
            signingConfig = signingConfigs.getByName("release")
        }

        buildConfig {
            className("SharedBuildConfig")
            packageName("chat.enrichment.eu")
            useKotlinOutput {
                internalVisibility = true
            }

            buildConfigField("CloudWebApiKey", keystoreProperties["cloudWebApiKey"] as String)
            buildConfigField("FirebaseProjectId", keystoreProperties["firebaseProjectId"] as String)
            buildConfigField(
                "AndroidAppId",
                keystoreProperties[if(isRelease) "androidReleaseAppId" else "androidDebugAppId"] as String
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
        compose = true
        buildConfig = false
    }
    dependencies {
        debugImplementation(compose.uiTooling)

    }
}
dependencies {
    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.androidx.activity.ktx)
}

compose.desktop {
    application {
        mainClass = "MainKt"

        buildTypes.release.proguard {
            version.set("7.4.0")
            configurationFiles.from(project.file("proguard-rules.pro"))
        }

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageVersion = libs.versions.version.name.get()
            packageName = "Chatrich"

            macOS {
                appStore = true
                iconFile.set(project.file("${project.projectDir}/src/nativeMain/resources/drawable/app_icon.icns"))
            }
            windows {
                iconFile.set(project.file("${project.projectDir}/src/jvmMain/resources/drawable/app_icon.ico"))
            }
            linux {
                iconFile.set(project.file("${project.projectDir}/src/jvmMain/resources/drawable/app_icon.png"))
            }
        }
    }
}

kotlin.sourceSets.all {
    languageSettings.optIn("kotlin.experimental.ExperimentalObjCName")
}

tasks.register("printVersionName") {
    doLast {
        println(libs.versions.version.name.get())
    }
}