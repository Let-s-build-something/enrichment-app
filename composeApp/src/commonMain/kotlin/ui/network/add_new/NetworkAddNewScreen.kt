package ui.network.add_new

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Tag
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import augmy.composeapp.generated.resources.Res
import augmy.composeapp.generated.resources.account_username_error_format
import augmy.composeapp.generated.resources.account_username_hint
import augmy.composeapp.generated.resources.error_general
import augmy.composeapp.generated.resources.network_inclusion_action
import augmy.composeapp.generated.resources.network_inclusion_description
import augmy.composeapp.generated.resources.network_inclusion_error_blocked
import augmy.composeapp.generated.resources.network_inclusion_error_duplicate
import augmy.composeapp.generated.resources.network_inclusion_error_non_existent
import augmy.composeapp.generated.resources.network_inclusion_error_non_format
import augmy.composeapp.generated.resources.network_inclusion_format_tag
import augmy.composeapp.generated.resources.network_inclusion_hint_tag
import augmy.composeapp.generated.resources.network_inclusion_success
import augmy.composeapp.generated.resources.network_inclusion_success_action
import augmy.composeapp.generated.resources.screen_network_new
import augmy.interactive.shared.ui.base.LocalNavController
import augmy.interactive.shared.ui.base.LocalSnackbarHost
import augmy.interactive.shared.ui.base.ModalScreenContent
import augmy.interactive.shared.ui.components.BrandHeaderButton
import augmy.interactive.shared.ui.components.input.EditFieldInput
import augmy.interactive.shared.ui.theme.LocalTheme
import base.BrandBaseScreen
import base.navigation.NavIconType
import base.navigation.NavigationArguments
import base.navigation.NavigationNode
import data.io.social.network.RequestCircleErrorCode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import ui.account.profile.USERNAME_MAX_LENGTH
import ui.account.profile.USERNAME_MIN_LENGTH

private const val REGEX_USER_TAG = """^[a-fA-F0-9]+${'$'}"""

/** Screen for including new users to one's social network */
@Composable
fun NetworkAddNewScreen(viewModel: NetworkAddNewViewModel = koinViewModel()) {
    val snackbarHostState = LocalSnackbarHost.current
    val navController = LocalNavController.current
    val isLoading = viewModel.isLoading.collectAsState()

    val inputDisplayName = rememberSaveable { mutableStateOf("") }
    val inputTag = rememberSaveable { mutableStateOf("") }
    val errorMessage = remember { mutableStateOf<String?>(null) }

    val isDisplayNameValid = with(inputDisplayName.value) {
        length in USERNAME_MIN_LENGTH..USERNAME_MAX_LENGTH
                && !startsWith(' ') && !endsWith(' ')
    }
    val isTagValid = with(inputTag.value) {
        length == 6 && REGEX_USER_TAG.toRegex().matches(this)
    }

    LaunchedEffect(Unit) {
        viewModel.response.collectLatest {
            it?.error?.let { error ->
                errorMessage.value = getString(
                    when(error.errors.firstOrNull()) {
                        RequestCircleErrorCode.DUPLICATE.name -> Res.string.network_inclusion_error_duplicate
                        RequestCircleErrorCode.NON_EXISTENT.name -> Res.string.network_inclusion_error_non_existent
                        RequestCircleErrorCode.INCORRECT_FORMAT.name -> Res.string.network_inclusion_error_non_format
                        RequestCircleErrorCode.USER_BLOCKED.name -> Res.string.network_inclusion_error_blocked
                        else -> Res.string.error_general
                    }
                )
            }
            it?.success?.data?.let { data ->
                CoroutineScope(Dispatchers.Main).launch {
                    if(snackbarHostState?.showSnackbar(
                            message = getString(Res.string.network_inclusion_success),
                            actionLabel = if(data.isInclusionImmediate == true && data.userUid != null) {
                                getString(Res.string.network_inclusion_success_action)
                            }else null,
                            duration = SnackbarDuration.Short
                        ) == SnackbarResult.ActionPerformed
                    ) {
                        navController?.navigate(NavigationNode.Conversation(userUid = data.userUid))
                    }
                }
                navController?.previousBackStackEntry
                    ?.savedStateHandle
                    ?.apply {
                        set(NavigationArguments.NETWORK_NEW_SUCCESS, true)
                    }
                navController?.popBackStack(route = NavigationNode.NetworkNew, inclusive = true)
            }
        }
    }

    BrandBaseScreen(
        title = stringResource(Res.string.screen_network_new),
        navIconType = NavIconType.CLOSE
    ) {
        ModalScreenContent(
            modifier = Modifier.verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = stringResource(Res.string.network_inclusion_description),
                style = LocalTheme.current.styles.category
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                EditFieldInput(
                    hint = stringResource(Res.string.account_username_hint),
                    value = inputDisplayName.value,
                    suggestText = if(!isDisplayNameValid) {
                        stringResource(Res.string.account_username_error_format)
                    } else null,
                    maxCharacters = USERNAME_MAX_LENGTH,
                    errorText = errorMessage.value,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Text,
                        imeAction = ImeAction.Next
                    ),
                    onValueChange = { value ->
                        errorMessage.value = null
                        inputDisplayName.value = value
                    }
                )
                EditFieldInput(
                    modifier = Modifier.weight(1f),
                    hint = stringResource(Res.string.network_inclusion_hint_tag),
                    value = inputTag.value,
                    maxCharacters = 6,
                    suggestText = if(!isTagValid) {
                        stringResource(Res.string.network_inclusion_format_tag)
                    } else null,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Text,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            if(isDisplayNameValid && isTagValid) {
                                viewModel.includeNewUser(
                                    inputDisplayName.value,
                                    inputTag.value
                                )
                            }
                        }
                    ),
                    leadingIcon = Icons.Outlined.Tag,
                    onValueChange = { value ->
                        inputTag.value = value
                    }
                )
            }

            BrandHeaderButton(
                modifier = Modifier.padding(top = 16.dp),
                isEnabled = isDisplayNameValid && isTagValid && errorMessage.value == null,
                isLoading = isLoading.value,
                text = stringResource(Res.string.network_inclusion_action),
                onClick = {
                    viewModel.includeNewUser(
                        inputDisplayName.value,
                        inputTag.value
                    )
                }
            )
        }
    }
}