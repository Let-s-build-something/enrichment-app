package ui.login

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import augmy.composeapp.generated.resources.Res
import augmy.composeapp.generated.resources.accessibility_hide_password
import augmy.composeapp.generated.resources.accessibility_show_password
import augmy.composeapp.generated.resources.accessibility_sign_in_illustration
import augmy.composeapp.generated.resources.accessibility_sign_up_illustration
import augmy.composeapp.generated.resources.error_general
import augmy.composeapp.generated.resources.error_google_sign_in_unavailable
import augmy.composeapp.generated.resources.login_email_error
import augmy.composeapp.generated.resources.login_error_canceled
import augmy.composeapp.generated.resources.login_error_duplicate_email
import augmy.composeapp.generated.resources.login_error_invalid_credential
import augmy.composeapp.generated.resources.login_error_no_windows
import augmy.composeapp.generated.resources.login_error_security
import augmy.composeapp.generated.resources.login_password_action_go
import augmy.composeapp.generated.resources.login_password_condition_0
import augmy.composeapp.generated.resources.login_password_condition_1
import augmy.composeapp.generated.resources.login_password_condition_2
import augmy.composeapp.generated.resources.login_password_condition_empty
import augmy.composeapp.generated.resources.login_password_email_hint
import augmy.composeapp.generated.resources.login_password_password_hint
import augmy.composeapp.generated.resources.login_screen_type_sign_in
import augmy.composeapp.generated.resources.login_screen_type_sign_up
import augmy.composeapp.generated.resources.login_success_snackbar
import augmy.composeapp.generated.resources.login_success_snackbar_action
import augmy.composeapp.generated.resources.screen_login
import augmy.interactive.shared.ext.scalingClickable
import augmy.interactive.shared.ui.base.CustomSnackbarVisuals
import augmy.interactive.shared.ui.base.LocalNavController
import augmy.interactive.shared.ui.base.LocalScreenSize
import augmy.interactive.shared.ui.base.LocalSnackbarHost
import augmy.interactive.shared.ui.base.ModalScreenContent
import augmy.interactive.shared.ui.components.BrandHeaderButton
import augmy.interactive.shared.ui.components.CorrectionText
import augmy.interactive.shared.ui.components.DEFAULT_ANIMATION_LENGTH_LONG
import augmy.interactive.shared.ui.components.MinimalisticIcon
import augmy.interactive.shared.ui.components.MultiChoiceSwitch
import augmy.interactive.shared.ui.components.input.EditFieldInput
import augmy.interactive.shared.ui.components.rememberMultiChoiceState
import augmy.interactive.shared.ui.theme.LocalTheme
import base.BrandBaseScreen
import base.navigation.NavIconType
import base.navigation.NavigationNode
import components.AsyncImageThumbnail
import data.Asset
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

/**
 * Screen for logging into an account through various methods, including:
 * email + password, Google, and Apple ID
 */
