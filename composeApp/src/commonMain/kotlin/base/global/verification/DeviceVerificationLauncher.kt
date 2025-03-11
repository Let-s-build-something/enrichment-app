package base.global.verification

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.TextObfuscationMode
import androidx.compose.foundation.text.input.delete
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Key
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SheetState
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import augmy.composeapp.generated.resources.Res
import augmy.composeapp.generated.resources.accessibility_hide_password
import augmy.composeapp.generated.resources.accessibility_show_password
import augmy.composeapp.generated.resources.device_verification_confirm
import augmy.composeapp.generated.resources.device_verification_match
import augmy.composeapp.generated.resources.device_verification_no_match
import augmy.composeapp.generated.resources.device_verification_other_device
import augmy.composeapp.generated.resources.device_verification_passphrase
import augmy.composeapp.generated.resources.device_verification_send
import augmy.composeapp.generated.resources.device_verification_title_decimal
import augmy.composeapp.generated.resources.device_verification_title_emojis
import augmy.composeapp.generated.resources.device_verification_title_other_device
import augmy.composeapp.generated.resources.device_verification_title_passphrase
import augmy.composeapp.generated.resources.login_password_password_hint
import augmy.interactive.com.BuildKonfig
import augmy.interactive.shared.ui.components.BrandHeaderButton
import augmy.interactive.shared.ui.components.MinimalisticIcon
import augmy.interactive.shared.ui.components.OutlinedButton
import augmy.interactive.shared.ui.components.SimpleModalBottomSheet
import augmy.interactive.shared.ui.components.input.CustomTextField
import augmy.interactive.shared.ui.theme.LocalTheme
import augmy.interactive.shared.ui.theme.SharedColors
import net.folivo.trixnity.client.verification.SelfVerificationMethod
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.context.loadKoinModules

// TODO should be full screen
/**
 * Bottom sheet for verifying current device
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeviceVerificationLauncher(
    modifier: Modifier = Modifier,
    sheetState: SheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true
    ) { value -> value != SheetValue.Hidden || BuildKonfig.BuildType == "development" }
) {
    loadKoinModules(verificationModule)
    val model: VerificationModel = koinViewModel()
    val verificationMethods = model.verificationMethods.collectAsState()
    val comparisonByUser = model.comparisonByUser.collectAsState()
    val isLoading = model.isLoading.collectAsState()

    if(verificationMethods.value.isEmpty()) return

    val passphraseMethod = remember(verificationMethods) {
        mutableStateOf(verificationMethods.value.none { it is SelfVerificationMethod.CrossSignedDeviceVerification })
    }
    val passphraseState = remember { TextFieldState() }
    val showPassphrase = remember { mutableStateOf(false) }

    SimpleModalBottomSheet(
        modifier = modifier,
        sheetState = sheetState,
        onDismissRequest = {  }
    ) {
        Text(
            modifier = Modifier.padding(bottom = 16.dp),
            text = stringResource(
                when {
                    comparisonByUser.value != null -> {
                        if(!comparisonByUser.value?.emojis.isNullOrEmpty()) {
                            Res.string.device_verification_title_emojis
                        }else Res.string.device_verification_title_decimal
                    }
                    passphraseMethod.value -> Res.string.device_verification_title_passphrase
                    else -> Res.string.device_verification_title_other_device
                }
            ),
            style = LocalTheme.current.styles.heading
        )

        AnimatedVisibility(comparisonByUser.value != null) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    comparisonByUser.value?.decimal?.forEach { decimal ->
                        Text(
                            modifier = Modifier
                                .padding(vertical = 4.dp)
                                .background(color = LocalTheme.current.colors.backgroundLight)
                                .padding(vertical = 6.dp, horizontal = 8.dp),
                            text = decimal.toString(),
                            style = LocalTheme.current.styles.subheading
                        )
                    }
                    comparisonByUser.value?.emojis?.forEach { emoji ->
                        EmojiEntity(
                            modifier = Modifier
                                .padding(vertical = 4.dp)
                                .background(color = LocalTheme.current.colors.backgroundLight)
                                .padding(vertical = 6.dp, horizontal = 8.dp),
                            emoji = emoji
                        )
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        text = stringResource(Res.string.device_verification_no_match),
                        onClick = {
                            model.matchChallenge(false)
                        },
                        activeColor = SharedColors.RED_ERROR_50
                    )
                    BrandHeaderButton(
                        text = stringResource(Res.string.device_verification_match),
                        onClick = {
                            model.matchChallenge(true)
                        }
                    )
                }
            }
        }

        AnimatedVisibility(passphraseMethod.value) {
            Column {
                CustomTextField(
                    modifier = Modifier
                        .padding(top = LocalTheme.current.shapes.betweenItemsSpace)
                        .fillMaxWidth(),
                    hint = stringResource(Res.string.login_password_password_hint),
                    prefixIcon = Icons.Outlined.Key,
                    state = passphraseState,
                    errorText = null,
                    textObfuscationMode = if(showPassphrase.value) {
                        TextObfuscationMode.Visible
                    }else TextObfuscationMode.Hidden,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        imeAction = ImeAction.Done
                    ),
                    onKeyboardAction = {
                        model.verify(passphraseState.text.toString().takeIf { passphraseMethod.value })
                    },
                    trailingIcon = {
                        Crossfade(
                            targetState = showPassphrase.value, label = "",
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
                                showPassphrase.value = !showPassphrase.value
                            }
                        }
                    },
                    inputTransformation = {
                        this.delete(PASSPHRASE_MAX_LENGTH, this.length)
                    },
                    paddingValues = PaddingValues(start = 16.dp)
                )
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedButton(
                modifier = Modifier.padding(horizontal = 4.dp),
                text = stringResource(
                    if(passphraseMethod.value) Res.string.device_verification_other_device
                    else Res.string.device_verification_passphrase
                ),
                onClick = {
                    passphraseMethod.value = !passphraseMethod.value
                },
                activeColor = LocalTheme.current.colors.secondary
            )
            BrandHeaderButton(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 16.dp, end = 16.dp),
                text = stringResource(
                    if(passphraseMethod.value) Res.string.device_verification_confirm
                    else Res.string.device_verification_send
                ),
                isEnabled = isLoading.value.not(),
                isLoading = isLoading.value,
                onClick = {
                    model.verify(passphraseState.text.toString().takeIf { passphraseMethod.value })
                }
            )
        }
    }
}

@Composable
private fun EmojiEntity(
    modifier: Modifier,
    emoji: Pair<Int, String>
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = emoji.second,
            style = LocalTheme.current.styles.subheading
        )
        Text(
            text = emoji.first.toString(),
            style = LocalTheme.current.styles.title
        )
    }
}

private const val PASSPHRASE_MAX_LENGTH = 256
