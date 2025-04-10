package ui.search.user

import androidx.compose.runtime.Composable
import augmy.composeapp.generated.resources.Res
import augmy.composeapp.generated.resources.screen_search_user
import augmy.interactive.shared.ui.base.LocalNavController
import base.BrandBaseScreen
import org.jetbrains.compose.resources.stringResource

@Composable
fun SearchUserScreen(awaitingResult: Boolean?) {
    val navController = LocalNavController.current

    BrandBaseScreen(title = stringResource(Res.string.screen_search_user)) {

    }
}