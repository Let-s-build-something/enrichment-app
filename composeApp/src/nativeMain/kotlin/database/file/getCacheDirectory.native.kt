package database.file

import okio.Path
import okio.Path.Companion.toPath
import platform.Foundation.NSTemporaryDirectory

actual fun getCacheDirectory(): Path {
    return NSTemporaryDirectory().toPath()
}
