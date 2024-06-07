package lets.build.chatenrichment.ui.home

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import lets.build.chatenrichment.R
import lets.build.chatenrichment.ui.base.PullRefreshScreen

/** Application home screen */
@Composable
fun HomeScreen(viewModel: HomeViewModel = hiltViewModel()) {

    PullRefreshScreen(
        modifier = Modifier.fillMaxSize(),
        viewModel = viewModel,
        title = stringResource(R.string.home_screen)
    ) {

    }
}