@Composable
fun LoginScreen(viewModel: LoginViewModel = koinViewModel()) {
    val navController = LocalNavController.current
    val snackbarHostState = LocalSnackbarHost.current

    val clientStatus = viewModel.clientStatus.collectAsState()
    val isWaitingForResult = remember { mutableStateOf(false) }
    val errorMessage = remember {
        mutableStateOf<String?>(null)
    }
    val password = remember { mutableStateOf("") }
    val screenStateIndex = rememberSaveable(clientStatus.value) {
        mutableStateOf(clientStatus.value.ordinal)
    }
    val screenType = LoginScreenType.entries[screenStateIndex.value]
    val validations = remember {
        mutableStateOf(listOf<FieldValidation>())
    }

    val coroutineScope = rememberCoroutineScope()
    val switchScreenState = rememberMultiChoiceState(
        tabs = mutableListOf(
            stringResource(Res.string.login_screen_type_sign_up),
            stringResource(Res.string.login_screen_type_sign_in)
        ),
        selectedTabIndex = screenStateIndex,
        onSelectionChange = {
            screenStateIndex.value = it
            errorMessage.value = null
        }
    )

    LaunchedEffect(Unit) {
        if(viewModel.currentUser.value != null) navController?.popBackStack()
    }

    LaunchedEffect(password.value) {
        coroutineScope.launch(Dispatchers.Default) {
            validations.value = listOf(
                FieldValidation(
                    isValid = password.value.length >= PASSWORD_MIN_LENGTH,
                    message = getString(if(password.value.isEmpty()) {
                        Res.string.login_password_condition_empty
                    }else Res.string.login_password_condition_0)
                ),
                FieldValidation(
                    isValid = password.value.contains("""\d""".toRegex()),
                    message = getString(Res.string.login_password_condition_1),
                    isRequired = false
                ),
                FieldValidation(
                    isValid = password.value.isNotEmpty()
                            && password.value.matches("""[A-Za-z0-9]+""".toRegex()).not(),
                    message = getString(Res.string.login_password_condition_2),
                    isRequired = false
                )
            )
        }
    }

    LaunchedEffect(Unit) {
        viewModel.loginResult.collectLatest { res ->
            errorMessage.value = when(res) {
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
                    null
                }
                LoginResultType.NO_GOOGLE_CREDENTIALS -> {
                    coroutineScope.launch {
                        snackbarHostState?.showSnackbar(
                            visuals = CustomSnackbarVisuals(
                                message = getString(Res.string.error_google_sign_in_unavailable),
                                isError = true
                            )
                        )
                    }
                    null
                }
                LoginResultType.CANCELLED -> getString(Res.string.login_error_canceled)
                LoginResultType.INVALID_CREDENTIAL -> getString(Res.string.login_error_invalid_credential)
                LoginResultType.NO_WINDOW -> getString(Res.string.login_error_no_windows)
                LoginResultType.AUTH_SECURITY -> getString(Res.string.login_error_security)
                LoginResultType.EMAIL_EXISTS -> getString(Res.string.login_error_duplicate_email)
                else -> getString(Res.string.error_general)
            }
            isWaitingForResult.value = false
        }
    }

    BrandBaseScreen(
        modifier = Modifier.imePadding(),
        title = stringResource(Res.string.screen_login),
        navIconType = NavIconType.BACK
    ) {
        ModalScreenContent(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .animateContentSize(),
            verticalArrangement = Arrangement.spacedBy(LocalTheme.current.shapes.betweenItemsSpace)
        ) {
            MultiChoiceSwitch(
                modifier = Modifier.fillMaxWidth(),
                shape = LocalTheme.current.shapes.rectangularActionShape,
                state = switchScreenState
            )

            Crossfade(
                modifier = Modifier.fillMaxWidth(),
                targetState = screenType,
                animationSpec = tween(DEFAULT_ANIMATION_LENGTH_LONG),
                label = ""
            ) { type ->
                Box(modifier = Modifier.fillMaxWidth()) {
                    AsyncImageThumbnail(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.Center)
                            .heightIn(
                                min = 150.dp,
                                max = (LocalScreenSize.current.height / 3).dp
                            ),
                        image = if(type == LoginScreenType.SIGN_UP) {
                            Asset.Image.SignUp
                        }else Asset.Image.SignIn,
                        contentDescription = stringResource(
                            if(type == LoginScreenType.SIGN_UP) {
                                Res.string.accessibility_sign_up_illustration
                            }else Res.string.accessibility_sign_in_illustration
                        )
                    )
                }
            }
            LoginScreenContent(
                viewModel = viewModel,
                screenType = screenType,
                validations = validations.value,
                errorMessage = errorMessage,
                isWaitingForResult = isWaitingForResult,
                password = password
            )
        }
    }
}

