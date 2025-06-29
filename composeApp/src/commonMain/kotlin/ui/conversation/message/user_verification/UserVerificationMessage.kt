package ui.conversation.message.user_verification


import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import augmy.composeapp.generated.resources.Res
import augmy.composeapp.generated.resources.accessibility_cancel
import augmy.composeapp.generated.resources.button_accept
import augmy.composeapp.generated.resources.device_verification_match
import augmy.composeapp.generated.resources.device_verification_no_match
import augmy.composeapp.generated.resources.device_verification_success
import augmy.composeapp.generated.resources.message_user_verification
import augmy.composeapp.generated.resources.message_user_verification_awaiting
import augmy.composeapp.generated.resources.message_user_verification_canceled
import augmy.composeapp.generated.resources.message_user_verification_digit
import augmy.composeapp.generated.resources.message_user_verification_emoji
import augmy.composeapp.generated.resources.message_user_verification_ready
import augmy.composeapp.generated.resources.message_user_verification_self
import augmy.composeapp.generated.resources.message_user_verification_start
import augmy.interactive.shared.ext.ifNull
import augmy.interactive.shared.ui.components.BrandHeaderButton
import augmy.interactive.shared.ui.components.OutlinedButton
import augmy.interactive.shared.ui.theme.LocalTheme
import augmy.interactive.shared.ui.theme.SharedColors
import base.global.verification.EmojiEntity
import components.AvatarImage
import data.io.social.network.conversation.message.FullConversationMessage
import data.io.social.network.conversation.message.MediaIO
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.context.loadKoinModules
import org.koin.core.parameter.parametersOf

@Composable
fun UserVerificationMessage(
    modifier: Modifier = Modifier,
    data: FullConversationMessage
) {
    // each verification message has its own Model
    loadKoinModules(userVerificationModule)
    val model: UserVerificationModel = koinViewModel(
        key = data.id,
        parameters = {
            parametersOf(data.message.conversationId ?: "")
        }
    )

    val verificationState = model.verificationState.collectAsState()
    val isLoading = model.isLoading.collectAsState()
    val isMyRequest = data.message.authorPublicId == model.matrixUserId

    LaunchedEffect(data.id) {
        model.getUserVerification(eventId = data.id)
    }

    AnimatedVisibility(verificationState.value !is VerificationState.Hidden) {
        Box(
            modifier = modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            Row(
                modifier = Modifier
                    .padding(vertical = 4.dp)
                    .fillMaxWidth(.8f)
                    .background(
                        color = if(verificationState.value.isFinished) LocalTheme.current.colors.disabledComponent
                        else LocalTheme.current.colors.backgroundDark,
                        shape = LocalTheme.current.shapes.rectangularActionShape
                    )
                    .padding(vertical = 24.dp, horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if(isMyRequest && data.author != null) {
                    AvatarImage(
                        modifier = Modifier
                            .align(Alignment.Top)
                            .size(48.dp),
                        media = MediaIO(url = data.author.avatarUrl),
                        tag = null,
                        name = data.author.displayName ?: ""
                    )
                }
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .animateContentSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if(isMyRequest) {
                        Text(
                            text = stringResource(Res.string.message_user_verification_self),
                            style = LocalTheme.current.styles.category.copy(
                                color = LocalTheme.current.colors.disabled
                            )
                        )
                    }else {
                        Text(
                            text = stringResource(
                                Res.string.message_user_verification,
                                data.author?.displayName ?: ""
                            ),
                            style = LocalTheme.current.styles.category.copy(
                                color = LocalTheme.current.colors.secondary
                            )
                        )
                    }
                    Crossfade(
                        modifier = Modifier.animateContentSize(),
                        targetState = verificationState.value
                    ) { state ->
                        when(state) {
                            is VerificationState.TheirRequest -> {
                                BrandHeaderButton(
                                    text = stringResource(Res.string.button_accept),
                                    contentPadding = PaddingValues(
                                        vertical = 8.dp,
                                        horizontal = 16.dp
                                    ),
                                    isLoading = isLoading.value
                                ) {
                                    model.confirmUserVerification()
                                }
                            }
                            is VerificationState.ComparisonByUser -> {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        modifier = Modifier.padding(bottom = 8.dp),
                                        text = stringResource(
                                            if (state.data.emojis.isNotEmpty()) Res.string.message_user_verification_emoji
                                            else Res.string.message_user_verification_digit
                                        ),
                                        style = LocalTheme.current.styles.regular
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
                                        state.data.emojis.takeIf { it.isNotEmpty() }?.chunked(4)?.forEach { chunks ->
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
                                                state.data.decimals.forEach { decimal ->
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
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        AnimatedVisibility(!isLoading.value) {
                                            OutlinedButton(
                                                text = stringResource(Res.string.device_verification_no_match),
                                                onClick = {
                                                    model.matchChallenge(false)
                                                },
                                                activeColor = SharedColors.RED_ERROR_50
                                            )
                                        }
                                        BrandHeaderButton(
                                            modifier = Modifier.weight(1f),
                                            isLoading = isLoading.value,
                                            text = stringResource(
                                                if(isLoading.value) Res.string.accessibility_cancel
                                                else Res.string.device_verification_match
                                            ),
                                            onClick = {
                                                if(isLoading.value) model.cancel()
                                                else model.matchChallenge(true)
                                            }
                                        )
                                    }
                                }
                            }
                            else -> {
                                Text(
                                    text = stringResource(
                                        when(state) {
                                            is VerificationState.Success -> Res.string.device_verification_success
                                            is VerificationState.Ready -> Res.string.message_user_verification_ready
                                            is VerificationState.Start -> Res.string.message_user_verification_start
                                            is VerificationState.Canceled -> Res.string.message_user_verification_canceled
                                            else -> Res.string.message_user_verification_awaiting
                                        }
                                    ),
                                    style = LocalTheme.current.styles.regular
                                )
                            }
                        }
                    }
                }

                AnimatedVisibility(
                    verificationState.value !is VerificationState.ComparisonByUser && isLoading.value
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.requiredSize(24.dp),
                        color = LocalTheme.current.colors.disabled,
                        trackColor = LocalTheme.current.colors.disabledComponent
                    )
                }
            }
        }
    }
}
