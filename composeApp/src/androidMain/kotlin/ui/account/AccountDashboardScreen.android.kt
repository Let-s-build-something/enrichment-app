package ui.account

import android.content.Context
import android.content.Intent
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