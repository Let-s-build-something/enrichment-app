package database.file

import android.content.Context
import okio.Path
import okio.Path.Companion.toPath
import org.koin.mp.KoinPlatform.getKoin

actual fun getCacheDirectory(): Path {
    val context: Context = getKoin().get()
    return context.cacheDir.absolutePath.toPath()
}
