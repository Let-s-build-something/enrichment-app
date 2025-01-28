package base.utils

import android.content.ActivityNotFoundException
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import androidx.core.net.toUri
import data.io.social.network.conversation.message.MediaIO
import korlibs.io.net.MimeType
import org.koin.mp.KoinPlatform.getKoin

actual fun shareLink(title: String, link: String): Boolean {
    val context: Context = getKoin().get()

    val share = Intent.createChooser(Intent().apply {
        action = Intent.ACTION_SEND
        putExtra(Intent.EXTRA_TEXT, link)
        putExtra(Intent.EXTRA_TITLE, title)
        type = "text/plain"
    }, null)
    context.startActivity(share)
    return true
}

actual fun shareMessage(media: List<MediaIO>, messageContent: String): Boolean {
    val context: Context = getKoin().get()

    val share = Intent.createChooser(Intent().apply {
        action = Intent.ACTION_SEND_MULTIPLE
        putExtra(Intent.EXTRA_TEXT, messageContent)
        type = if(media.isNotEmpty()) {
            putParcelableArrayListExtra(Intent.EXTRA_STREAM, ArrayList(media.mapNotNull { it.url?.toUri() }))
            "image/*"
        }else "text/plain"
    }, null)
    context.startActivity(share)
    return true
}

actual fun openLink(link: String): Boolean {
    val context = getKoin().get<Context>()

    return try {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(link))
        context.startActivity(intent)
        true
    }catch (e: ActivityNotFoundException) {
        false
    }
}

actual fun downloadFiles(data: Map<MediaIO, ByteArray>): Boolean {
    val resolver = getKoin().get<Context>().contentResolver

    var result = true
    data.forEach { (media, data) ->
        val isImage = media.mimetype?.startsWith("image/") == true
        val isVideo = media.mimetype?.startsWith("video/") == true
        val mimeType = media.mimetype ?: MimeType.getByExtension(getUrlExtension(media.url ?: "")).mime

        val contentValues = ContentValues().apply {
            put(MediaStore.Downloads.DISPLAY_NAME, "augmy_${sha256(media.url)}.${getExtensionFromMimeType(mimeType) ?: ""}")
            put(MediaStore.Downloads.MIME_TYPE, mimeType)
            put(
                MediaStore.Downloads.RELATIVE_PATH,
                when {
                    isImage -> Environment.DIRECTORY_PICTURES
                    isVideo -> Environment.DIRECTORY_MOVIES
                    else -> Environment.DIRECTORY_DOWNLOADS
                }
            )
        }

        val contentUri = when {
            isImage -> MediaStore.Images.Media.EXTERNAL_CONTENT_URI
            mimeType.startsWith("video/") -> MediaStore.Video.Media.EXTERNAL_CONTENT_URI
            mimeType.startsWith("audio/") -> MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q -> MediaStore.Downloads.EXTERNAL_CONTENT_URI
            else -> MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        }

        try {
            val uri = resolver.insert(contentUri, contentValues)
            if (uri != null) {
                resolver.openOutputStream(uri)?.use { outputStream ->
                    outputStream.write(data)
                    outputStream.flush()
                }
            } else {
                Log.e("DownloadImage", "Failed to insert content values.")
                result = false
            }
        } catch (e: Exception) {
            Log.e("DownloadImage", "Error saving image: ${e.message}", e)
            result = false
        }
    }

    return result
}

actual fun openFile(path: String?) {
}

actual fun openEmail(address: String?): Boolean {
    val context = getKoin().get<Context>()

    return try {
        context.startActivity(
            Intent(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_APP_EMAIL)
            }
        )
        true
    }catch (e: Exception) {
        false
    }
}
