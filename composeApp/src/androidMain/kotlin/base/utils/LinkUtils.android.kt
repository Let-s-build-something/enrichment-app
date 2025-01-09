package base.utils

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
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