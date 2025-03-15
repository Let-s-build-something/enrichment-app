package base.global.verification

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.InputTransformation
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.TextObfuscationMode
import androidx.compose.foundation.text.input.byValue
import androidx.compose.foundation.text.input.maxLength
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Pin
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SheetState
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.unit.dp
import augmy.composeapp.generated.resources.Res
import augmy.composeapp.generated.resources.accessibility_cancel
import augmy.composeapp.generated.resources.accessibility_hide_password
import augmy.composeapp.generated.resources.accessibility_show_password
import augmy.composeapp.generated.resources.device_verification_confirm
import augmy.composeapp.generated.resources.device_verification_match
import augmy.composeapp.generated.resources.device_verification_no_match
import augmy.composeapp.generated.resources.device_verification_other_device
import augmy.composeapp.generated.resources.device_verification_passphrase
import augmy.composeapp.generated.resources.device_verification_passphrase_error
import augmy.composeapp.generated.resources.device_verification_passphrase_hint
import augmy.composeapp.generated.resources.device_verification_send
import augmy.composeapp.generated.resources.device_verification_send_info
import augmy.composeapp.generated.resources.device_verification_send_info_2
import augmy.composeapp.generated.resources.device_verification_title
import augmy.composeapp.generated.resources.device_verification_title_decimal
import augmy.composeapp.generated.resources.device_verification_title_emojis
import augmy.interactive.com.BuildKonfig
import augmy.interactive.shared.ext.ifNull
import augmy.interactive.shared.ui.components.BrandHeaderButton
import augmy.interactive.shared.ui.components.MinimalisticIcon
import augmy.interactive.shared.ui.components.MultiChoiceSwitch
import augmy.interactive.shared.ui.components.OutlinedButton
import augmy.interactive.shared.ui.components.SimpleModalBottomSheet
import augmy.interactive.shared.ui.components.input.CustomTextField
import augmy.interactive.shared.ui.components.rememberMultiChoiceState
import augmy.interactive.shared.ui.theme.LocalTheme
import augmy.interactive.shared.ui.theme.SharedColors
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.context.loadKoinModules

