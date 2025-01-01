package database.file

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext
import okio.FileNotFoundException
import okio.FileSystem
import okio.Path
import okio.SYSTEM

/** Returns a platform specific cache directory */
expect fun getCacheDirectory(): Path

/** Use case for general file operations */
open class FileAccess {

    /**
     * Saves a file to the cache directory with raw ByteArray content [data]
     * @return the path to the created file
     */
    suspend fun saveFileToCache(data: ByteArray, fileName: String): Path? {
        return withContext(Dispatchers.IO) {
            try {
                val cachePath = getCacheDirectory().div(fileName)
                FileSystem.SYSTEM.write(cachePath) {
                    write(data)
                }
                cachePath
            } catch (e: FileNotFoundException) {
                null
            }
        }
    }

    /** Attempts to retrieve a file from the cache directory */
    suspend fun loadFileFromCache(fileName: String): ByteArray? {
        return withContext(Dispatchers.IO) {
            val cachePath = getCacheDirectory().div(fileName)
            if (FileSystem.SYSTEM.exists(cachePath)) {
                FileSystem.SYSTEM.read(cachePath) { readByteArray() }
            } else null
        }
    }
}
