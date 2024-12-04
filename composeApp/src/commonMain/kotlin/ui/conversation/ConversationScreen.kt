package ui.conversation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material3.Icon
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.paging.LoadState
import app.cash.paging.compose.collectAsLazyPagingItems
import augmy.composeapp.generated.resources.Res
import augmy.composeapp.generated.resources.accessibility_cancel
import augmy.composeapp.generated.resources.accessibility_message_image
import augmy.composeapp.generated.resources.accessibility_message_more_options
import augmy.composeapp.generated.resources.account_picture_pick_title
import augmy.composeapp.generated.resources.action_settings
import augmy.interactive.shared.ui.base.LocalDeviceType
import augmy.interactive.shared.ui.components.DEFAULT_ANIMATION_LENGTH_LONG
import augmy.interactive.shared.ui.components.input.EditFieldInput
import augmy.interactive.shared.ui.components.navigation.ActionBarIcon
import augmy.interactive.shared.ui.theme.LocalTheme
import base.BrandBaseScreen
import base.getOrNull
import base.navigation.NavIconType
import base.theme.Colors
import components.MessageBubble
import components.UserProfileImage
import future_shared_module.ext.scalingClickable
import io.github.vinceglb.filekit.compose.rememberFilePickerLauncher
import io.github.vinceglb.filekit.core.PickerMode
import io.github.vinceglb.filekit.core.PickerType
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.context.loadKoinModules
import org.koin.core.parameter.parametersOf
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/** Screen displaying a conversation */
@OptIn(ExperimentalUuidApi::class)
@Composable
fun ConversationScreen(
    conversationId: String? = null,
    name: String? = null
) {
    loadKoinModules(conversationModule)
    val viewModel: ConversationViewModel = koinViewModel(
        parameters = { parametersOf(conversationId ?: "") }
    )

    val density = LocalDensity.current

    val messages = viewModel.conversationMessages.collectAsLazyPagingItems()
    val conversationDetail = viewModel.conversationDetail.collectAsState(initial = null)
    val isLoadingInitialPage = messages.loadState.refresh is LoadState.Loading

    val messagePanelHeight = rememberSaveable {
        mutableStateOf(100f)
    }

    BrandBaseScreen(
        navIconType = NavIconType.BACK,
        headerPrefix = {
            AnimatedVisibility(conversationDetail.value != null) {
                Row {
                    UserProfileImage(
                        modifier = Modifier
                            .align(Alignment.CenterVertically)
                            .size(32.dp),
                        model = conversationDetail.value?.pictureUrl,
                        tag = conversationDetail.value?.tag,
                        animate = true
                    )
                    Spacer(Modifier.width(LocalTheme.current.shapes.betweenItemsSpace))
                }
            }
        },
        actionIcons = { isExpanded ->
            ActionBarIcon(
                text = if(isExpanded && LocalDeviceType.current != WindowWidthSizeClass.Compact) {
                    stringResource(Res.string.action_settings)
                } else null,
                imageVector = Icons.Outlined.MoreVert,
                onClick = {
                    // TODO hamburger menu
                }
            )
        },
        title = name
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.BottomCenter
        ) {
            LazyColumn(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(LocalTheme.current.shapes.betweenItemsSpace),
                reverseLayout = true
            ) {
                item {
                    Spacer(
                        Modifier
                            .padding(WindowInsets.navigationBars.asPaddingValues())
                            .height(messagePanelHeight.value.dp)
                            .animateContentSize()
                    )
                }
                item {
                    AnimatedVisibility(
                        enter = expandVertically() + fadeIn(),
                        visible = messages.itemCount == 0 && !isLoadingInitialPage
                    ) {
                        // TODO empty layout
                    }
                }
                items(
                    count = if(messages.itemCount == 0 && isLoadingInitialPage) MESSAGES_SHIMMER_ITEM_COUNT else messages.itemCount,
                    key = { index -> messages.getOrNull(index)?.id ?: Uuid.random().toString() }
                ) { index ->
                    messages.getOrNull(index).let { data ->
                        val isCurrentUser = if(data != null) {
                            data.authorPublicId == viewModel.currentUser.value?.publicId
                        }else (0..1).random() == 0

                        Row(
                            modifier = Modifier
                                .padding(start = if(isCurrentUser) 50.dp else 12.dp)
                                .fillMaxWidth()
                                .animateItem(),
                            horizontalArrangement = if(isCurrentUser) Arrangement.End else Arrangement.Start,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if(!isCurrentUser) {
                                UserProfileImage(
                                    modifier = Modifier.size(48.dp),
                                    model = conversationDetail.value?.users?.find {
                                        it.publicId == data?.authorPublicId
                                    }?.photoUrl ?: conversationDetail.value?.pictureUrl,
                                    tag = data?.tag
                                )
                                Spacer(Modifier.width(LocalTheme.current.shapes.betweenItemsSpace))
                            }
                            MessageBubble(
                                data = data,
                                isCurrentUser = isCurrentUser
                            )
                        }
                    }
                }
            }
            SendMessagePanel(
                modifier = Modifier.onSizeChanged {
                    if(it.height != 0) {
                        with(density) {
                            messagePanelHeight.value = it.height.toDp().value
                        }
                    }
                },
                viewModel = viewModel
            )
        }
    }
}

