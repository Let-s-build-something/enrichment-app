package base.utils

import okio.FileSystem
import okio.Path.Companion.toPath
import org.koin.core.module.Module
import org.koin.dsl.module
import java.util.Locale

actual fun platformPathsModule(): Module = module {
    single<RootPath> {
        val path = getAppPath("Augmy")
        FileSystem.SYSTEM.createDirectories(path)
        RootPath(path)
    }
}

actual fun getDownloadsPath(): String {
    return when (getOs()) {
        OS.MAC_OS, OS.LINUX -> {
            val home = System.getenv("HOME")?.toPath()
                ?: throw IllegalStateException("HOME environment variable is not set.")
            home.resolve("Downloads")
        }

        OS.WINDOWS -> {
            val userProfile = System.getenv("USERPROFILE")?.toPath()
                ?: throw IllegalStateException("USERPROFILE environment variable is not set.")
            userProfile.resolve("Downloads")
        }
    }.toString()
}

enum class OS(val value: String) {
    WINDOWS("Windows"), MAC_OS("macOS"), LINUX("Linux")
}

fun getOs(): OS {
    val os = System.getProperty("os.name", "generic").lowercase(Locale.ENGLISH)
    return when {
        os.contains("mac", ignoreCase = true) || os.contains("darwin", ignoreCase = true) -> OS.MAC_OS
        os.contains("win", ignoreCase = true) -> OS.WINDOWS
        os.contains("linux", ignoreCase = true) -> OS.LINUX
        else -> throw RuntimeException("os $os is not supported")
    }
}

fun getAppPath(appId: String) =
    // TODO we don't have that environment variable yet
    System.getenv("Augmy")?.toPath()
        ?: when (getOs()) {
            OS.MAC_OS -> {
                System.getenv("HOME").toPath()
                    .resolve("Library")
                    .resolve("Application Support")
                    .resolve(appId)
            }

            OS.WINDOWS -> {
                System.getenv("LOCALAPPDATA").toPath()
                    .resolve(appId)
            }

            OS.LINUX -> {
                val dataHome = System.getenv("XDG_DATA_HOME")?.toPath()
                    ?: System.getenv("HOME").toPath().resolve(".local").resolve("share")

                dataHome.resolve(appId)
            }
        }
