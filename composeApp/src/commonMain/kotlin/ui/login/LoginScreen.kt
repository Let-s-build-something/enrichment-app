package ui.login

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import base.BrandBaseScreen
import base.navigation.NavIconType
import base.navigation.NavigationNode
import chat.enrichment.shared.ui.base.LocalNavController
import chat.enrichment.shared.ui.base.LocalSnackbarHost
import chat.enrichment.shared.ui.components.BrandHeaderButton
import chat.enrichment.shared.ui.components.CorrectionText
import chat.enrichment.shared.ui.components.MinimalisticIcon
import chat.enrichment.shared.ui.components.input.EditFieldInput
import chat.enrichment.shared.ui.theme.LocalTheme
import chatenrichment.composeapp.generated.resources.Res
import chatenrichment.composeapp.generated.resources.error_google_sign_in_unavailable
import chatenrichment.composeapp.generated.resources.firebase_web_client_id
import chatenrichment.composeapp.generated.resources.login_password_action_go
import chatenrichment.composeapp.generated.resources.login_password_email_error
import chatenrichment.composeapp.generated.resources.login_password_email_hint
import chatenrichment.composeapp.generated.resources.login_password_password_condition_0
import chatenrichment.composeapp.generated.resources.login_password_password_condition_1
import chatenrichment.composeapp.generated.resources.login_password_password_condition_2
import chatenrichment.composeapp.generated.resources.login_password_password_condition_empty
import chatenrichment.composeapp.generated.resources.login_password_password_hint
import chatenrichment.composeapp.generated.resources.login_success_snackbar
import chatenrichment.composeapp.generated.resources.login_success_snackbar_action
import chatenrichment.composeapp.generated.resources.screen_login
import future_shared_module.ext.scalingClickable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.annotation.KoinExperimentalAPI

/**
 * Email address pattern, same as [android.util.Patterns.EMAIL_ADDRESS]
 */
private val emailAddressRegex = """[a-zA-Z0-9+._%-+]{1,256}@[a-zA-Z0-9][a-zA-Z0-9-]{0,64}(\.[a-zA-Z0-9][a-zA-Z0-9-]{0,25})+""".toRegex()

private const val PASSWORD_MIN_LENGTH = 9

/**
 * Screen for logging into an account through various methods, including:
 * email + password, Google, and Apple ID
 */
