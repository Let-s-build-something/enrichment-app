package lets.build.chatenrichment.ui.home

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import com.squadris.squadris.compose.components.navigation.NavIconType
import lets.build.chatenrichment.R
import lets.build.chatenrichment.ui.base.PullRefreshScreen

/** Application home screen */
@Composable
fun HomeScreen(viewModel: HomeViewModel = hiltViewModel()) {

    PullRefreshScreen(
        modifier = Modifier.fillMaxSize(),
        viewModel = viewModel,
        navIconType = NavIconType.HOME,
        title = stringResource(R.string.screen_home)
    ) {

    }
}