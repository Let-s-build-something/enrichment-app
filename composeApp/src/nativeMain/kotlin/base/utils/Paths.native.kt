package base.utils

import okio.FileSystem
import okio.Path.Companion.toPath
import okio.Sink
import org.koin.core.module.Module
import org.koin.dsl.module
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSSearchPathForDirectoriesInDomains
import platform.Foundation.NSURL
import platform.Foundation.NSUserDomainMask

actual fun platformPathsModule(): Module = module {
    single { FileSystem.SYSTEM }
    single<RootPath> {
        RootPath(
            (NSSearchPathForDirectoriesInDomains(
                NSDocumentDirectory,
                NSUserDomainMask,
                true
            )[0] as String)
                .toPath()
        )
    }
}

actual fun openSinkFromUri(uri: String): Sink {
    val nsUrl = NSURL.URLWithString(uri)
        ?: throw IllegalArgumentException("Invalid URL: $uri")

    val filePath = nsUrl.path ?: throw IllegalArgumentException("Cannot extract file path from URI: $uri")

    val file = filePath.toPath()

    // Security scoped access (needed if the file is outside sandbox, i.e. user-selected)
    val accessed = nsUrl.startAccessingSecurityScopedResource()
    if (!accessed) {
        println("Failed to access security-scoped resource: $uri")
    }

    // Wrap with a sink, appending mode
    val sink = FileSystem.SYSTEM.appendingSink(file)

    // Stop access when the sink is closed (this is important!)
    return object : Sink by sink {
        override fun close() {
            sink.close()
            if (accessed) {
                nsUrl.stopAccessingSecurityScopedResource()
            }
        }
    }
}

actual fun getDownloadsPath(): String {
    val paths = NSSearchPathForDirectoriesInDomains(
        directory = NSDocumentDirectory,
        domainMask = NSUserDomainMask,
        expandTilde = true
    )
    val documentsDir = paths.firstOrNull() as? String
        ?: error("Unable to locate Documents directory")

    return "$documentsDir/Downloads"
}
