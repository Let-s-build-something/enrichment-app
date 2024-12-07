package ui.account.profile

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
import data.io.ApiErrorCode
import data.io.DELAY_BETWEEN_REQUESTS_SHORT
import koin.profileChangeModule
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.cancelChildren
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
fun DisplayNameChangeLauncher(
    modifier: Modifier = Modifier,
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    onDismissRequest: () -> Unit
) {
    loadKoinModules(profileChangeModule)
    val viewModel: ProfileChangeViewModel = koinViewModel()

    val coroutineScope = rememberCoroutineScope()
    val cancellableScope = rememberCoroutineScope()
    val focusRequester = remember { FocusRequester() }
    val snackbarHostState = LocalSnackbarHost.current

    val currentUser = viewModel.currentUser.collectAsState()
    val isLoading = viewModel.isLoading.collectAsState()

    val username = rememberSaveable(currentUser.value) {
        mutableStateOf(currentUser.value?.displayName ?: "")
    }
    val errorMessage = remember {
        mutableStateOf<String?>(null)
    }
    val validations = remember {
        mutableStateOf(listOf<FieldValidation>())
    }
    val isUsernameValid = validations.value.all { it.isValid || it.isRequired.not() }
            && errorMessage.value == null

    val respondToError: suspend (String?) -> Unit = { error: String? ->
        errorMessage.value = when(error) {
            ApiErrorCode.DUPLICATE.name -> {
                getString(Res.string.account_username_error_duplicate)
            }
            ApiErrorCode.INVALID_FORMAT.name-> {
                getString(Res.string.account_username_error_format)
            }
            else -> null
        }
    }

    LaunchedEffect(Unit) {
        viewModel.displayNameChangeResponse.collectLatest { res ->
            respondToError(res?.error?.errors?.firstOrNull())
            res?.success?.data?.displayName?.let { newUsername ->
                onDismissRequest()
                CoroutineScope(Dispatchers.Main).launch {
                    snackbarHostState?.showSnackbar(
                        message = getString(
                            Res.string.account_username_success,
                            newUsername
                        )
                    )
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        delay(400)
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
            if(validations.value.isEmpty()) {
                cancellableScope.coroutineContext.cancelChildren()
                cancellableScope.launch {
                    delay(DELAY_BETWEEN_REQUESTS_SHORT)
                    viewModel.validateDisplayName(username.value)
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        viewModel.displayNameValidationResponse.collectLatest { res ->
            respondToError(res?.error?.errors?.firstOrNull())
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
                currentUser.value?.displayName ?: stringResource(Res.string.account_username_empty)
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
            value = currentUser.value?.displayName ?: "",
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
                isEnabled = isUsernameValid
                        && errorMessage.value == null
                        && username.value != currentUser.value?.displayName,
                onClick = {
                    viewModel.requestDisplayNameChange(username.value)
                }
            )
        }
    }
}

const val USERNAME_MIN_LENGTH = 3
const val USERNAME_MAX_LENGTH = 48