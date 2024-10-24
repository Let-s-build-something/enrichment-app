package ui.login.username

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import augmy.composeapp.generated.resources.Res
import augmy.composeapp.generated.resources.account_username_empty
import augmy.composeapp.generated.resources.account_username_error_duplicate
import augmy.composeapp.generated.resources.account_username_error_format
import augmy.composeapp.generated.resources.account_username_error_long
import augmy.composeapp.generated.resources.account_username_error_short
import augmy.composeapp.generated.resources.account_username_error_spaces
import augmy.composeapp.generated.resources.account_username_hint
import augmy.composeapp.generated.resources.account_username_success
import augmy.composeapp.generated.resources.button_dismiss
import augmy.composeapp.generated.resources.username_change_launcher_confirm
import augmy.composeapp.generated.resources.username_change_launcher_content
import augmy.composeapp.generated.resources.username_change_launcher_title
import augmy.interactive.shared.ui.base.LocalSnackbarHost
import augmy.interactive.shared.ui.components.BrandHeaderButton
import augmy.interactive.shared.ui.components.CorrectionText
import augmy.interactive.shared.ui.components.ErrorHeaderButton
import augmy.interactive.shared.ui.components.SimpleModalBottomSheet
import augmy.interactive.shared.ui.components.input.EditFieldInput
import augmy.interactive.shared.ui.theme.LocalTheme
import data.io.social.username.UsernameChangeError
import koin.usernameChangeModule
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.context.loadKoinModules
import ui.login.FieldValidation

/**
 * Bottom sheet for notifying user about username change and enforcing it
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UsernameChangeLauncher(
    modifier: Modifier = Modifier,
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    onDismissRequest: () -> Unit
) {
    loadKoinModules(usernameChangeModule)
    val viewModel: UsernameChangeViewModel = koinViewModel()
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = LocalSnackbarHost.current
    val focusRequester = remember {
        FocusRequester()
    }

    val currentUser = viewModel.currentUser.collectAsState()
    val isLoading = viewModel.isLoading.collectAsState()

    val username = rememberSaveable(currentUser.value) {
        mutableStateOf(currentUser.value?.username ?: "")
    }
    val errorMessage = remember {
        mutableStateOf<String?>(null)
    }
    val validations = remember {
        mutableStateOf(listOf<FieldValidation>())
    }
    val isUsernameValid = validations.value.all { it.isValid || it.isRequired.not() }
            && errorMessage.value == null

    LaunchedEffect(Unit) {
        delay(500)
        focusRequester.requestFocus()
    }

    LaunchedEffect(username.value) {
        coroutineScope.launch(Dispatchers.Default) {
            validations.value = listOf(
                FieldValidation(
                    isValid = username.value.length <= USERNAME_MAX_LENGTH,
                    message = getString(Res.string.account_username_error_long),
                    isVisibleCorrect = false
                ),
                FieldValidation(
                    isValid = username.value.length >= USERNAME_MIN_LENGTH,
                    message = getString(Res.string.account_username_error_short),
                    isVisibleCorrect = false
                ),
                FieldValidation(
                    isValid = !username.value.startsWith(' ') && !username.value.endsWith(' '),
                    message = getString(Res.string.account_username_error_spaces),
                    isVisibleCorrect = false
                )
            )
        }
    }

    LaunchedEffect(Unit) {
        viewModel.response.collectLatest { res ->
            errorMessage.value = when(res?.error?.errors?.firstOrNull()) {
                UsernameChangeError.DUPLICATE.name -> {
                    getString(Res.string.account_username_error_duplicate)
                }
                UsernameChangeError.INVALID_FORMAT.name-> {
                    getString(Res.string.account_username_error_format)
                }
                else -> null
            }
            res?.success?.data?.username?.let { newUsername ->
                CoroutineScope(Dispatchers.Main).launch {
                    snackbarHostState?.showSnackbar(
                        message = getString(
                            Res.string.account_username_success,
                            newUsername
                        )
                    )
                }
                sheetState.hide()
            }
        }
    }

    SimpleModalBottomSheet(
        modifier = modifier,
        sheetState = sheetState,
        onDismissRequest = onDismissRequest
    ) {
        Text(
            text = stringResource(
                Res.string.username_change_launcher_title,
                currentUser.value?.username ?: stringResource(Res.string.account_username_empty)
            ),
            style = LocalTheme.current.styles.category
        )
        Text(
            modifier = Modifier.padding(top = 2.dp),
            text = stringResource(Res.string.username_change_launcher_content),
            style = LocalTheme.current.styles.regular
        )

        EditFieldInput(
            modifier = Modifier
                .padding(top = 16.dp)
                .focusRequester(focusRequester)
                .fillMaxWidth(0.8f),
            hint = stringResource(Res.string.account_username_hint),
            value = username.value,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Text,
                imeAction = ImeAction.Done
            ),
            maxCharacters = USERNAME_MAX_LENGTH,
            onValueChange = { value ->
                username.value = value
                errorMessage.value = null
            },
            isClearable = true,
            errorText = errorMessage.value ?: if(!isUsernameValid) " " else null,
            paddingValues = PaddingValues(start = 16.dp)
        )

        Column(
            modifier = Modifier
                .padding(top = 4.dp)
                .animateContentSize(),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            validations.value.forEach { validation ->
                AnimatedVisibility(!validation.isValid || validation.isVisibleCorrect) {
                    CorrectionText(
                        text = validation.message,
                        isCorrect = validation.isValid,
                        isRequired = validation.isRequired
                    )
                }
            }
        }

        Row(
            modifier = Modifier
                .padding(vertical = 16.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ErrorHeaderButton(
                modifier = Modifier.weight(1f),
                text = stringResource(Res.string.button_dismiss),
                onClick = {
                    coroutineScope.launch {
                        sheetState.hide()
                    }
                }
            )
            BrandHeaderButton(
                modifier = Modifier.weight(1f),
                isLoading = isLoading.value,
                text = stringResource(Res.string.username_change_launcher_confirm),
                isEnabled = isUsernameValid && errorMessage.value == null,
                onClick = {
                    viewModel.requestUsernameChange(username.value)
                }
            )
        }
    }
}

private const val USERNAME_MIN_LENGTH = 3
private const val USERNAME_MAX_LENGTH = 48