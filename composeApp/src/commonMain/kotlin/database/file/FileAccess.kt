package database.file

import augmy.interactive.shared.utils.DateUtils
import base.utils.getUrlExtension
import korlibs.io.net.MimeType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext
import okio.FileNotFoundException
import okio.FileSystem
import okio.Path
import okio.SYSTEM
import ui.conversation.components.audio.MediaProcessorRepository

/** Use case for general file operations */
open class FileAccess {

    companion object {
        const val EXPIRATION_MILLIS = 7L * 24L * 60L * 60L * 1000L

        val TEMPORARY_DIRECTORY: Path
            get() = ensureDirectoryExists(FileSystem.SYSTEM_TEMPORARY_DIRECTORY.div("Augmy"))
                ?: FileSystem.SYSTEM_TEMPORARY_DIRECTORY

        private fun ensureDirectoryExists(directory: Path): Path? {
            return try {
                val metadata = FileSystem.SYSTEM.metadataOrNull(directory)
                if (metadata == null || !metadata.isDirectory) {
                    FileSystem.SYSTEM.createDirectories(directory)
                }
                directory
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }
    }

    /**
     * Saves a file to the cache directory with raw ByteArray content [data]
     * @return the path to the created file
     */
    suspend fun saveFileToCache(data: ByteArray, fileName: String): Path? {
        return withContext(Dispatchers.IO) {
            try {
                val cachePath = TEMPORARY_DIRECTORY.div(fileName)
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
        extension: String?,
        expirationMillis: Long = EXPIRATION_MILLIS
    ): MediaProcessorRepository.FileResult? {
        return withContext(Dispatchers.IO) {
            val cachePath = TEMPORARY_DIRECTORY.div(fileName).takeIf {
                FileSystem.SYSTEM.exists(it)
            } ?: FileSystem.SYSTEM.list(TEMPORARY_DIRECTORY).firstOrNull { it.name.startsWith("$fileName.") }

            if(cachePath == null) return@withContext null

            try {
                if((FileSystem.SYSTEM.metadataOrNull(cachePath)?.lastAccessedAtMillis
                    ?.minus(DateUtils.now.toEpochMilliseconds()) ?: expirationMillis) <= expirationMillis) {
                    MediaProcessorRepository.FileResult(
                        byteArray = FileSystem.SYSTEM.read(cachePath) { readByteArray() },
                        path = cachePath,
                        mimetype = MimeType.getByExtension(
                            ext = extension ?: getUrlExtension(cachePath.name),
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
