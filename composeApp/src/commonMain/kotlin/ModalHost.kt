
import androidx.compose.runtime.Composable
import ui.network.profile.UserProfileLauncher

@Composable
fun ModalHost(
    deepLink: String?,
    onDismissRequest: () -> Unit
) {
    when {
        deepLink?.matches("""users\/.+""".toRegex()) == true -> {
            UserProfileLauncher(
                publicId = deepLink.split("/").getOrNull(1),
                onDismissRequest = onDismissRequest
            )
        }
    }
}