@Composable
private fun ColumnScope.LoginScreenContent(
    viewModel: LoginViewModel,
    screenType: LoginScreenType,
    validations: List<FieldValidation>,
    errorMessage: MutableState<String?>,
    password: MutableState<String>,
    isWaitingForResult: MutableState<Boolean>
) {
    val isEmailFocused = remember { mutableStateOf(false) }
    val passwordVisible = remember { mutableStateOf(false) }
    val email = remember { mutableStateOf("") }
    val isPasswordValid = validations.all { it.isValid || it.isRequired.not() }
    val isEmailValid = emailAddressRegex.matches(email.value) && errorMessage.value == null

    EditFieldInput(
        modifier = Modifier
            .fillMaxWidth()
            .onFocusChanged { state ->
                isEmailFocused.value = state.isFocused
            },
        hint = stringResource(Res.string.login_password_email_hint),
        value = "",
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Email,
            imeAction = ImeAction.Next
        ),
        onValueChange = { value ->
            errorMessage.value = null
            email.value = value.text
        },
        errorText = if(isEmailValid || email.value.isEmpty() || isEmailFocused.value) {
            null
        } else errorMessage.value ?: stringResource(Res.string.login_email_error),
        paddingValues = PaddingValues(start = 16.dp)
    )

    EditFieldInput(
        modifier = Modifier.fillMaxWidth(),
        hint = stringResource(Res.string.login_password_password_hint),
        value = "",
        isCorrect = isPasswordValid,
        errorText = if(isPasswordValid.not() && password.value.isNotEmpty()) " " else null,
        visualTransformation = if (passwordVisible.value) {
            VisualTransformation.None
        } else PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Password,
            imeAction = ImeAction.Done
        ),
        keyboardActions = KeyboardActions(
            onDone = {
                if(isPasswordValid && isEmailValid) {
                    isWaitingForResult.value = true
                    viewModel.signUpWithPassword(
                        email = email.value,
                        password = password.value,
                        screenType = screenType
                    )
                }
            }
        ),
        trailingIcon = {
            Crossfade(
                targetState = passwordVisible.value, label = "",
            ) { isPasswordVisible ->
                MinimalisticIcon(
                    contentDescription = stringResource(
                        if(isPasswordVisible) {
                            Res.string.accessibility_hide_password
                        }else Res.string.accessibility_show_password
                    ),
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
            password.value = value.text.take(PASSWORD_MAX_LENGTH)
            errorMessage.value = null
        },
        paddingValues = PaddingValues(start = 16.dp)
    )

    AnimatedVisibility(screenType == LoginScreenType.SIGN_UP) {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            validations.forEach { validation ->
                CorrectionText(
                    text = validation.message,
                    isCorrect = validation.isValid,
                    isRequired = validation.isRequired
                )
            }
        }
    }

    BrandHeaderButton(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 16.dp, start = 16.dp, end = 16.dp),
        text = stringResource(Res.string.login_password_action_go),
        isEnabled = isPasswordValid && isEmailValid && isWaitingForResult.value.not(),
        isLoading = isWaitingForResult.value,
        onClick = {
            isWaitingForResult.value = true
            viewModel.signUpWithPassword(
                email = email.value,
                password = password.value,
                screenType = screenType
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
                                when(option) {
                                    SingInServiceOption.GOOGLE -> viewModel.requestGoogleSignIn()
                                    SingInServiceOption.APPLE -> viewModel.requestAppleSignIn()
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

private const val PASSWORD_MAX_LENGTH = 64

/**
 * Email address pattern, same as [android.util.Patterns.EMAIL_ADDRESS]
 */
private val emailAddressRegex = """[a-zA-Z0-9+._%-+]{1,256}@[a-zA-Z0-9][a-zA-Z0-9-]{0,64}(\.[a-zA-Z0-9][a-zA-Z0-9-]{0,25})+""".toRegex()

private const val PASSWORD_MIN_LENGTH = 8

/** Type of interface for login screen */
enum class LoginScreenType {
    SIGN_UP,
    SIGN_IN
}