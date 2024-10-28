
import com.codingfeline.buildkonfig.compiler.FieldSpec.Type.STRING
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

    kotlin("plugin.serialization") version libs.versions.kotlin
    kotlin("native.cocoapods") version libs.versions.kotlin
    id("com.codingfeline.buildkonfig") version "0.15.2"
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

            binaryOption("bundleId", "augmy.interactive.com")
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
            implementation(compose.preview)

            implementation(libs.ktor.client.okhttp)
            implementation(libs.kotlinx.coroutines.android)

            implementation(libs.koin.android)

            implementation(libs.androidx.splashscreen)

            //Credentials
            implementation(libs.androidx.credentials)
            implementation(libs.androidx.credentials.auth)
            implementation(libs.google.identity)

            //Firebase
            implementation(libs.android.firebase.common)
            implementation(libs.android.firebase.auth)
            implementation(libs.android.firebase.messaging)
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
            implementation(project(":shared"))
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.ui)
            implementation(compose.material)
            implementation(compose.material3)
            implementation(compose.components.resources)
            implementation(compose.components.uiToolingPreview)
            implementation(compose.materialIconsExtended)

            implementation(libs.compottie.dot)
            implementation(libs.navigation.compose)
            implementation(libs.material3.window.size)
            implementation(libs.compose.file.kit)

            api(libs.koin.core)
            implementation(libs.koin.compose)
            implementation(libs.koin.compose.view.model)

            implementation(libs.settings.no.arg)

            implementation(libs.kotlin.crypto.sha2)
            implementation(libs.kotlinx.datetime)
            implementation(libs.kotlinx.coroutines)
            implementation(libs.kotlinx.serialization)
            implementation(libs.bundles.ktor.common)
            implementation(libs.firebase.gitlive.common)
            implementation(libs.firebase.gitlive.auth)
            implementation(libs.firebase.gitlive.messaging)
            implementation(libs.firebase.gitlive.storage)

            implementation(libs.coil)
            implementation(libs.coil.svg)
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

val vCode = libs.versions.version.code.get().toInt()
val vName = "${libs.versions.version.name.get()}.$vCode"
val debugHostname = "api.fly-here.com/api"
val releaseHostname = "api.fly-here.com/api"

