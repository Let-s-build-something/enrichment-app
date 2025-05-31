package base.utils

import android.content.Context
import android.os.Environment
import androidx.core.net.toUri
import okio.Path.Companion.toOkioPath
import okio.Sink
import okio.buffer
import okio.sink
import org.koin.core.module.Module
import org.koin.dsl.module
import org.koin.mp.KoinPlatform.getKoin
import java.io.FileOutputStream
import java.io.IOException

actual fun platformPathsModule(): Module = module {
    single<RootPath> {
        val context = get<Context>()
        RootPath(context.filesDir.toOkioPath())
    }
}

actual fun getDownloadsPath(): String {
    val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
    return downloadsDir.absolutePath
}

actual fun openSinkFromUri(uri: String): Sink {
    val parsedUri = uri.toUri()
    val pfd = getKoin().get<Context>().contentResolver.openFileDescriptor(parsedUri, "wa") // append mode
        ?: throw IOException("Cannot open file descriptor for URI: $uri")

    val outputStream = FileOutputStream(pfd.fileDescriptor)
    return outputStream.sink().buffer()
}
