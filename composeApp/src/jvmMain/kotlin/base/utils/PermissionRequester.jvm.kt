package base.utils

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

@Composable
actual fun rememberPermissionRequesterState(
    type: PermissionType,
    onResponse: (Boolean) -> Unit
): PermissionRequester {
    return remember {
        object: PermissionRequester(type = type) {
            override val isGranted: Boolean = true
            override val requestPermission: () -> Unit = {}
        }
    }
}