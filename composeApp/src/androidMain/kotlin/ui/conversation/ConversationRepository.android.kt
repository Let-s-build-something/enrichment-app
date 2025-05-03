package ui.conversation

import base.utils.fromByteArrayToData
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.storage.storage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Attempts to upload a file to Firebase storage, and returns the download URL of the uploaded file. */
actual suspend fun uploadMediaToStorage(
    conversationId: String,
    byteArray: ByteArray,
    fileName: String
): String {
    return withContext(Dispatchers.IO) {
        val reference = Firebase.storage.reference.child("conversation/$conversationId/$fileName")

        reference.putData(fromByteArrayToData(byteArray))
        reference.getDownloadUrl()
    }
}