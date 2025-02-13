package database.file

import augmy.interactive.shared.utils.DateUtils
import korlibs.io.net.MimeType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext
import okio.FileNotFoundException
import okio.FileSystem
import okio.Path
import okio.SYSTEM
import ui.conversation.components.audio.MediaProcessorRepository

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
                val cachePath = FileSystem.SYSTEM_TEMPORARY_DIRECTORY.div(fileName)
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
    suspend fun loadFileFromCache(
        fileName: String,
        extension: String?
    ): MediaProcessorRepository.FileResult? {
        return withContext(Dispatchers.IO) {
            val cachePath = FileSystem.SYSTEM_TEMPORARY_DIRECTORY.div(fileName)
            try {
                if((FileSystem.SYSTEM.metadataOrNull(cachePath)?.lastAccessedAtMillis
                    ?.minus(DateUtils.now.toEpochMilliseconds()) ?: EXPIRATION_MILLIS) < EXPIRATION_MILLIS) {
                    MediaProcessorRepository.FileResult(
                        byteArray = FileSystem.SYSTEM.read(cachePath) { readByteArray() },
                        path = cachePath,
                        mimetype = MimeType.getByExtension(
                            ext = extension ?: "",
                            default = MimeType.IMAGE_JPEG
                        ).mime
                    )
                }else {
                    FileSystem.SYSTEM.delete(cachePath)
                    null
                }
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }
    }
}

private const val EXPIRATION_MILLIS = 7L * 24L * 60L * 60L * 1000L
