package database.file

import okio.Path
import okio.Path.Companion.toOkioPath
import java.nio.file.Paths

actual fun getCacheDirectory(): Path {
    return Paths.get(System.getProperty("java.io.tmpdir")).toOkioPath()
}