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
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.TextObfuscationMode
import androidx.compose.foundation.text.input.delete
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AlternateEmail
import androidx.compose.material.icons.outlined.Face
import androidx.compose.material.icons.outlined.Handshake
import androidx.compose.material.icons.outlined.Key
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.lifecycle.Lifecycle
import augmy.composeapp.generated.resources.Res
import augmy.composeapp.generated.resources.accessibility_hide_password
import augmy.composeapp.generated.resources.accessibility_show_password
import augmy.composeapp.generated.resources.accessibility_sign_in_illustration
import augmy.composeapp.generated.resources.accessibility_sign_up_illustration
import augmy.composeapp.generated.resources.button_confirm
import augmy.composeapp.generated.resources.dialog_recaptcha_title
import augmy.composeapp.generated.resources.dialog_terms_message
import augmy.composeapp.generated.resources.dialog_terms_title
import augmy.composeapp.generated.resources.error_general
import augmy.composeapp.generated.resources.error_google_sign_in_unavailable
import augmy.composeapp.generated.resources.ic_robot
import augmy.composeapp.generated.resources.login_agreement
import augmy.composeapp.generated.resources.login_email_error
import augmy.composeapp.generated.resources.login_email_verification_action
import augmy.composeapp.generated.resources.login_email_verification_heading
import augmy.composeapp.generated.resources.login_email_verification_message
import augmy.composeapp.generated.resources.login_email_verification_repeat
import augmy.composeapp.generated.resources.login_error_canceled
import augmy.composeapp.generated.resources.login_error_duplicate_email
import augmy.composeapp.generated.resources.login_error_duplicate_username
import augmy.composeapp.generated.resources.login_error_invalid_credential
import augmy.composeapp.generated.resources.login_error_no_windows
import augmy.composeapp.generated.resources.login_error_security
import augmy.composeapp.generated.resources.login_matrix_homeserver
import augmy.composeapp.generated.resources.login_password_action_go
import augmy.composeapp.generated.resources.login_password_condition_0
import augmy.composeapp.generated.resources.login_password_condition_1
import augmy.composeapp.generated.resources.login_password_condition_2
import augmy.composeapp.generated.resources.login_password_condition_empty
import augmy.composeapp.generated.resources.login_password_email_hint
import augmy.composeapp.generated.resources.login_password_password_hint
import augmy.composeapp.generated.resources.login_privacy_policy
import augmy.composeapp.generated.resources.login_screen_type_sign_in
import augmy.composeapp.generated.resources.login_screen_type_sign_up
import augmy.composeapp.generated.resources.login_success_snackbar
import augmy.composeapp.generated.resources.login_success_snackbar_action
import augmy.composeapp.generated.resources.login_terms_of_use
import augmy.composeapp.generated.resources.login_username_condition_0
import augmy.composeapp.generated.resources.login_username_condition_1
import augmy.composeapp.generated.resources.login_username_hint
import augmy.composeapp.generated.resources.no_email_client_error
import augmy.composeapp.generated.resources.screen_login
import augmy.interactive.shared.ext.scalingClickable
import augmy.interactive.shared.ui.base.CustomSnackbarVisuals
import augmy.interactive.shared.ui.base.LocalNavController
import augmy.interactive.shared.ui.base.LocalScreenSize
import augmy.interactive.shared.ui.base.LocalSnackbarHost
import augmy.interactive.shared.ui.base.ModalScreenContent
import augmy.interactive.shared.ui.components.BrandHeaderButton
import augmy.interactive.shared.ui.components.ComponentHeaderButton
import augmy.interactive.shared.ui.components.CorrectionText
import augmy.interactive.shared.ui.components.DEFAULT_ANIMATION_LENGTH_LONG
import augmy.interactive.shared.ui.components.MinimalisticIcon
import augmy.interactive.shared.ui.components.MultiChoiceSwitch
import augmy.interactive.shared.ui.components.SimpleModalBottomSheet
import augmy.interactive.shared.ui.components.dialog.AlertDialog
import augmy.interactive.shared.ui.components.dialog.ButtonState
import augmy.interactive.shared.ui.components.input.CustomTextField
import augmy.interactive.shared.ui.components.input.DELAY_BETWEEN_TYPING_SHORT
import augmy.interactive.shared.ui.components.rememberMultiChoiceState
import augmy.interactive.shared.ui.theme.LocalTheme
import augmy.interactive.shared.ui.utils.LifecycleListener
import base.BrandBaseScreen
import base.navigation.NavIconType
import base.navigation.NavigationNode
import base.utils.Matrix
import base.utils.openEmail
import base.utils.openLink
import com.multiplatform.webview.jsbridge.IJsMessageHandler
import com.multiplatform.webview.jsbridge.JsMessage
import com.multiplatform.webview.jsbridge.rememberWebViewJsBridge
import com.multiplatform.webview.util.KLogSeverity
import com.multiplatform.webview.web.WebView
import com.multiplatform.webview.web.WebViewNavigator
import com.multiplatform.webview.web.rememberWebViewNavigator
import com.multiplatform.webview.web.rememberWebViewState
import components.AsyncImageThumbnail
import components.buildAnnotatedLink
import data.Asset
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.resources.vectorResource
import org.koin.compose.viewmodel.koinViewModel
import ui.conversation.components.gif.GifImage

