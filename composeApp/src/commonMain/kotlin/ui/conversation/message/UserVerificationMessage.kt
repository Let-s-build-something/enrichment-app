package ui.conversation.message


import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import augmy.composeapp.generated.resources.Res
import augmy.composeapp.generated.resources.button_verify
import augmy.composeapp.generated.resources.message_user_verification
import augmy.composeapp.generated.resources.message_user_verification_error
import augmy.interactive.shared.ext.ifNull
import augmy.interactive.shared.ui.components.BrandHeaderButton
import augmy.interactive.shared.ui.theme.LocalTheme
import base.global.verification.EmojiEntity
import data.io.base.BaseResponse
import data.io.social.network.conversation.message.ConversationMessageIO
import data.io.social.network.conversation.message.MediaIO
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.context.loadKoinModules
import org.koin.core.parameter.parametersOf
import ui.conversation.components.MediaElement
import ui.conversation.settings.ConversationSettingsModel
import ui.conversation.settings.conversationSettingsModule

@Composable
fun UserVerificationMessage(
    modifier: Modifier = Modifier,
    data: ConversationMessageIO
) {
    // each verification message has its own Model
    loadKoinModules(conversationSettingsModule)
    val model: ConversationSettingsModel = koinViewModel(
        key = data.id,
        parameters = {
            parametersOf(data.conversationId ?: "")
        }
    )

    val ongoingChange = model.ongoingChange.collectAsState()
    val isLoading = ongoingChange.value?.state is BaseResponse.Loading
    val isError = ongoingChange.value?.state is BaseResponse.Error
    val comparisonData = remember {
        derivedStateOf {
            (ongoingChange.value as? ConversationSettingsModel.ChangeType.VerifyMember)?.data
        }
    }

    // TODO separation between my request and their request
    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier
                .padding(vertical = 4.dp)
                .fillMaxWidth(.8f)
                .background(
                    color = if(isError) LocalTheme.current.colors.disabledComponent
                    else LocalTheme.current.colors.backgroundDark,
                    shape = LocalTheme.current.shapes.rectangularActionShape
                )
                .padding(vertical = 7.dp, horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            MediaElement(
                modifier = Modifier
                    .align(Alignment.Top)
                    .size(32.dp),
                media = MediaIO(url = data.user?.content?.avatarUrl)
            )
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = if(isError) {
                        stringResource(Res.string.message_user_verification_error)
                    }else stringResource(
                        Res.string.message_user_verification,
                        data.user?.displayName ?: ""
                    ),
                    style = LocalTheme.current.styles.category.copy(
                        color = if(isError) LocalTheme.current.colors.disabled
                        else LocalTheme.current.colors.secondary
                    )
                )
                AnimatedVisibility(comparisonData.value != null) {
                    Column {
                        with(Modifier
                            .padding(4.dp)
                            .border(
                                width = 1.dp,
                                color = LocalTheme.current.colors.backgroundLight,
                                shape = LocalTheme.current.shapes.rectangularActionShape
                            )
                            .padding(vertical = 8.dp, horizontal = 10.dp)
                        ) {
                            comparisonData.value?.emojis.takeIf { !it.isNullOrEmpty() }?.chunked(4)?.forEach { chunks ->
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
                                    comparisonData.value?.decimals?.forEach { decimal ->
                                        Text(
                                            modifier = this@with,
                                            text = decimal.toString(),
                                            style = LocalTheme.current.styles.subheading
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
            AnimatedVisibility(!isError) {
                BrandHeaderButton(
                    text = stringResource(Res.string.button_verify),
                    contentPadding = PaddingValues(
                        vertical = 16.dp,
                        horizontal = 24.dp
                    ),
                    isLoading = isLoading
                ) {
                    model.confirmUserVerification(data.id)
                }
            }
        }
    }
}
