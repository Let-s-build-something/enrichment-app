
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import data.io.user.NetworkItemIO
import ui.network.profile.UserProfileLauncher

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModalHost(
    deepLink: String?,
    onDismissRequest: () -> Unit
) {
    when {
        deepLink?.matches("""users\/.+""".toRegex()) == true -> {
            UserProfileLauncher(
                user = NetworkItemIO(
                    publicId = deepLink.split("/").getOrNull(1) ?: ""
                ),
                onDismissRequest = onDismissRequest
            )
        }
    }
}