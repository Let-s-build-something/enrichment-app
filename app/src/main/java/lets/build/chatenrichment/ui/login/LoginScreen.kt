package lets.build.chatenrichment.ui.login

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AlternateEmail
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.squadris.squadris.compose.base.LocalNavController
import com.squadris.squadris.compose.base.LocalSnackbarHost
import com.squadris.squadris.compose.theme.LocalTheme
import com.squadris.squadris.ext.scalingClickable
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import lets.build.chatenrichment.R
import lets.build.chatenrichment.navigation.NavigationTree
import lets.build.chatenrichment.ui.base.BrandBaseScreen
import lets.build.chatenrichment.ui.components.BrandHeaderButton
import lets.build.chatenrichment.ui.components.OutlinedButton

/** Application home screen */
@Composable
fun LoginScreen(viewModel: LoginViewModel = hiltViewModel()) {

    BrandBaseScreen(
        modifier = Modifier.fillMaxSize(),
        title = stringResource(R.string.screen_login)
    ) {
        LoginContent(viewModel = viewModel)
    }
}

@Preview(showBackground = true)
@Composable
private fun LoginContent(viewModel: LoginViewModel? = null) {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val navController = LocalNavController.current
    val snackbarHostState = LocalSnackbarHost.current

    val currentUser = viewModel?.currentUser?.collectAsState()

    val credentialManager = remember { CredentialManager.create(context) }

    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        viewModel?.loginErrorResponse?.collectLatest {
            when(it) {
                LoginErrorType.NO_GOOGLE_CREDENTIALS -> {
                    coroutineScope.launch {
                        snackbarHostState?.showSnackbar(
                            message = context.getString(R.string.error_google_sign_in_unavailable)
                        )
                    }
                }
                else -> {}
            }
        }
    }

    LaunchedEffect(currentUser?.value) {
        if(currentUser?.value != null) {
            navController?.popBackStack(NavigationTree.Login, inclusive = true)
        }
    }

    Column(
        modifier = Modifier
            .padding(horizontal = 8.dp)
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        AsyncImage(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = configuration.screenHeightDp.div(2.5).dp),
            model = R.drawable.i0_sign_up,
            contentDescription = stringResource(R.string.accessibility_sign_up_illustration)
        )

        Column(
            modifier = Modifier
                .padding(top = 16.dp)
                .fillMaxWidth()
                .wrapContentHeight()
                .background(
                    color = LocalTheme.current.colors.onBackgroundComponent,
                    shape = LocalTheme.current.shapes.componentShape
                )
                .padding(vertical = 12.dp, horizontal = 8.dp)
                .animateContentSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Image(
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .scalingClickable(
                        scaleInto = 0.85f,
                        onTap = {
                            viewModel?.requestGoogleSignIn(
                                webClientId = context.getString(R.string.firebase_web_client_id),
                                filterAuthorizedAccounts = true,
                                credentialManager = credentialManager,
                                context = context
                            )
                        }
                    ),
                painter = painterResource(LocalTheme.current.icons.googleSignUp),
                contentDescription = null
            )
            OutlinedButton(
                icon = Icons.Outlined.AlternateEmail,
                text = stringResource(R.string.login_password_method),
                onClick = {
                    navController?.navigate(NavigationTree.LoginPassword)
                }
            )
        }
    }
}