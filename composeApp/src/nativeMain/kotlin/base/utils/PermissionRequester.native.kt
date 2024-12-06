package base.utils

import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import platform.AVFAudio.AVAudioApplication
import platform.AVFAudio.AVAudioApplicationRecordPermissionGranted
import platform.AVFoundation.AVAuthorizationStatusAuthorized
import platform.AVFoundation.AVCaptureDevice
import platform.AVFoundation.AVMediaTypeVideo
import platform.AVFoundation.authorizationStatusForMediaType
import platform.AVFoundation.requestAccessForMediaType
import platform.UserNotifications.UNAuthorizationOptionAlert
import platform.UserNotifications.UNAuthorizationOptionBadge
import platform.UserNotifications.UNAuthorizationOptionSound
import platform.UserNotifications.UNAuthorizationStatusAuthorized
import platform.UserNotifications.UNUserNotificationCenter

@Composable
actual fun rememberPermissionRequesterState(
    type: PermissionType,
    onResponse: (Boolean) -> Unit
): PermissionRequester {
    val isGranted = remember {
        mutableStateOf(false)
    }

    when(type) {
        PermissionType.CAMERA -> {
            isGranted.value = AVCaptureDevice.authorizationStatusForMediaType(
                AVMediaTypeVideo
            ) == AVAuthorizationStatusAuthorized
        }
        PermissionType.AUDIO_RECORD -> {
            isGranted.value = AVAudioApplication.sharedInstance.recordPermission == AVAudioApplicationRecordPermissionGranted
        }
        PermissionType.NOTIFICATIONS -> {
            UNUserNotificationCenter.currentNotificationCenter().getNotificationSettingsWithCompletionHandler {
                isGranted.value = it?.authorizationStatus == UNAuthorizationStatusAuthorized
            }
        }
    }

    return remember(isGranted.value) {
        object : PermissionRequester(type = type) {
            override val isGranted: Boolean
                get() = isGranted.value

            override val requestPermission: () -> Unit = {
                when (type) {
                    PermissionType.CAMERA -> {
                        AVCaptureDevice.requestAccessForMediaType(AVMediaTypeVideo) {
                            isGranted.value = it
                            onResponse(it)
                        }
                    }
                    PermissionType.AUDIO_RECORD -> {
                        AVAudioApplication.requestRecordPermissionWithCompletionHandler {
                            isGranted.value = it
                            onResponse(it)
                        }
                    }
                    PermissionType.NOTIFICATIONS -> UNUserNotificationCenter.currentNotificationCenter().requestAuthorizationWithOptions(
                        UNAuthorizationOptionSound or UNAuthorizationOptionBadge or UNAuthorizationOptionAlert
                    ) { result, _ ->
                        isGranted.value = result
                        onResponse(result)
                    }
                }
            }
        }
    }
}
