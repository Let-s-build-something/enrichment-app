package chat.enrichment.eu.fcm

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.NotificationCompat
import base.navigation.NavigationNode
import chat.enrichment.eu.MainActivity
import chat.enrichment.eu.R
import coil3.ImageLoader
import coil3.request.CachePolicy
import coil3.request.ErrorResult
import coil3.request.ImageRequest
import coil3.request.allowHardware
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import io.notifications.NotificationTag
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

internal class GeneralFirebaseMessagingService : FirebaseMessagingService() {

    companion object {
        /**
         * extra information connected to intent for informing about uri that needs to be opened
         */
        private const val EXTRA_NOTIFICATION_CLICK_URI = "extra_notification_click_uri"
    }

    private val processScope = CoroutineScope(Dispatchers.IO)

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        val json = Json {
            encodeDefaults = true
            ignoreUnknownKeys = true
        }

        (ProcessedMessageData.fromNotification(
            remoteMessage.notification
        ) ?: json.decodeFromString<ProcessedMessageData>(
            json.encodeToString(remoteMessage.data)
        )).let { directNotification ->
            processScope.launch {
                processNotification(directNotification)
            }
        }
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
    }

    private suspend fun processNotification(notification: ProcessedMessageData) {
        withContext(Dispatchers.IO) {
            if(notification.body != null) {
                notification.largeIcon = if(!notification.icon.isNullOrEmpty()) {
                    getRemoteIcon(notification.icon.toString())
                }else null

                notification.image = if(!notification.imageUrl.isNullOrEmpty()) {
                    getRemoteIcon(notification.imageUrl.toString())
                }else null

                notification.messageType = NotificationTag.entries.find { it.name == notification.tag }

                val channelName = notification.messageType?.humanReadableChannel?.let {
                    org.jetbrains.compose.resources.getString(it)
                }

                withContext(Dispatchers.Main) {
                    notification.action = if(notification.clickAction != null) {
                        NotificationCompat.Action(
                            0,
                            notification.clickAction,
                            PendingIntent.getActivity(
                                this@GeneralFirebaseMessagingService,
                                0,
                                Intent(
                                    this@GeneralFirebaseMessagingService,
                                    MainActivity::class.java
                                ).apply {
                                    when(notification.messageType) {
                                        NotificationTag.OPEN_ACCOUNT -> {
                                            putExtra(
                                                EXTRA_NOTIFICATION_CLICK_URI,
                                                NavigationNode.AccountDashboard.deepLink
                                            )
                                        }
                                        null -> {}
                                    }
                                },
                                PendingIntent.FLAG_IMMUTABLE,
                            )
                        )
                    }else null

                    sendNotification(notification, channelName)
                }
            }
        }
    }

    private fun sendNotification(
        notification: ProcessedMessageData,
        channelName: String?
    ) {
        val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        val channelId = notification.channelId ?: getString(R.string.default_notification_channel_id)
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager

        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java).apply {
                putExtra(
                    EXTRA_NOTIFICATION_CLICK_URI,
                    notification.link
                )
            },
            PendingIntent.FLAG_IMMUTABLE,
        )

        val notificationBuilder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.mipmap.ic_launcher_round)
            .setLargeIcon(notification.largeIcon)
            .setContentTitle(notification.title)
            .setContentText(notification.body)
            .setAutoCancel(true) // notifications while we're in the app should be seen in the app
            .setOnlyAlertOnce(true)
            .addAction(notification.action)
            .setOngoing(notification.sticky)
            .setSound(if(notification.defaultSound) defaultSoundUri else null)
            .setContentIntent(pendingIntent)
            .setStyle(
                if(notification.image != null) {
                    NotificationCompat
                        .BigPictureStyle()
                        .bigPicture(notification.image)
                }else null
            )

        // Since android Oreo notification channel is needed.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                channelName ?: getString(R.string.default_notification_channel_name),
                NotificationManager.IMPORTANCE_DEFAULT,
            )
            notificationManager?.createNotificationChannel(channel)
        }

        notificationManager?.notify((0..999999).random(), notificationBuilder.build())
    }


    //==== UTILS =====

    private suspend fun getRemoteIcon(imageUrl: String): Bitmap? = suspendCoroutine { coroutine ->
        ImageLoader(this).enqueue(
            ImageRequest.Builder(this)
                .data(imageUrl)
                .target { drawable ->
                    coroutine.resume((drawable as? BitmapDrawable)?.bitmap)
                }
                .listener(
                    object: ImageRequest.Listener {
                        override fun onError(request: ImageRequest, result: ErrorResult) {
                            coroutine.resume(null)
                        }
                    }
                )
                .memoryCachePolicy(CachePolicy.ENABLED)
                .diskCachePolicy(CachePolicy.ENABLED)
                .allowHardware(false)
                .build()
        )
    }
}