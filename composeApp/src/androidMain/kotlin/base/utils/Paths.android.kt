package base.utils

import android.content.Context
import android.os.Environment
import okio.Path.Companion.toOkioPath
import org.koin.core.module.Module
import org.koin.dsl.module

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
