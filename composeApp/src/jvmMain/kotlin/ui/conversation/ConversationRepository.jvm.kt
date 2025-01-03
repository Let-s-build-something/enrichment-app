package ui.conversation

import augmy.interactive.com.BuildKonfig
import com.google.auth.oauth2.ServiceAccountCredentials
import com.google.cloud.storage.BlobId
import com.google.cloud.storage.BlobInfo
import com.google.cloud.storage.StorageOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.FileInputStream
import java.util.concurrent.TimeUnit

/** Attempts to upload a file to Firebase storage, and returns the download URL of the uploaded file. */
actual suspend fun uploadMediaToStorage(
    conversationId: String,
    byteArray: ByteArray,
    fileName: String
): String {
    return withContext(Dispatchers.IO) {
        try {
            val pathToJson = "chat-enrichment-fbc09765a643.json"
            val credentials = ServiceAccountCredentials.fromStream(FileInputStream(pathToJson))

            val storage = StorageOptions.newBuilder()
                .setCredentials(credentials)
                .build()
                .service

            // Define bucket name and path
            val bucketName = BuildKonfig.StorageBucketName
            val objectPath = "conversation/$conversationId/$fileName"

            // Create a blob ID and blob info
            val blobId = BlobId.of(bucketName, objectPath)
            val blobInfo = BlobInfo.newBuilder(blobId).build()
            storage.create(blobInfo, byteArray)
            storage.signUrl(blobInfo, 10 * 365, TimeUnit.DAYS).toString()
        } catch (e: Exception) {
            e.printStackTrace()
            throw e
        }
    }
}
