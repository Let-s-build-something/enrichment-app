package lets.build.chatenrichment.ui.login.password

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.squadris.squadris.compose.base.LocalNavController
import com.squadris.squadris.compose.components.MinimalisticIcon
import com.squadris.squadris.compose.components.navigation.NavIconType
import com.squadris.squadris.compose.theme.LocalTheme
import lets.build.chatenrichment.R
import lets.build.chatenrichment.navigation.NavigationTree
import lets.build.chatenrichment.ui.base.BrandBaseScreen
import lets.build.chatenrichment.ui.components.BrandHeaderButton
import lets.build.chatenrichment.ui.components.EditFieldInput

data class PasswordValidation(
    val isValid: Boolean,
    val message: String,
    val isRequired: Boolean = false
)

private const val PASSWORD_MIN_LENGTH = 12
private const val PASSWORD_MAX_LENGTH = 64

/** Login via email and password */
@Composable
fun LoginPasswordScreen(
    viewModel: LoginPasswordViewModel? = hiltViewModel()
) {
    val navController = LocalNavController.current
    val context = LocalContext.current

    val currentUser = viewModel?.currentUser?.collectAsState()

    val email = remember { mutableStateOf("") }
    val password = remember { mutableStateOf("") }
    val passwordVisible = remember { mutableStateOf(false) }
    val validations = remember {
        derivedStateOf {
            listOf(
                PasswordValidation(
                    isValid = password.value.length >= PASSWORD_MIN_LENGTH,
                    message = context.getString(R.string.login_password_password_condition_0)
                ),
                PasswordValidation(
                    isValid = password.value.length >= PASSWORD_MIN_LENGTH,
                    message = context.getString(R.string.login_password_password_condition_0)
                )
            )
        }
    }

    LaunchedEffect(currentUser?.value) {
        if(currentUser?.value != null) {
            navController?.popBackStack(NavigationTree.LoginPassword, inclusive = true)
        }
    }

    BrandBaseScreen(
        title = stringResource(R.string.screen_login),
        navIconType = NavIconType.BACK
    ) {
        Column(
            modifier = Modifier
                .padding(
                    top = 24.dp,
                    start = 16.dp,
                    end = 16.dp
                ),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalAlignment = Alignment.End
        ) {
            val isEmailValid = remember {
                derivedStateOf {
                    android.util.Patterns.EMAIL_ADDRESS.matcher(email.value).matches()
                }
            }

            EditFieldInput(
                modifier = Modifier.fillMaxWidth(),
                hint = stringResource(R.string.login_password_email_hint),
                value = "",
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                onValueChange = { value ->
                    email.value = value
                },
                errorText = if(isEmailValid.value) null else stringResource(R.string.login_password_email_error),
                paddingValues = PaddingValues(start = 16.dp)
            )
            EditFieldInput(
                modifier = Modifier.fillMaxWidth(),
                hint = stringResource(R.string.login_password_password_hint),
                value = "",
                visualTransformation = if (passwordVisible.value) {
                    VisualTransformation.None
                } else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
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
            BrandHeaderButton(
                modifier = Modifier.padding(top = 16.dp),
                text = stringResource(R.string.login_password_confirm),
                onClick = {
                    viewModel?.signUpWithPassword(
                        email = email.value,
                        password = password.value
                    )
                }
            )
        }
    }
}