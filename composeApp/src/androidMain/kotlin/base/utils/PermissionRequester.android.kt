package base.utils

import android.os.Build
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState

@OptIn(ExperimentalPermissionsApi::class)
@Composable
actual fun rememberPermissionRequesterState(
    type: PermissionType,
    onResponse: (Boolean) -> Unit
): PermissionRequester {
    val isGranted = remember {
        mutableStateOf(false)
    }

    val permissionState = rememberPermissionState(
        when(type) {
            PermissionType.CAMERA -> android.Manifest.permission.CAMERA
            PermissionType.AUDIO_RECORD -> android.Manifest.permission.RECORD_AUDIO
            PermissionType.NOTIFICATIONS -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                android.Manifest.permission.POST_NOTIFICATIONS
            } else {
                android.Manifest.permission.INTERNET
            }
        },
        onPermissionResult = {
            isGranted.value = it
            onResponse(it)
        }
    )

    LaunchedEffect(Unit) {
        isGranted.value = permissionState.status.isGranted
    }

    return remember(isGranted.value) {
        object: PermissionRequester(type = type) {
            override val isGranted: Boolean
                get() = permissionState.status.isGranted

            override val requestPermission: () -> Unit = {
                permissionState.launchPermissionRequest()
            }
        }
    }
}