sealed class VerificationState {
    data object ComparisonByUser: VerificationState()
    data object OtherDevice: VerificationState()
    data object Passphrase: VerificationState()
}

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
    val showLauncher = model.showLauncher.collectAsState()
    val comparisonByUser = model.comparisonByUser.collectAsState()
    val isLoading = model.isLoading.collectAsState()
    val verificationResult = model.verificationResult.collectAsState()

    val multiChoiceState = rememberMultiChoiceState(
        items = mutableListOf(
            stringResource(Res.string.device_verification_other_device),
            stringResource(Res.string.device_verification_passphrase)
        )
    )
    val verificationState = remember {
        derivedStateOf {
            when {
                comparisonByUser.value != null -> VerificationState.ComparisonByUser
                multiChoiceState.selectedTabIndex.value == 0 -> VerificationState.OtherDevice
                else -> VerificationState.Passphrase
            }
        }
    }

    if(!showLauncher.value) return

    val passphraseState = remember { TextFieldState() }
    val showPassphrase = remember { mutableStateOf(false) }

    val verify = {
        model.verify(
            state = verificationState.value,
            passphrase = passphraseState.text.toString()
        )
    }

    SimpleModalBottomSheet(
        modifier = modifier,
        sheetState = sheetState,
        contentPadding = PaddingValues(
            start = 16.dp, end = 16.dp, bottom = 12.dp
        ),
        onDismissRequest = {}
    ) {
        Text(
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .padding(bottom = 24.dp),
            text = stringResource(Res.string.device_verification_title),
            style = LocalTheme.current.styles.heading
        )

        AnimatedVisibility(
            comparisonByUser.value == null && !isLoading.value
        ) {
            MultiChoiceSwitch(
                shape = LocalTheme.current.shapes.rectangularActionShape,
                state = multiChoiceState
            )
        }

        AnimatedVisibility(comparisonByUser.value != null) {
            Column(
                modifier = Modifier
                    .padding(bottom = 24.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .padding(top = 32.dp),
                    text = stringResource(
                        if(!comparisonByUser.value?.emojis.isNullOrEmpty()) {
                            Res.string.device_verification_title_emojis
                        }else Res.string.device_verification_title_decimal
                    ),
                    style = LocalTheme.current.styles.subheading
                )

                with(Modifier
                    .padding(4.dp)
                    .border(
                        width = 1.dp,
                        color = LocalTheme.current.colors.backgroundLight,
                        shape = LocalTheme.current.shapes.rectangularActionShape
                    )
                    .padding(vertical = 8.dp, horizontal = 10.dp)
                ) {
                    comparisonByUser.value?.emojis?.takeIf { it.isNotEmpty() }?.chunked(4)?.forEach { chunks ->
                        Row(horizontalArrangement = Arrangement.SpaceBetween) {
                            chunks.forEach { emoji ->
                                EmojiEntity(
                                    modifier = this@with,
                                    emoji = emoji
                                )
                            }
                        }
                    }?.ifNull {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            comparisonByUser.value?.decimals?.forEach { decimal ->
                                Text(
                                    modifier = this@with,
                                    text = decimal.toString(),
                                    style = LocalTheme.current.styles.subheading
                                )
                            }
                        }
                    }
                }
                Row(
                    modifier = Modifier
                        .padding(vertical = 12.dp, horizontal = 16.dp)
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        modifier = Modifier.padding(end = 12.dp),
                        text = stringResource(Res.string.device_verification_no_match),
                        onClick = {
                            model.matchChallenge(false)
                        },
                        activeColor = SharedColors.RED_ERROR_50
                    )
                    BrandHeaderButton(
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 4.dp),
                        text = stringResource(Res.string.device_verification_match),
                        onClick = {
                            model.matchChallenge(true)
                        }
                    )
                }
            }
        }

        AnimatedVisibility(
            modifier = Modifier.align(Alignment.CenterHorizontally),
            visible = verificationState.value == VerificationState.Passphrase
                    && comparisonByUser.value == null
        ) {
            CustomTextField(
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(vertical = LocalTheme.current.shapes.betweenItemsSpace)
                    .fillMaxWidth(.7f),
                hint = stringResource(Res.string.device_verification_passphrase_hint),
                prefixIcon = Icons.Outlined.Pin,
                state = passphraseState,
                errorText = if(verificationResult.value?.isFailure == true) {
                    stringResource(Res.string.device_verification_passphrase_error)
                }else null,
                textObfuscationMode = if(showPassphrase.value) {
                    TextObfuscationMode.Visible
                }else TextObfuscationMode.RevealLastTyped,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.NumberPassword,
                    imeAction = ImeAction.Send
                ),
                onKeyboardAction = { verify() },
                trailingIcon = {
                    Crossfade(
                        targetState = showPassphrase.value
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
                inputTransformation = InputTransformation.maxLength(PASSPHRASE_MAX_LENGTH).byValue { _, proposed ->
                    proposed.replace(Regex("[^0-9]"), "")
                },
                paddingValues = PaddingValues(start = 16.dp)
            )
        }

        AnimatedVisibility(verificationState.value !is VerificationState.ComparisonByUser) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                BrandHeaderButton(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    text = stringResource(
                        when {
                            isLoading.value -> Res.string.accessibility_cancel
                            verificationState.value is VerificationState.Passphrase -> Res.string.device_verification_confirm
                            else -> Res.string.device_verification_send
                        }
                    ),
                    isLoading = isLoading.value,
                    onClick = {
                        if(isLoading.value) {
                            model.cancel()
                        }else verify()
                    }
                )
                Text(
                    modifier = Modifier
                        .padding(start = 16.dp, end = 16.dp, top = 2.dp)
                        .animateContentSize(),
                    text = stringResource(
                        if(isLoading.value) {
                            Res.string.device_verification_send_info_2
                        }else Res.string.device_verification_send_info
                    ),
                    style = LocalTheme.current.styles.regular
                )
            }
        }
    }
}

@Composable
private fun EmojiEntity(
    modifier: Modifier,
    emoji: Pair<String, Map<String, String>>
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = emoji.first,
            style = LocalTheme.current.styles.heading
        )
        Text(
            text = emoji.second[Locale.current.language.lowercase()] ?: emoji.second.values.first(),
            style = LocalTheme.current.styles.title
        )
    }
}

private const val PASSPHRASE_MAX_LENGTH = 8
