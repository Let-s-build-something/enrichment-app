
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import augmy.composeapp.generated.resources.Res
import augmy.composeapp.generated.resources.user_profile_invalid_link
import augmy.interactive.shared.ext.ifNull
import augmy.interactive.shared.ui.base.CustomSnackbarVisuals
import augmy.interactive.shared.ui.base.LocalSnackbarHost
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.getString
import ui.network.components.user_detail.UserDetailDialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModalHost(
    deepLink: String?,
    onDismissRequest: () -> Unit
) {
    val snackbarHostState = LocalSnackbarHost.current
    val coroutineScope = rememberCoroutineScope()

    when {
        deepLink?.matches("""users\/.+""".toRegex()) == true -> {
            deepLink.split("/").getOrNull(1)?.let { userId ->
                UserDetailDialog(
                    userId = deepLink.split("/").getOrNull(1) ?: "",
                    onDismissRequest = onDismissRequest
                )
            }.ifNull {
                coroutineScope.launch {
                    snackbarHostState?.showSnackbar(
                        CustomSnackbarVisuals(
                            isError = true,
                            message = getString(Res.string.user_profile_invalid_link)
                        )
                    )
                }
            }
        }
    }
}