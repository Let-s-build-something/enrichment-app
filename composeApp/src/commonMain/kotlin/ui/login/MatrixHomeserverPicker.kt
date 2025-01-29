package ui.login

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import augmy.composeapp.generated.resources.Res
import augmy.composeapp.generated.resources.button_confirm
import augmy.composeapp.generated.resources.login_homeserver_address
import augmy.composeapp.generated.resources.login_homeserver_address_error
import augmy.composeapp.generated.resources.login_homeserver_description
import augmy.composeapp.generated.resources.login_homeserver_heading
import augmy.interactive.shared.ext.scalingClickable
import augmy.interactive.shared.ui.components.BrandHeaderButton
import augmy.interactive.shared.ui.components.SimpleModalBottomSheet
import augmy.interactive.shared.ui.components.input.CustomTextField
import augmy.interactive.shared.ui.theme.LocalTheme
import data.io.DELAY_BETWEEN_REQUESTS_SHORT
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MatrixHomeserverPicker(
    viewModel: LoginViewModel,
    screenType: LoginScreenType,
    homeserver: String,
    onDismissRequest: () -> Unit,
    onSelect: (String) -> Unit
) {
    val isLoading = viewModel.isLoading.collectAsState()
    val homeServerResponse = viewModel.homeServerResponse.collectAsState()
    val cancellableScope = rememberCoroutineScope()
    val isValid = homeServerResponse.value?.state == LoginViewModel.HomeServerState.Valid
    val homeServerState = remember {
        TextFieldState(
            initialText = if(homeserver != AUGMY_HOME_SERVER && homeserver != MATRIX_HOME_SERVER) homeserver else ""
        )
    }

    LaunchedEffect(homeServerState.text) {
        cancellableScope.coroutineContext.cancelChildren()
        if(homeServerState.text.isNotEmpty()) {
            cancellableScope.launch {
                viewModel.setLoading(true)
                delay(DELAY_BETWEEN_REQUESTS_SHORT)
                viewModel.selectHomeServer(
                    screenType = screenType,
                    address = homeServerState.text.toString()
                )
            }
        }
    }

    SimpleModalBottomSheet(
        onDismissRequest = {
            if(!isValid) {
                viewModel.selectHomeServer(address = homeserver, screenType = screenType)
            }
            onDismissRequest()
        }
    ) {
        Text(
            text = stringResource(Res.string.login_homeserver_heading),
            style = LocalTheme.current.styles.subheading
        )
        Text(
            text = stringResource(Res.string.login_homeserver_description),
            style = LocalTheme.current.styles.regular
        )
        Row(
            modifier = Modifier
                .padding(top = 8.dp)
                .scalingClickable(hoverEnabled = false) {
                    homeServerState.setTextAndPlaceCursorAtEnd("")
                    onSelect(AUGMY_HOME_SERVER)
                }
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            RadioButton(
                selected = homeserver == AUGMY_HOME_SERVER && homeServerState.text.isBlank(),
                colors = LocalTheme.current.styles.radioButtonColors,
                onClick = {
                    onSelect(AUGMY_HOME_SERVER)
                }
            )
            Text(
                text = AUGMY_HOME_SERVER,
                style = LocalTheme.current.styles.title
            )
        }
        Row(
            modifier = Modifier
                .scalingClickable(hoverEnabled = false) {
                    homeServerState.setTextAndPlaceCursorAtEnd("")
                    onSelect(MATRIX_HOME_SERVER)
                }
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            RadioButton(
                selected = homeserver == MATRIX_HOME_SERVER && homeServerState.text.isBlank(),
                colors = LocalTheme.current.styles.radioButtonColors,
                onClick = {
                    homeServerState.setTextAndPlaceCursorAtEnd("")
                    onSelect(MATRIX_HOME_SERVER)
                }
            )
            Text(
                text = MATRIX_HOME_SERVER,
                style = LocalTheme.current.styles.title
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(.7f),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            RadioButton(
                selected = homeServerState.text.isNotBlank(),
                colors = LocalTheme.current.styles.radioButtonColors,
                onClick = {
                    onSelect(homeServerState.text.toString())
                }
            )
            CustomTextField(
                modifier = Modifier.weight(1f),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Text,
                    imeAction = ImeAction.Send
                ),
                onKeyboardAction = {
                    if(isValid) {
                        onSelect(homeServerState.text.toString())
                    }
                },
                hint = stringResource(Res.string.login_homeserver_address),
                errorText = if(!isValid && homeServerState.text.isNotEmpty()) {
                    stringResource(Res.string.login_homeserver_address_error)
                } else null,
                state = homeServerState,
                lineLimits = TextFieldLineLimits.SingleLine,
                shape = LocalTheme.current.shapes.rectangularActionShape
            )
        }

        BrandHeaderButton(
            modifier = Modifier
                .align(Alignment.End)
                .padding(top = 8.dp),
            text = stringResource(Res.string.button_confirm),
            isEnabled = isValid || homeServerState.text.isBlank(),
            isLoading = isLoading.value
        ) {
            if(isValid && homeServerState.text.isNotBlank()
                && homeServerState.text == homeServerResponse.value?.address
            ) {
                onSelect(homeServerState.text.toString())
            }
            onDismissRequest()
        }
    }
}

private const val MATRIX_HOME_SERVER = "matrix.org"
const val AUGMY_HOME_SERVER = "homeserver.augmy.org"
