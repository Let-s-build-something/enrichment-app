# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

-keep class com.google.gson.reflect.TypeToken
-keep class study.me.please.base.navigation.** { *; }
-keep class * extends com.google.gson.reflect.TypeToken
-keep public class * implements java.lang.reflect.Type

-keep class data.** { *; }
-keep class base.** { *; }

-keep class com.google.firebase.** { *; }
-keep class com.google.android.gms.** { *; }
-keep class org.koin.** { *; }
-keep class Chatenrichment.composeApp.BuildConfig.** { *; }
-keep class org.koin.core.** { *; }
-keep class org.koin.dsl.** { *; }

-keep class io.ktor.utils.io.** { *; }
-keep class io.ktor.utils.io.jvm.** { *; }
-keep class io.ktor.utils.io.nio.** { *; }
-keep class io.ktor.server.config.** { *; }
-keep class io.ktor.serialization.** { *; }
-keep class kotlin.reflect.jvm.internal.** { *; }
-keep class coil3.** { *; }
-keep class ui.** { *; }
-keep class androidx.datastore.preferences.** { *; }
-dontwarn androidx.compose.material.**
-keep class androidx.compose.material3.** { *; }
-keep class androidx.compose.runtime.** { *; }
-keep class androidx.collection.** { *; }
-keep class androidx.lifecycle.** { *; }

# We're excluding Material 2 from the project as we're using Material 3
-dontwarn androidx.compose.material.**

# Kotlinx coroutines rules seems to be outdated with the latest version of Kotlin and Proguard
-keep class kotlinx.coroutines.** { *; }

-ignorewarnings