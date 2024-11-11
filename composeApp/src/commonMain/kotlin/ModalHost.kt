import androidx.compose.runtime.Composable
import org.koin.core.context.loadKoinModules
import ui.network.profile.UserProfileLauncher
import ui.network.profile.userProfileModule

@Composable
fun ModalHost(
    deepLink: String?,
    onDismissRequest: () -> Unit
) {
    when {
        deepLink?.matches("""users\/.+""".toRegex()) == true -> {
            loadKoinModules(userProfileModule)
            UserProfileLauncher(
                publicId = deepLink.split("/").getOrNull(1),
                onDismissRequest = onDismissRequest
            )
        }
    }
}