android {
    namespace = "augmy.interactive.com"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    sourceSets["main"].manifest.srcFile("src/androidMain/AndroidManifest.xml")
    sourceSets["main"].res.srcDirs("src/androidMain/res")
    sourceSets["main"].resources.srcDirs("src/commonMain/resources")

    defaultConfig {
        applicationId = "augmy.interactive.com"
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        versionCode = vCode
        versionName = vName
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }

    val keystoreProperties = Properties()
    keystoreProperties.load(FileInputStream(rootProject.file("local.properties")))

    signingConfigs {
        create("dev") {
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
        debug {
            isMinifyEnabled = false
            isShrinkResources = false
            isDebuggable = true
            applicationIdSuffix = ".test"
            signingConfig = signingConfigs.getByName("debug")
            manifestPlaceholders["hostName"] = debugHostname
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            isDebuggable = false
            setProguardFiles(listOf(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro"))
            signingConfig = signingConfigs.getByName("release")
            manifestPlaceholders["hostName"] = releaseHostname
        }
    }

    dependencies {
        debugImplementation(compose.uiTooling)
    }
}
dependencies {
    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.androidx.activity.ktx)
}

//java.toolchain.languageVersion = JavaLanguageVersion.of(libs.versions.java.get())

compose.desktop {
    application {
        mainClass = "MainKt"

        buildTypes.release.proguard {
            //version.set("7.5.0")
            configurationFiles.from(project.file("proguard-rules.pro"))
        }

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageVersion = vName
            packageName = "Augmy"

            macOS {
                appStore = true
                iconFile.set(project.file("${project.projectDir}/src/nativeMain/resources/drawable/app_icon.icns"))
            }
            windows {
                modules("java.instrument", "java.management", "java.naming", "java.sql", "jdk.unsupported")
                menuGroup = "Augmy Interactive"
                shortcut = true
                iconFile.set(project.file("${project.projectDir}/src/jvmMain/resources/drawable/app_icon.ico"))
            }
            linux {
                modules("java.instrument", "java.management", "java.naming", "java.sql", "jdk.unsupported", "jdk.security.auth")
                menuGroup = "Augmy Interactive"
                iconFile.set(project.file("${project.projectDir}/src/jvmMain/resources/drawable/app_icon.png"))
            }
        }
    }
}

buildkonfig {
    packageName = "augmy.interactive.com"

    val keystoreProperties = Properties()
    keystoreProperties.load(FileInputStream(rootProject.file("local.properties")))

    // this is the production setting
    defaultConfigs {
        buildConfigField(STRING, "CloudWebApiKey", keystoreProperties["cloudWebApiKey"] as String)
        buildConfigField(STRING, "FirebaseProjectId", keystoreProperties["firebaseProjectId"] as String)

        buildConfigField(STRING, "HttpsHostName", releaseHostname)
        buildConfigField(STRING, "AndroidAppId", keystoreProperties["androidReleaseAppId"] as String)
        buildConfigField(STRING, "StorageBucketName", "chat-enrichment.appspot.com")
    }

    // change the setting just for development
    defaultConfigs("development") {
        buildConfigField(STRING, "HttpsHostName", debugHostname)
        buildConfigField(STRING, "AndroidAppId", keystoreProperties["androidDebugAppId"] as String)
    }
}

kotlin.sourceSets.all {
    languageSettings.optIn("kotlin.experimental.ExperimentalObjCName")
}

tasks.register("printVersionName") {
    doLast {
        println(vName)
    }
}
tasks.register("syncWithGradleFiles") {
    dependsOn(
        ":commonizeNativeDistribution",
        ":commonize",
        ":prepareKotlinIdeaImport",
        ":composeApp:generateDefFirebaseAuth",
        ":composeApp:xcodeVersion",
        ":composeApp:podGenIos",
        ":composeApp:podInstallSyntheticIos",
        ":composeApp:podSetupBuildFirebaseAuthIos",
        ":composeApp:podBuildFirebaseAuthIos",
        ":composeApp:cinteropFirebaseAuthIosArm64",
        ":composeApp:podSetupBuildFirebaseAuthIosSimulator",
        ":composeApp:podBuildFirebaseAuthIosSimulator",
        ":composeApp:cinteropFirebaseAuthIosSimulatorArm64",
        ":composeApp:cinteropFirebaseAuthIosX64",
        ":composeApp:generateDefFirebaseCore",
        ":composeApp:podSetupBuildFirebaseCoreIos",
        ":composeApp:podBuildFirebaseCoreIos",
        ":composeApp:cinteropFirebaseCoreIosArm64",
        ":composeApp:podSetupBuildFirebaseCoreIosSimulator",
        ":composeApp:podBuildFirebaseCoreIosSimulator",
        ":composeApp:cinteropFirebaseCoreIosSimulatorArm64",
        ":composeApp:cinteropFirebaseCoreIosX64",
        ":composeApp:generateDefFirebaseMessaging",
        ":composeApp:podSetupBuildFirebaseMessagingIos",
        ":composeApp:podBuildFirebaseMessagingIos",
        ":composeApp:cinteropFirebaseMessagingIosArm64",
        ":composeApp:podSetupBuildFirebaseMessagingIosSimulator",
        ":composeApp:podBuildFirebaseMessagingIosSimulator",
        ":composeApp:cinteropFirebaseMessagingIosSimulatorArm64",
        ":composeApp:cinteropFirebaseMessagingIosX64",
        ":composeApp:generateDefGoogleSignIn",
        ":composeApp:podSetupBuildGoogleSignInIos",
        ":composeApp:podBuildGoogleSignInIos",
        ":composeApp:cinteropGoogleSignInIosArm64",
        ":composeApp:podSetupBuildGoogleSignInIosSimulator",
        ":composeApp:podBuildGoogleSignInIosSimulator",
        ":composeApp:cinteropGoogleSignInIosSimulatorArm64",
        ":composeApp:cinteropGoogleSignInIosX64",
        ":composeApp:commonizeCInterop",
        ":composeApp:copyCommonizeCInteropForIde",
        ":composeApp:commonize",
        ":composeApp:convertXmlValueResourcesForAndroidDebug",
        ":composeApp:convertXmlValueResourcesForAndroidInstrumentedTest",
        ":composeApp:convertXmlValueResourcesForAndroidInstrumentedTestDebug",
        ":composeApp:convertXmlValueResourcesForAndroidMain",
        ":composeApp:convertXmlValueResourcesForAndroidRelease",
        ":composeApp:convertXmlValueResourcesForAndroidUnitTest",
        ":composeApp:convertXmlValueResourcesForAndroidUnitTestDebug",
        ":composeApp:convertXmlValueResourcesForAndroidUnitTestRelease",
        ":composeApp:convertXmlValueResourcesForAppleMain",
        ":composeApp:convertXmlValueResourcesForAppleTest",
        ":composeApp:convertXmlValueResourcesForCommonMain",
        ":composeApp:convertXmlValueResourcesForCommonTest",
        ":composeApp:convertXmlValueResourcesForIosArm64Main",
        ":composeApp:convertXmlValueResourcesForIosArm64Test",
        ":composeApp:convertXmlValueResourcesForIosMain",
        ":composeApp:convertXmlValueResourcesForIosSimulatorArm64Main",
        ":composeApp:convertXmlValueResourcesForIosSimulatorArm64Test",
        ":composeApp:convertXmlValueResourcesForIosTest",
        ":composeApp:convertXmlValueResourcesForIosX64Main",
        ":composeApp:convertXmlValueResourcesForIosX64Test",
        ":composeApp:convertXmlValueResourcesForJvmMain",
        ":composeApp:convertXmlValueResourcesForJvmTest",
        ":composeApp:convertXmlValueResourcesForNativeMain",
        ":composeApp:convertXmlValueResourcesForNativeTest",
        ":composeApp:copyNonXmlValueResourcesForAndroidDebug",
        ":composeApp:copyNonXmlValueResourcesForAndroidInstrumentedTest",
        ":composeApp:copyNonXmlValueResourcesForAndroidInstrumentedTestDebug",
        ":composeApp:copyNonXmlValueResourcesForAndroidMain",
        ":composeApp:copyNonXmlValueResourcesForAndroidRelease",
        ":composeApp:copyNonXmlValueResourcesForAndroidUnitTest",
        ":composeApp:copyNonXmlValueResourcesForAndroidUnitTestDebug",
        ":composeApp:copyNonXmlValueResourcesForAndroidUnitTestRelease",
        ":composeApp:copyNonXmlValueResourcesForAppleMain",
        ":composeApp:copyNonXmlValueResourcesForAppleTest",
        ":composeApp:copyNonXmlValueResourcesForCommonMain",
        ":composeApp:copyNonXmlValueResourcesForCommonTest",
        ":composeApp:copyNonXmlValueResourcesForIosArm64Main",
        ":composeApp:copyNonXmlValueResourcesForIosArm64Test",
        ":composeApp:copyNonXmlValueResourcesForIosMain",
        ":composeApp:copyNonXmlValueResourcesForIosSimulatorArm64Main",
        ":composeApp:copyNonXmlValueResourcesForIosSimulatorArm64Test",
        ":composeApp:copyNonXmlValueResourcesForIosTest",
        ":composeApp:copyNonXmlValueResourcesForIosX64Main",
        ":composeApp:copyNonXmlValueResourcesForIosX64Test",
        ":composeApp:copyNonXmlValueResourcesForJvmMain",
        ":composeApp:copyNonXmlValueResourcesForJvmTest",
        ":composeApp:copyNonXmlValueResourcesForNativeMain",
        ":composeApp:copyNonXmlValueResourcesForNativeTest",
        ":composeApp:prepareComposeResourcesTaskForAndroidMain",
        ":composeApp:generateResourceAccessorsForAndroidMain",
        ":composeApp:prepareComposeResourcesTaskForCommonMain",
        ":composeApp:generateResourceAccessorsForCommonMain",
        ":composeApp:generateActualResourceCollectorsForAndroidMain",
        ":composeApp:prepareComposeResourcesTaskForAppleMain",
        ":composeApp:generateResourceAccessorsForAppleMain",
        ":composeApp:prepareComposeResourcesTaskForIosArm64Main",
        ":composeApp:generateResourceAccessorsForIosArm64Main",
        ":composeApp:prepareComposeResourcesTaskForIosMain",
        ":composeApp:generateResourceAccessorsForIosMain",
        ":composeApp:prepareComposeResourcesTaskForNativeMain",
        ":composeApp:generateResourceAccessorsForNativeMain",
        ":composeApp:generateActualResourceCollectorsForIosArm64Main",
        ":composeApp:prepareComposeResourcesTaskForIosSimulatorArm64Main",
        ":composeApp:generateResourceAccessorsForIosSimulatorArm64Main",
        ":composeApp:generateActualResourceCollectorsForIosSimulatorArm64Main",
        ":composeApp:prepareComposeResourcesTaskForIosX64Main",
        ":composeApp:generateResourceAccessorsForIosX64Main",
        ":composeApp:generateActualResourceCollectorsForIosX64Main",
        ":composeApp:prepareComposeResourcesTaskForJvmMain",
        ":composeApp:generateResourceAccessorsForJvmMain",
        ":composeApp:generateActualResourceCollectorsForJvmMain",
        ":composeApp:generateComposeResClass",
        ":composeApp:generateExpectResourceCollectorsForCommonMain",
        ":composeApp:prepareComposeResourcesTaskForAndroidDebug",
        ":composeApp:generateResourceAccessorsForAndroidDebug",
        ":composeApp:prepareComposeResourcesTaskForAndroidInstrumentedTest",
        ":composeApp:generateResourceAccessorsForAndroidInstrumentedTest",
        ":composeApp:prepareComposeResourcesTaskForAndroidInstrumentedTestDebug",
        ":composeApp:generateResourceAccessorsForAndroidInstrumentedTestDebug",
        ":composeApp:prepareComposeResourcesTaskForAndroidRelease",
        ":composeApp:generateResourceAccessorsForAndroidRelease",
        ":composeApp:prepareComposeResourcesTaskForAndroidUnitTest",
        ":composeApp:generateResourceAccessorsForAndroidUnitTest",
        ":composeApp:prepareComposeResourcesTaskForAndroidUnitTestDebug",
        ":composeApp:generateResourceAccessorsForAndroidUnitTestDebug",
        ":composeApp:prepareComposeResourcesTaskForAndroidUnitTestRelease",
        ":composeApp:generateResourceAccessorsForAndroidUnitTestRelease",
        ":composeApp:prepareComposeResourcesTaskForAppleTest",
        ":composeApp:generateResourceAccessorsForAppleTest",
        ":composeApp:prepareComposeResourcesTaskForCommonTest",
        ":composeApp:generateResourceAccessorsForCommonTest",
        ":composeApp:prepareComposeResourcesTaskForIosArm64Test",
        ":composeApp:generateResourceAccessorsForIosArm64Test",
        ":composeApp:prepareComposeResourcesTaskForIosSimulatorArm64Test",
        ":composeApp:generateResourceAccessorsForIosSimulatorArm64Test",
        ":composeApp:prepareComposeResourcesTaskForIosTest",
        ":composeApp:generateResourceAccessorsForIosTest",
        ":composeApp:prepareComposeResourcesTaskForIosX64Test",
        ":composeApp:generateResourceAccessorsForIosX64Test",
        ":composeApp:prepareComposeResourcesTaskForJvmTest",
        ":composeApp:generateResourceAccessorsForJvmTest",
        ":composeApp:prepareComposeResourcesTaskForNativeTest",
        ":composeApp:generateResourceAccessorsForNativeTest",
        ":composeApp:iosArm64Cinterop-FirebaseAuthKlib",
        ":composeApp:iosArm64Cinterop-FirebaseCoreKlib",
        ":composeApp:iosArm64Cinterop-FirebaseMessagingKlib",
        ":composeApp:iosArm64Cinterop-GoogleSignInKlib",
        ":composeApp:iosSimulatorArm64Cinterop-FirebaseAuthKlib",
        ":composeApp:iosSimulatorArm64Cinterop-FirebaseCoreKlib",
        ":composeApp:iosSimulatorArm64Cinterop-FirebaseMessagingKlib",
        ":composeApp:iosSimulatorArm64Cinterop-GoogleSignInKlib",
        ":composeApp:iosX64Cinterop-FirebaseAuthKlib",
        ":composeApp:iosX64Cinterop-FirebaseCoreKlib",
        ":composeApp:iosX64Cinterop-FirebaseMessagingKlib",
        ":composeApp:iosX64Cinterop-GoogleSignInKlib",
        ":composeApp:generateDummyFramework",
        ":composeApp:podspec",
        ":composeApp:podInstall",
        ":composeApp:podImport",
        ":composeApp:transformNativeMainCInteropDependenciesMetadataForIde",
        ":composeApp:transformAppleMainCInteropDependenciesMetadataForIde",
        ":composeApp:transformIosMainCInteropDependenciesMetadataForIde",
        ":composeApp:transformNativeTestCInteropDependenciesMetadataForIde",
        ":composeApp:transformAppleTestCInteropDependenciesMetadataForIde",
        ":composeApp:transformIosTestCInteropDependenciesMetadataForIde",
        ":composeApp:prepareKotlinIdeaImport",
        ":shared:commonizeCInterop",
        ":shared:commonize",
        ":shared:convertXmlValueResourcesForAndroidDebug",
        ":shared:convertXmlValueResourcesForAndroidInstrumentedTest",
        ":shared:convertXmlValueResourcesForAndroidInstrumentedTestDebug",
        ":shared:convertXmlValueResourcesForAndroidMain",
        ":shared:convertXmlValueResourcesForAndroidRelease",
        ":shared:convertXmlValueResourcesForAndroidUnitTest",
        ":shared:convertXmlValueResourcesForAndroidUnitTestDebug",
        ":shared:convertXmlValueResourcesForAndroidUnitTestRelease",
        ":shared:convertXmlValueResourcesForAppleMain",
        ":shared:convertXmlValueResourcesForAppleTest",
        ":shared:convertXmlValueResourcesForCommonMain",
        ":shared:convertXmlValueResourcesForCommonTest",
        ":shared:convertXmlValueResourcesForIosArm64Main",
        ":shared:convertXmlValueResourcesForIosArm64Test",
        ":shared:convertXmlValueResourcesForIosMain",
        ":shared:convertXmlValueResourcesForIosSimulatorArm64Main",
        ":shared:convertXmlValueResourcesForIosSimulatorArm64Test",
        ":shared:convertXmlValueResourcesForIosTest",
        ":shared:convertXmlValueResourcesForIosX64Main",
        ":shared:convertXmlValueResourcesForIosX64Test",
        ":shared:convertXmlValueResourcesForJvmMain",
        ":shared:convertXmlValueResourcesForJvmTest",
        ":shared:convertXmlValueResourcesForNativeMain",
        ":shared:convertXmlValueResourcesForNativeTest",
        ":shared:copyNonXmlValueResourcesForAndroidDebug",
        ":shared:copyNonXmlValueResourcesForAndroidInstrumentedTest",
        ":shared:copyNonXmlValueResourcesForAndroidInstrumentedTestDebug",
        ":shared:copyNonXmlValueResourcesForAndroidMain",
        ":shared:copyNonXmlValueResourcesForAndroidRelease",
        ":shared:copyNonXmlValueResourcesForAndroidUnitTest",
        ":shared:copyNonXmlValueResourcesForAndroidUnitTestDebug",
        ":shared:copyNonXmlValueResourcesForAndroidUnitTestRelease",
        ":shared:copyNonXmlValueResourcesForAppleMain",
        ":shared:copyNonXmlValueResourcesForAppleTest",
        ":shared:copyNonXmlValueResourcesForCommonMain",
        ":shared:copyNonXmlValueResourcesForCommonTest",
        ":shared:copyNonXmlValueResourcesForIosArm64Main",
        ":shared:copyNonXmlValueResourcesForIosArm64Test",
        ":shared:copyNonXmlValueResourcesForIosMain",
        ":shared:copyNonXmlValueResourcesForIosSimulatorArm64Main",
        ":shared:copyNonXmlValueResourcesForIosSimulatorArm64Test",
        ":shared:copyNonXmlValueResourcesForIosTest",
        ":shared:copyNonXmlValueResourcesForIosX64Main",
        ":shared:copyNonXmlValueResourcesForIosX64Test",
        ":shared:copyNonXmlValueResourcesForJvmMain",
        ":shared:copyNonXmlValueResourcesForJvmTest",
        ":shared:copyNonXmlValueResourcesForNativeMain",
        ":shared:copyNonXmlValueResourcesForNativeTest",
        ":shared:prepareComposeResourcesTaskForAndroidMain",
        ":shared:generateResourceAccessorsForAndroidMain",
        ":shared:prepareComposeResourcesTaskForCommonMain",
        ":shared:generateResourceAccessorsForCommonMain",
        ":shared:generateActualResourceCollectorsForAndroidMain",
        ":shared:prepareComposeResourcesTaskForAppleMain",
        ":shared:generateResourceAccessorsForAppleMain",
        ":shared:prepareComposeResourcesTaskForIosArm64Main",
        ":shared:generateResourceAccessorsForIosArm64Main",
        ":shared:prepareComposeResourcesTaskForIosMain",
        ":shared:generateResourceAccessorsForIosMain",
        ":shared:prepareComposeResourcesTaskForNativeMain",
        ":shared:generateResourceAccessorsForNativeMain",
        ":shared:generateActualResourceCollectorsForIosArm64Main",
        ":shared:prepareComposeResourcesTaskForIosSimulatorArm64Main",
        ":shared:generateResourceAccessorsForIosSimulatorArm64Main",
        ":shared:generateActualResourceCollectorsForIosSimulatorArm64Main",
        ":shared:prepareComposeResourcesTaskForIosX64Main",
        ":shared:generateResourceAccessorsForIosX64Main",
        ":shared:generateActualResourceCollectorsForIosX64Main",
        ":shared:prepareComposeResourcesTaskForJvmMain",
        ":shared:generateResourceAccessorsForJvmMain",
        ":shared:generateActualResourceCollectorsForJvmMain",
        ":shared:generateComposeResClass",
        ":shared:generateExpectResourceCollectorsForCommonMain",
        ":shared:prepareComposeResourcesTaskForAndroidDebug",
        ":shared:generateResourceAccessorsForAndroidDebug",
        ":shared:prepareComposeResourcesTaskForAndroidInstrumentedTest",
        ":shared:generateResourceAccessorsForAndroidInstrumentedTest",
        ":shared:prepareComposeResourcesTaskForAndroidInstrumentedTestDebug",
        ":shared:generateResourceAccessorsForAndroidInstrumentedTestDebug",
        ":shared:prepareComposeResourcesTaskForAndroidRelease",
        ":shared:generateResourceAccessorsForAndroidRelease",
        ":shared:prepareComposeResourcesTaskForAndroidUnitTest",
        ":shared:generateResourceAccessorsForAndroidUnitTest",
        ":shared:prepareComposeResourcesTaskForAndroidUnitTestDebug",
        ":shared:generateResourceAccessorsForAndroidUnitTestDebug",
        ":shared:prepareComposeResourcesTaskForAndroidUnitTestRelease",
        ":shared:generateResourceAccessorsForAndroidUnitTestRelease",
        ":shared:prepareComposeResourcesTaskForAppleTest",
        ":shared:generateResourceAccessorsForAppleTest",
        ":shared:prepareComposeResourcesTaskForCommonTest",
        ":shared:generateResourceAccessorsForCommonTest",
        ":shared:prepareComposeResourcesTaskForIosArm64Test",
        ":shared:generateResourceAccessorsForIosArm64Test",
        ":shared:prepareComposeResourcesTaskForIosSimulatorArm64Test",
        ":shared:generateResourceAccessorsForIosSimulatorArm64Test",
        ":shared:prepareComposeResourcesTaskForIosTest",
        ":shared:generateResourceAccessorsForIosTest",
        ":shared:prepareComposeResourcesTaskForIosX64Test",
        ":shared:generateResourceAccessorsForIosX64Test",
        ":shared:prepareComposeResourcesTaskForJvmTest",
        ":shared:generateResourceAccessorsForJvmTest",
        ":shared:prepareComposeResourcesTaskForNativeTest",
        ":shared:generateResourceAccessorsForNativeTest",
        ":shared:generateDummyFramework",
        ":shared:podspec",
        ":shared:podImport",
        ":shared:transformNativeMainCInteropDependenciesMetadataForIde",
        ":shared:transformAppleMainCInteropDependenciesMetadataForIde",
        ":shared:transformIosMainCInteropDependenciesMetadataForIde",
        ":shared:transformNativeTestCInteropDependenciesMetadataForIde",
        ":shared:transformAppleTestCInteropDependenciesMetadataForIde",
        ":shared:transformIosTestCInteropDependenciesMetadataForIde",
        ":shared:prepareKotlinIdeaImport",
        ":prepareKotlinBuildScriptModel"
    )
}