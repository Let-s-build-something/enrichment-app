package ui.conversation.message

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Stop
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import augmy.composeapp.generated.resources.Res
import augmy.composeapp.generated.resources.accessibility_play
import augmy.interactive.shared.ext.scalingClickable
import augmy.interactive.shared.ext.verticallyDraggable
import augmy.interactive.shared.ui.theme.LocalTheme
import augmy.interactive.shared.ui.theme.SharedColors
import augmy.interactive.shared.utils.DateUtils.formatAsRelative
import base.BrandBaseScreen
import base.navigation.NavIconType
import base.theme.Colors
import base.theme.DefaultThemeStyles.Companion.fontQuicksandMedium
import base.utils.openLink
import components.UserProfileImage
import components.buildAnnotatedLinkString
import data.io.social.network.conversation.message.MessageState
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import ui.conversation.components.TempoText

/** Screen of a specific message and its subsequent reactions */
@Composable
fun MessageDetailScreen(
    messageId: String?,
    title: String?
) {
    val viewModel: MessageDetailViewModel = koinViewModel(
        key = messageId,
        parameters = { parametersOf(messageId ?: "") }
    )
    val listState = rememberLazyListState()

    val message = viewModel.message.collectAsState()
    val isCurrentUser = viewModel.currentUser.value?.publicId == message.value?.authorPublicId
    val transcribing = rememberSaveable(messageId) {
        mutableStateOf(false)
    }

    BrandBaseScreen(
        navIconType = NavIconType.CLOSE,
        title = title,
        subtitle = message.value?.sentAt?.formatAsRelative()
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .verticallyDraggable(listState),
            horizontalAlignment = Alignment.CenterHorizontally,
            state = listState
        ) {
            item {
                Column(
                    modifier = Modifier
                        .padding(horizontal = 12.dp)
                ) {
                    Spacer(Modifier.height(LocalTheme.current.shapes.betweenItemsSpace))
                    if(!isCurrentUser) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row {
                                UserProfileImage(
                                    modifier = Modifier
                                        .padding(top = 8.dp)
                                        .size(48.dp),
                                    model = message.value?.user?.photoUrl,
                                    tag = message.value?.user?.tag,
                                    animate = true
                                )
                                Spacer(Modifier.width(LocalTheme.current.shapes.betweenItemsSpace))
                                Text(
                                    text = message.value?.user?.name ?: "",
                                    style = LocalTheme.current.styles.title
                                )
                            }
                            if(!message.value?.timings.isNullOrEmpty()) {
                                Crossfade(
                                    modifier = Modifier.padding(start = 8.dp),
                                    targetState = transcribing.value
                                ) { isTranscribing ->
                                    Icon(
                                        modifier = Modifier
                                            .scalingClickable {
                                                transcribing.value = !transcribing.value
                                            }
                                            .size(42.dp)
                                            .padding(4.dp),
                                        imageVector = if(isTranscribing) {
                                            Icons.Outlined.Stop
                                        }else Icons.Outlined.PlayArrow,
                                        tint = LocalTheme.current.colors.secondary,
                                        contentDescription = stringResource(Res.string.accessibility_play)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(LocalTheme.current.shapes.betweenItemsSpace))
                    SelectionContainer {
                        TempoText(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 4.dp),
                            enabled = transcribing.value,
                            text = buildAnnotatedLinkString(
                                text = message.value?.content ?: "",
                                onLinkClicked = { openLink(it) }
                            ),
                            style = LocalTheme.current.styles.title.copy(
                                color = (if (isCurrentUser) Colors.GrayLight else LocalTheme.current.colors.secondary),
                                fontFamily = FontFamily(fontQuicksandMedium)
                            ),
                            timings = message.value?.timings.orEmpty(),
                            onFinish = {
                                transcribing.value = false
                            }
                        )
                    }
                }
            }
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = if(isCurrentUser) Arrangement.End else Arrangement.Start
                ) {
                    message.value?.state?.imageVector?.let { imgVector ->
                        Icon(
                            modifier = Modifier.size(16.dp),
                            imageVector = imgVector,
                            contentDescription = message.value?.state?.description,
                            tint = if (message.value?.state == MessageState.Failed) {
                                SharedColors.RED_ERROR
                            } else LocalTheme.current.colors.disabled
                        )
                    } ?: CircularProgressIndicator(
                        modifier = Modifier.requiredSize(12.dp),
                        color = LocalTheme.current.colors.disabled,
                        trackColor = LocalTheme.current.colors.disabledComponent,
                        strokeWidth = 2.dp
                    )
                    Text(
                        modifier = Modifier.padding(start = 6.dp),
                        text = (message.value?.state?.description?.plus(", ") ?: "") +
                                " ${message.value?.sentAt?.formatAsRelative() ?: ""}",
                        style = LocalTheme.current.styles.regular
                    )
                }
            }
            item {
                Spacer(Modifier.height(300.dp))
            }
        }
    }
}