/**
 * Screen for logging into an account through various methods, including:
 * email + password, Google, and Apple ID
 */
@Composable
fun LoginScreen(viewModel: LoginViewModel = koinViewModel()) {
    val navController = LocalNavController.current
    val snackbarHostState = LocalSnackbarHost.current

    val clientStatus = viewModel.clientStatus.collectAsState()
    val matrixProgress = viewModel.matrixProgress.collectAsState()
    val errorMessage = remember {
        mutableStateOf<String?>(null)
    }
    val passwordState = remember { TextFieldState() }
    val usernameState = remember { TextFieldState() }
    val isMatrixMode = rememberSaveable { mutableStateOf(false) }
    val screenStateIndex = rememberSaveable {
        mutableStateOf(clientStatus.value.ordinal)
    }
    val passwordValidations = remember {
        mutableStateOf(listOf<FieldValidation>())
    }
    val usernameValidations = remember {
        mutableStateListOf<FieldValidation>()
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
    LaunchedEffect(clientStatus.value) {
        switchScreenState.selectedTabIndex.value = clientStatus.value.ordinal
    }

    LaunchedEffect(Unit) {
        if(viewModel.currentUser.value != null) navController?.popBackStack()
    }

    LaunchedEffect(passwordState.text) {
        errorMessage.value = null
        coroutineScope.launch(Dispatchers.Default) {
            passwordValidations.value = listOf(
                FieldValidation(
                    isValid = passwordState.text.length >= PASSWORD_MIN_LENGTH,
                    message = getString(if(passwordState.text.isEmpty()) {
                        Res.string.login_password_condition_empty
                    }else Res.string.login_password_condition_0)
                ),
                FieldValidation(
                    isValid = passwordState.text.contains("""\d""".toRegex()),
                    message = getString(Res.string.login_password_condition_1),
                    isRequired = false
                ),
                FieldValidation(
                    isValid = passwordState.text.isNotEmpty()
                            && passwordState.text.matches("""[A-Za-z0-9]+""".toRegex()).not(),
                    message = getString(Res.string.login_password_condition_2),
                    isRequired = false
                )
            )
        }
    }

    LaunchedEffect(usernameState.text, screenStateIndex.value) {
        errorMessage.value = null
        coroutineScope.launch(Dispatchers.Default) {
            usernameValidations.clear()
            usernameValidations.addAll(
                listOf(
                    FieldValidation(
                        isValid = usernameState.text.none { it.isUpperCase() },
                        message = getString(Res.string.login_username_condition_0)
                    ),
                    FieldValidation(
                        isValid = strippedUsernameRegex.matches(usernameState.text),
                        message = getString(Res.string.login_username_condition_1)
                    )
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
                LoginResultType.USERNAME_EXISTS -> {
                    if(screenStateIndex.value == LoginScreenType.SIGN_UP.ordinal) {
                        usernameValidations.add(
                            FieldValidation(
                                isValid = false,
                                message = getString(Res.string.login_error_duplicate_username),
                                isRequired = true
                            )
                        )
                    }
                    errorMessage.value
                }
                else -> getString(Res.string.error_general)
            }
            viewModel.setLoading(false)
        }
    }

    matrixProgress.value?.let { progress ->
        val flow = progress.response?.flows?.firstOrNull()
        flow?.stages?.getOrNull(progress.index)?.let { stage ->
            MatrixProgressStage(
                stage = stage,
                viewModel = viewModel
            )
        }
    }

    BrandBaseScreen(
        modifier = Modifier.imePadding(),
        title = stringResource(Res.string.screen_login),
        navIconType = NavIconType.BACK
    ) {
        ModalScreenContent(
            modifier = Modifier.verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.Center
        ) {
            MultiChoiceSwitch(
                modifier = Modifier.fillMaxWidth(),
                shape = LocalTheme.current.shapes.rectangularActionShape,
                state = switchScreenState
            )

            Crossfade(
                modifier = Modifier
                    .padding(top = LocalTheme.current.shapes.betweenItemsSpace)
                    .fillMaxWidth(),
                targetState = LoginScreenType.entries[screenStateIndex.value],
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
                screenStateIndex = screenStateIndex,
                passwordValidations = passwordValidations.value,
                usernameValidations = usernameValidations,
                errorMessage = errorMessage,
                passwordState = passwordState,
                usernameState = usernameState,
                isMatrixMode = isMatrixMode
            )
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun ColumnScope.LoginScreenContent(
    viewModel: LoginViewModel,
    screenStateIndex: MutableState<Int>,
    passwordValidations: List<FieldValidation>,
    usernameValidations: List<FieldValidation>,
    errorMessage: MutableState<String?>,
    passwordState: TextFieldState,
    usernameState: TextFieldState,
    isMatrixMode: MutableState<Boolean>
) {
    val isLoading = viewModel.isLoading.collectAsState()
    val homeServerResponse = viewModel.homeServerResponse.collectAsState()
    val isEmailFocused = remember { mutableStateOf(false) }
    val passwordVisible = remember { mutableStateOf(false) }
    val emailState = remember { TextFieldState() }
    val homeServer = rememberSaveable { mutableStateOf(AUGMY_HOME_SERVER) }
    val showHomeServerPicker = remember { mutableStateOf(false) }

    val screenType = LoginScreenType.entries[screenStateIndex.value]
    val isPasswordValid = passwordValidations.all { it.isValid || it.isRequired.not() }
    val isEmailValid = emailAddressRegex.matches(emailState.text) && errorMessage.value == null
    val supportsEmail = true // TODO either implement Matrix in Firebase, or get rid of Firebase
    /*!isMatrixMode.value || homeServerResponse.value?.plan?.flows?.any {
        it.stages?.contains(LoginEmail) == true
    } != false*/

    val sendRequest = {
        viewModel.setLoading(true)
        viewModel.signUpWithPassword(
            email = emailState.text.toString(),
            password = passwordState.text.toString(),
            screenType = screenType,
            username = if(isMatrixMode.value) usernameState.text.toString() else null,
            isMatrix = isMatrixMode.value
        )
    }

    if(showHomeServerPicker.value) {
        MatrixHomeserverPicker(
            viewModel = viewModel,
            screenType = screenType,
            homeserver = homeServer.value,
            onDismissRequest = { showHomeServerPicker.value = false },
            onSelect = {
                homeServer.value = it
                viewModel.selectHomeServer(
                    screenType = screenType,
                    address = it
                )
            }
        )
    }

    LaunchedEffect(emailState.text) {
        errorMessage.value = null
    }

    LaunchedEffect(screenType) {
        viewModel.selectHomeServer(
            screenType = screenType,
            address = homeServer.value
        )
    }

    AnimatedVisibility(isMatrixMode.value && homeServerResponse.value?.supportsEmail != true) {
        val cancellableScope = rememberCoroutineScope()
        val error = usernameValidations.find { it.isRequired && !it.isValid }?.message

        if(screenType == LoginScreenType.SIGN_UP) {
            LaunchedEffect(usernameState.text) {
                cancellableScope.coroutineContext.cancelChildren()
                cancellableScope.launch {
                    delay(DELAY_BETWEEN_TYPING_SHORT)
                    viewModel.validateUsername(
                        address = homeServer.value,
                        username = usernameState.text.toString()
                    )
                }
            }
        }

        CustomTextField(
            modifier = Modifier
                .padding(top = LocalTheme.current.shapes.betweenItemsSpace)
                .fillMaxWidth(),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Text,
                imeAction = ImeAction.Next
            ),
            prefixIcon = Icons.Outlined.Face,
            hint = stringResource(Res.string.login_username_hint),
            errorText = if(usernameState.text.isEmpty()) {
                null
            } else error,
            state = usernameState,
            lineLimits = TextFieldLineLimits.SingleLine,
            shape = LocalTheme.current.shapes.rectangularActionShape
        )
    }
    AnimatedVisibility(visible = supportsEmail) {
        CustomTextField(
            modifier = Modifier
                .padding(top = LocalTheme.current.shapes.betweenItemsSpace)
                .fillMaxWidth()
                .onFocusChanged { state ->
                    isEmailFocused.value = state.isFocused
                },
            hint = stringResource(Res.string.login_password_email_hint),
            prefixIcon = Icons.Outlined.AlternateEmail,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Email,
                imeAction = ImeAction.Next
            ),
            state = emailState,
            errorText = if(isEmailValid || emailState.text.isEmpty() || isEmailFocused.value) {
                null
            } else errorMessage.value ?: stringResource(Res.string.login_email_error),
            paddingValues = PaddingValues(start = 16.dp)
        )
    }

    CustomTextField(
        modifier = Modifier
            .padding(top = LocalTheme.current.shapes.betweenItemsSpace)
            .fillMaxWidth(),
        hint = stringResource(Res.string.login_password_password_hint),
        prefixIcon = Icons.Outlined.Key,
        state = passwordState,
        isCorrect = isPasswordValid,
        errorText = if(isPasswordValid.not() && passwordState.text.isNotEmpty()) " " else null,
        textObfuscationMode = if(passwordVisible.value) {
            TextObfuscationMode.Visible
        }else TextObfuscationMode.RevealLastTyped,
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Password,
            imeAction = ImeAction.Done
        ),
        onKeyboardAction = {
            if(isPasswordValid && isEmailValid) {
                sendRequest()
            }
        },
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
        inputTransformation = {
            this.delete(PASSWORD_MAX_LENGTH, this.length)
        },
        paddingValues = PaddingValues(start = 16.dp)
    )

    AnimatedVisibility(screenType == LoginScreenType.SIGN_UP) {
        Column(
            modifier = Modifier
                .padding(top = LocalTheme.current.shapes.betweenItemsSpace)
                .animateContentSize()
        ) {
            passwordValidations.forEach { validation ->
                CorrectionText(
                    modifier = Modifier.padding(bottom = 2.dp),
                    text = validation.message,
                    isCorrect = validation.isValid,
                    isRequired = validation.isRequired
                )
            }
        }
    }

    BrandHeaderButton(
        modifier = Modifier
            .padding(top = LocalTheme.current.shapes.betweenItemsSpace)
            .fillMaxWidth()
            .padding(top = 16.dp, start = 16.dp, end = 16.dp),
        text = stringResource(Res.string.login_password_action_go),
        isEnabled = isPasswordValid && isEmailValid && isLoading.value.not(),
        isLoading = isLoading.value,
        onClick = sendRequest
    )

    AnimatedVisibility(
        modifier = Modifier.align(Alignment.CenterHorizontally),
        visible = screenType == LoginScreenType.SIGN_UP
    ) {
        Text(
            modifier = Modifier
                .padding(top = LocalTheme.current.shapes.betweenItemsSpace)
                .animateContentSize()
                .align(alignment = Alignment.CenterHorizontally)
                .padding(horizontal = 8.dp),
            text = buildAnnotatedLink(
                text = stringResource(Res.string.login_agreement),
                linkTexts = listOf(
                    stringResource(Res.string.login_terms_of_use),
                    stringResource(Res.string.login_privacy_policy)
                ),
                onLinkClicked = { _, index ->
                    when(index) {
                        0 -> openLink("https://augmy.org/terms-of-use")
                        1 -> openLink("https://augmy.org/privacy-policy")
                    }
                }
            ),
            style = LocalTheme.current.styles.regular
        )
    }

    AnimatedVisibility(isLoading.value.not()) {
        Row(
            modifier = Modifier
                .padding(top = LocalTheme.current.shapes.betweenItemsSpace)
                .fillMaxWidth()
                .align(alignment = Alignment.CenterHorizontally),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier
                    .padding(top = 4.dp)
                    .wrapContentHeight()
                    .animateContentSize(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                viewModel.availableOptions.forEach { option ->
                    if(!isMatrixMode.value || option == SingInServiceOption.MATRIX) {
                        Image(
                            modifier = Modifier
                                .height(34.dp)
                                .scalingClickable(
                                    onTap = {
                                        when(option) {
                                            SingInServiceOption.GOOGLE -> {
                                                isMatrixMode.value = false
                                                viewModel.requestGoogleSignIn()
                                            }
                                            SingInServiceOption.APPLE -> {
                                                isMatrixMode.value = false
                                                viewModel.requestAppleSignIn()
                                            }
                                            SingInServiceOption.MATRIX -> {
                                                if(homeServer.value.isBlank()) {
                                                    showHomeServerPicker.value = true
                                                }else isMatrixMode.value = !isMatrixMode.value
                                            }
                                        }
                                    }
                                ),
                            painter = painterResource(
                                when(option) {
                                    SingInServiceOption.GOOGLE -> LocalTheme.current.icons.googleSignUp
                                    SingInServiceOption.APPLE -> LocalTheme.current.icons.appleSignUp
                                    SingInServiceOption.MATRIX -> LocalTheme.current.icons.matrixSignUp
                                }
                            ),
                            contentDescription = null,
                            colorFilter = if(option == SingInServiceOption.MATRIX) {
                                ColorFilter.tint(
                                    if(isMatrixMode.value) LocalTheme.current.colors.primary
                                    else LocalTheme.current.colors.secondary
                                )
                            }else null
                        )
                    }
                }
            }
            AnimatedVisibility(isMatrixMode.value) {
                Text(
                    modifier = Modifier
                        .padding(start = 16.dp)
                        .scalingClickable(enabled = !isLoading.value) {
                            showHomeServerPicker.value = true
                        }
                        .padding(4.dp)
                        .animateContentSize(),
                    text = stringResource(
                        Res.string.login_matrix_homeserver,
                        homeServer.value
                    ),
                    style = LocalTheme.current.styles.regular
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MatrixProgressStage(
    stage: String,
    viewModel: LoginViewModel
) {
    val progress = viewModel.matrixProgress.collectAsState()

    Crossfade(targetState = stage) { currentStage ->
        when(currentStage) {
            Matrix.LOGIN_RECAPTCHA -> {
                val density = LocalDensity.current
                val isSuccess = remember { mutableStateOf(false) }

                val state = rememberWebViewState(
                    "http://augmy.org/recaptcha.html?site-key=${progress.value?.response?.params?.recaptcha?.publicKey}",
                )
                val jsBridge = rememberWebViewJsBridge()
                val navigator = rememberWebViewNavigator()

                LaunchedEffect(jsBridge) {
                    jsBridge.register(object: IJsMessageHandler {
                        override fun canHandle(methodName: String) = methodName() == methodName
                        override fun handle(
                            message: JsMessage,
                            navigator: WebViewNavigator?,
                            callback: (String) -> Unit
                        ) {
                            isSuccess.value = true
                            viewModel.matrixStepOver(
                                type = currentStage,
                                recaptchaJson = message.params
                            )
                        }
                        override fun methodName(): String = "recaptchaResult"
                    })
                }

                DisposableEffect(Unit) {
                    state.webSettings.apply {
                        androidWebSettings.domStorageEnabled = true
                        desktopWebSettings.disablePopupWindows = true
                        allowFileAccessFromFileURLs = true
                        allowUniversalAccessFromFileURLs = true
                        customUserAgentString = "use required / intended UA string"
                        isJavaScriptEnabled = true
                        supportZoom = false
                        logSeverity = KLogSeverity.Verbose
                    }
                    onDispose {  }
                }

                AlertDialog(
                    title = stringResource(Res.string.dialog_recaptcha_title),
                    icon = vectorResource(Res.drawable.ic_robot),
                    additionalContent = {
                        WebView(
                            modifier = Modifier
                                .wrapContentHeight()
                                .requiredWidth(with(density) { 340f.toDp() })
                                .heightIn(min = 500.dp),
                            navigator = navigator,
                            state = state,
                            webViewJsBridge = jsBridge
                        )
                    },
                    onDismissRequest = {
                        if(!isSuccess.value) viewModel.clearMatrixProgress()
                    }
                )
            }
            Matrix.LOGIN_TERMS -> {
                val policies = progress.value?.response?.params?.terms?.policies?.values?.toList()

                val isSuccess = remember { mutableStateOf(false) }
                AlertDialog(
                    title = stringResource(Res.string.dialog_terms_title),
                    icon = Icons.Outlined.Handshake,
                    message = buildAnnotatedLink(
                        stringResource(
                            Res.string.dialog_terms_message,
                            policies?.joinToString(", ") { it.en?.name ?: "" } ?: "?"
                        ),
                        linkTexts = policies.orEmpty().mapNotNull { it.en?.name },
                        onLinkClicked = { _, index ->
                            policies?.getOrNull(index)?.en?.url?.let { url ->
                                openLink(url)
                            }
                        }
                    ),
                    confirmButtonState = ButtonState(
                        text = stringResource(Res.string.button_confirm),
                        onClick = {
                            isSuccess.value = true
                            viewModel.matrixStepOver(
                                type = currentStage,
                                agreements = policies.orEmpty().mapNotNull { it.en?.url }
                            )
                        }
                    ),
                    onDismissRequest = {
                        if(!isSuccess.value) viewModel.clearMatrixProgress()
                    }
                )
            }
            Matrix.LOGIN_EMAIL_IDENTITY -> {
                val counter = remember { mutableStateOf(STOPWATCH_MAX) }
                val snackbarHostState = LocalSnackbarHost.current
                val coroutineScope = rememberCoroutineScope()

                LaunchedEffect(Unit, counter.value) {
                    if(counter.value >= STOPWATCH_MAX) {
                        coroutineScope.coroutineContext.cancelChildren()
                        coroutineScope.launch {
                            while(counter.value > 0) {
                                delay(100)
                                counter.value -= 100
                            }
                        }
                    }
                }
                LaunchedEffect(Unit) {
                    viewModel.matrixProgress.collectLatest {
                        if(it?.retryAfter != null) {
                            counter.value = it.retryAfter
                        }
                    }
                }
                LifecycleListener { event ->
                    if(event == Lifecycle.Event.ON_RESUME && progress.value?.sid != null) {
                        viewModel.matrixStepOver(type = currentStage)
                    }
                }
                LaunchedEffect(Unit) {
                    if(progress.value?.sid == null) {
                        viewModel.matrixRequestToken()
                    }
                }

                SimpleModalBottomSheet(
                    sheetState = rememberModalBottomSheetState(
                        skipPartiallyExpanded = true,
                        confirmValueChange = { false }
                    ),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    onDismissRequest = {}
                ) {
                    Text(
                        text = stringResource(Res.string.login_email_verification_heading),
                        style = LocalTheme.current.styles.subheading
                    )
                    Text(
                        text = buildAnnotatedString {
                            append(stringResource(Res.string.login_email_verification_message))
                            withStyle(
                                SpanStyle(
                                    fontWeight = FontWeight.Bold,
                                    fontStyle = LocalTheme.current.styles.title.fontStyle
                                )
                            ) { append(progress.value?.email) }
                            append(".")

                        },
                        style = LocalTheme.current.styles.regular
                    )
                    GifImage(
                        modifier = Modifier
                            .padding(top = 6.dp)
                            .zIndex(1f)
                            .clip(RoundedCornerShape(6.dp))
                            .wrapContentWidth()
                            .animateContentSize(alignment = Alignment.BottomCenter),
                        data = "https://media2.giphy.com/media/v1.Y2lkPTc5MGI3NjExZzVrZTdmbHhybnZ4MTJ5eXNkNTBza3RreWxoeTBtaDB6Yjl2Y2JzMSZlcD12MV9pbnRlcm5hbF9naWZfYnlfaWQmY3Q9Zw/gjxYeOTM4Zq1ODtTv3/giphy.gif",
                        contentDescription = null
                    )
                    ComponentHeaderButton(
                        modifier = Modifier
                            .padding(top = 32.dp)
                            .fillMaxWidth(.7f),
                        text = stringResource(Res.string.login_email_verification_action),
                        extraContent = {
                            Icon(
                                imageVector = Icons.Outlined.AlternateEmail,
                                contentDescription = null,
                                tint = LocalTheme.current.colors.secondary
                            )
                        },
                        onClick = {
                            if(!openEmail(address = progress.value?.email)) {
                                coroutineScope.launch {
                                    snackbarHostState?.showSnackbar(
                                        message = getString(Res.string.no_email_client_error)
                                    )
                                }
                            }
                        }
                    )
                    Row(
                        modifier = Modifier.animateContentSize(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            modifier = Modifier
                                .scalingClickable(enabled = counter.value <= 0) {
                                    counter.value = STOPWATCH_MAX
                                    viewModel.matrixRequestToken()
                                }
                                .padding(top = 2.dp),
                            text = stringResource(Res.string.login_email_verification_repeat),
                            style = LocalTheme.current.styles.regular
                        )
                        if(counter.value > 0) {
                            Text(
                                text = "${counter.value.div(1000)}s",
                                style = LocalTheme.current.styles.title
                            )
                        }
                    }
                }
            }
            else -> {}
        }
    }
}

private const val STOPWATCH_MAX = 60 * 1000
private const val PASSWORD_MAX_LENGTH = 64

/** Email address pattern, same as android.util.Patterns.EMAIL_ADDRESS */
private val emailAddressRegex = """[a-zA-Z0-9+._%-+]{1,256}@[a-zA-Z0-9][a-zA-Z0-9-]{0,64}(\.[a-zA-Z0-9][a-zA-Z0-9-]{0,25})+""".toRegex()

/** Matrix stripped username */
private val strippedUsernameRegex = """^[a-zA-Z0-9._=\-/]+${'$'}""".toRegex()

private const val PASSWORD_MIN_LENGTH = 8

/** Type of interface for login screen */
enum class LoginScreenType {
    SIGN_UP,
    SIGN_IN
}