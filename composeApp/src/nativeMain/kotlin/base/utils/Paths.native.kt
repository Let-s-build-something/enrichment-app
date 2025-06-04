package base.utils

import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.startAccessingSecurityScopedResource
import io.github.vinceglb.filekit.stopAccessingSecurityScopedResource
import okio.FileSystem
import okio.Path.Companion.toPath
import okio.Sink
import org.koin.core.module.Module
import org.koin.dsl.module
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSSearchPathForDirectoriesInDomains
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

actual fun PlatformFile.openSinkFromUri(): Sink {
    val filePath = nsUrl.path ?: throw IllegalArgumentException("Cannot extract file path")
    val fileIOPath = filePath.toPath()

    val accessed = startAccessingSecurityScopedResource()
    println("Security scoped access result: $accessed, uri: $filePath")

    val sink = FileSystem.SYSTEM.appendingSink(fileIOPath)

    return object : Sink by sink {
        override fun close() {
            sink.close()
            if (accessed) {
                stopAccessingSecurityScopedResource()
                println("Stopped security scoped access")
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