@OptIn(KoinExperimentalAPI::class)
@Composable
fun LoginScreen(viewModel: LoginViewModel = koinViewModel()) {
    val navController = LocalNavController.current
    val snackbarHostState = LocalSnackbarHost.current

    val coroutineScope = rememberCoroutineScope()

    val isWaitingForResult = remember { mutableStateOf(false) }
    val email = remember { mutableStateOf("") }
    val password = remember { mutableStateOf("") }
    val passwordVisible = remember { mutableStateOf(false) }
    val validations = remember {
        mutableStateOf(listOf<PasswordValidation>())
    }

    LaunchedEffect(password.value) {
        coroutineScope.launch(Dispatchers.Default) {
            validations.value = listOf(
                PasswordValidation(
                    isValid = password.value.length >= PASSWORD_MIN_LENGTH,
                    message = getString(if(password.value.isEmpty()) {
                        Res.string.login_password_password_condition_empty
                    }else Res.string.login_password_password_condition_0),
                    isRequired = true
                ),
                PasswordValidation(
                    isValid = password.value.contains("""\d""".toRegex()),
                    message = getString(Res.string.login_password_password_condition_1)
                ),
                PasswordValidation(
                    isValid = password.value.isNotEmpty()
                            && password.value.matches("""[A-Za-z0-9]+""".toRegex()).not(),
                    message = getString(Res.string.login_password_password_condition_2)
                )
            )
        }
    }
    val isPasswordValid = remember {
        derivedStateOf {
            validations.value.all { it.isValid || it.isRequired.not() }
        }
    }
    val isEmailValid = emailAddressRegex.matches(email.value)

    LaunchedEffect(Unit) {
        viewModel.loginResult.collectLatest { res ->
            when(res) {
                LoginResultType.NO_GOOGLE_CREDENTIALS -> {
                    CoroutineScope(Dispatchers.Main).launch {
                        snackbarHostState?.showSnackbar(
                            message = getString(Res.string.error_google_sign_in_unavailable)
                        )
                    }
                }
                LoginResultType.SUCCESS -> {
                    CoroutineScope(Dispatchers.Main).launch {
                        if(snackbarHostState?.showSnackbar(
                            message = getString(Res.string.login_success_snackbar),
                            actionLabel = getString(Res.string.login_success_snackbar_action),
                            duration = SnackbarDuration.Short
                        ) == SnackbarResult.ActionPerformed) {
                            navController?.navigate(NavigationNode.Water)
                        }
                    }
                    navController?.popBackStack(NavigationNode.Login, inclusive = true)
                }
                else -> {}
            }
            isWaitingForResult.value = false
        }
    }

    BrandBaseScreen(
        modifier = Modifier.imePadding(),
        title = stringResource(Res.string.screen_login),
        navIconType = NavIconType.BACK
    ) {
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .widthIn(max = MaxModalWidthDp.dp)
                .fillMaxHeight()
                .padding(
                    top = 24.dp,
                    start = 16.dp,
                    end = 16.dp
                ),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalAlignment = Alignment.Start
        ) {
            EditFieldInput(
                modifier = Modifier.fillMaxWidth(),
                hint = stringResource(Res.string.login_password_email_hint),
                value = "",
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Email,
                    imeAction = ImeAction.Next
                ),
                onValueChange = { value ->
                    email.value = value
                },
                errorText = if(isEmailValid || email.value.isEmpty()) {
                    null
                } else stringResource(Res.string.login_password_email_error),
                paddingValues = PaddingValues(start = 16.dp)
            )

            EditFieldInput(
                modifier = Modifier.fillMaxWidth(),
                hint = stringResource(Res.string.login_password_password_hint),
                value = "",
                isCorrect = isPasswordValid.value,
                errorText = if(isPasswordValid.value.not() && password.value.isNotEmpty()) " " else null,
                visualTransformation = if (passwordVisible.value) {
                    VisualTransformation.None
                } else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(
                    onDone = {
                        if(isPasswordValid.value && isEmailValid) {
                            isWaitingForResult.value = true
                            viewModel.signUpWithPassword(
                                email = email.value,
                                password = password.value
                            )
                        }
                    }
                ),
                trailingIcon = {
                    Crossfade(
                        targetState = passwordVisible.value, label = "",
                    ) { isPasswordVisible ->
                        MinimalisticIcon(
                            contentDescription = "Clear",
                            imageVector = if(isPasswordVisible) {
                                Icons.Outlined.Visibility
                            }else Icons.Outlined.VisibilityOff,
                            tint = LocalTheme.current.colors.secondary
                        ) {
                            passwordVisible.value = !passwordVisible.value
                        }
                    }
                },
                onValueChange = { value ->
                    password.value = value.take(PASSWORD_MAX_LENGTH)
                },
                paddingValues = PaddingValues(start = 16.dp)
            )
            validations.value.forEach { validation ->
                CorrectionText(
                    text = validation.message,
                    isCorrect = validation.isValid,
                    isRequired = validation.isRequired
                )
            }

            BrandHeaderButton(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp, start = 16.dp, end = 16.dp),
                text = stringResource(Res.string.login_password_action_go),
                isEnabled = isPasswordValid.value && isEmailValid && isWaitingForResult.value.not(),
                isLoading = isWaitingForResult.value,
                onClick = {
                    isWaitingForResult.value = true
                    viewModel.signUpWithPassword(
                        email = email.value,
                        password = password.value
                    )
                }
            )
            AnimatedVisibility(
                modifier = Modifier.align(alignment = Alignment.CenterHorizontally),
                visible = isWaitingForResult.value.not()
            ) {
                Row(
                    modifier = Modifier
                        .padding(top = 4.dp)
                        .wrapContentHeight(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    viewModel.availableOptions.forEach { option ->
                        Image(
                            modifier = Modifier
                                .scalingClickable(
                                    onTap = {
                                        coroutineScope.launch {
                                            when(option) {
                                                SingInServiceOption.GOOGLE -> {
                                                    viewModel.requestGoogleSignIn(
                                                        webClientId = getString(Res.string.firebase_web_client_id)
                                                    )
                                                }
                                                SingInServiceOption.APPLE -> {
                                                    viewModel.requestAppleSignIn()
                                                }
                                            }
                                        }
                                    }
                                ),
                            painter = painterResource(
                                when(option) {
                                    SingInServiceOption.GOOGLE -> LocalTheme.current.icons.googleSignUp
                                    SingInServiceOption.APPLE -> LocalTheme.current.icons.appleSignUp
                                }
                            ),
                            contentDescription = null
                        )
                    }
                }
            }
        }
    }
}
private const val PASSWORD_MAX_LENGTH = 64

/** Maximum width of a modal element - this can include a screen in case the device is a desktop */
const val MaxModalWidthDp = 520