@Composable
private fun SendMessagePanel(
    modifier: Modifier = Modifier,
    viewModel: ConversationViewModel
) {
    val coroutineScope = rememberCoroutineScope()
    val spacing = LocalTheme.current.shapes.betweenItemsSpace / 2
    val content = remember {
        mutableStateOf(viewModel.savedMessage)
    }
    val showMoreOptions = rememberSaveable {
        mutableStateOf(content.value.isNotBlank())
    }
    val showMoreIconRotation = animateFloatAsState(
        targetValue = if(showMoreOptions.value) 0f else 135f,
        label = "showMoreIconRotation",
        animationSpec = tween(durationMillis = DEFAULT_ANIMATION_LENGTH_LONG)
    )

    val launcher = rememberFilePickerLauncher(
        type = PickerType.Image,
        mode = PickerMode.Single,
        title = stringResource(Res.string.account_picture_pick_title)
    ) { file ->
        if(file != null) {
            coroutineScope.launch {
                viewModel.requestPictureUpload(
                    mediaByteArray = file.readBytes(),
                    fileName = file.name
                )
            }
        }
    }


    DisposableEffect(null) {
        onDispose {
            viewModel.savedMessage = content.value
        }
    }

    Row(
        modifier = modifier
            .padding(WindowInsets.navigationBars.asPaddingValues())
            .fillMaxWidth()
            //.background(color = LocalTheme.current.colors.backgroundLight)
            .padding(horizontal = 10.dp, vertical = 6.dp)
            .padding(bottom = 16.dp)
            .imePadding()
            .animateContentSize(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        EditFieldInput(
            modifier = Modifier
                .weight(1f)
                .padding(end = spacing),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Text,
                imeAction = ImeAction.Send
            ),
            keyboardActions = KeyboardActions(
                onSend = {
                    // TODO send message
                }
            ),
            value = viewModel.savedMessage,
            onValueChange = {
                content.value = it
                showMoreOptions.value = false
            }
        )

        Icon(
            modifier = Modifier
                .size(38.dp)
                .background(
                    color = LocalTheme.current.colors.brandMainDark,
                    shape = LocalTheme.current.shapes.circularActionShape
                )
                .padding(6.dp)
                .scalingClickable {
                    showMoreOptions.value = !showMoreOptions.value
                }
                .rotate(showMoreIconRotation.value),
            imageVector = Icons.Outlined.Close,
            contentDescription = stringResource(
                if(showMoreOptions.value) Res.string.accessibility_cancel
                else Res.string.accessibility_message_more_options
            ),
            tint = Colors.GrayLight
        )
        AnimatedVisibility(showMoreOptions.value) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Spacer(Modifier.width(spacing))
                Icon(
                    modifier = Modifier
                        .size(38.dp)
                        .background(
                            color = LocalTheme.current.colors.brandMainDark,
                            shape = LocalTheme.current.shapes.circularActionShape
                        )
                        .padding(6.dp)
                        .scalingClickable {
                            // TODO image selector
                        },
                    imageVector = Icons.Outlined.Image,
                    contentDescription = stringResource(Res.string.accessibility_message_image),
                    tint = Color.White
                )
                Spacer(Modifier.width(spacing))
                Icon(
                    modifier = Modifier
                        .size(38.dp)
                        .background(
                            color = LocalTheme.current.colors.brandMainDark,
                            shape = LocalTheme.current.shapes.circularActionShape
                        )
                        .padding(6.dp)
                        .scalingClickable {
                            // TODO in-app audio recorder
                        },
                    imageVector = Icons.Outlined.Mic,
                    contentDescription = stringResource(Res.string.accessibility_message_image),
                    tint = Color.White
                )
            }
        }
    }
}

/** Number of network items within one screen to be shimmered */
private const val MESSAGES_SHIMMER_ITEM_COUNT = 24
