package base.utils

import androidx.compose.runtime.Composable

abstract class PermissionRequester(val type: PermissionType) {
    abstract val isGranted: Boolean
    abstract val requestPermission: () -> Unit
}

@Composable
expect fun rememberPermissionRequesterState(
    type: PermissionType,
    onResponse: (Boolean) -> Unit = {}
): PermissionRequester

/** Type of permission to be requested */
enum class PermissionType {
    CAMERA,
    AUDIO_RECORD,
    NOTIFICATIONS
}