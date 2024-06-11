package lets.build.chatenrichment.ui.account

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.squadris.squadris.compose.base.LocalNavController
import com.squadris.squadris.compose.components.navigation.NavIconType
import com.squadris.squadris.compose.theme.LocalTheme
import lets.build.chatenrichment.R
import lets.build.chatenrichment.ui.account.AccountDashboardViewModel
import lets.build.chatenrichment.ui.base.BrandBaseScreen
import lets.build.chatenrichment.ui.components.ErrorHeaderButton

/**
 * Main screen for displaying basic user data and dashboard for user-related actions
 */
@Composable
fun AccountDashboardScreen(
    viewModel: AccountDashboardViewModel = hiltViewModel()
) {
    val navController = LocalNavController.current

    val currentUser = viewModel.currentUser.collectAsState()

    LaunchedEffect(key1 = currentUser.value) {
        if(currentUser.value == null) navController?.popBackStack()
    }

    BrandBaseScreen(
        title = stringResource(R.string.screen_account_title),
        subtitle = currentUser.value?.displayName,
        navIconType = NavIconType.HOME,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            AsyncImage(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.4f),
                model = R.drawable.i1_sign_in,
                contentDescription = stringResource(R.string.accessibility_sign_in_illustration)
            )
            Column(
                modifier = Modifier
                    .padding(start = 8.dp, end = 8.dp, bottom = 8.dp)
                    .background(
                        color = LocalTheme.current.colors.onBackgroundComponent,
                        shape = LocalTheme.current.shapes.componentShape
                    )
                    .padding(vertical = 8.dp),
                verticalArrangement = Arrangement.Bottom
            ) {
                ErrorHeaderButton(
                    modifier = Modifier
                        .padding(horizontal = 16.dp)
                        .fillMaxWidth(),
                    text = stringResource(R.string.screen_account_logout),
                    onClick = {
                        viewModel.logoutCurrentUser()
                    }
                )
            }
        }
    }
}