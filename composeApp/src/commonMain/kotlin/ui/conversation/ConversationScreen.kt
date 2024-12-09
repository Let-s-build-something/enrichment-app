package ui.conversation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
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
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.paging.LoadState
import app.cash.paging.compose.collectAsLazyPagingItems
import augmy.composeapp.generated.resources.Res
import augmy.composeapp.generated.resources.action_settings
import augmy.interactive.shared.ui.base.LocalDeviceType
import augmy.interactive.shared.ui.components.navigation.ActionBarIcon
import augmy.interactive.shared.ui.theme.LocalTheme
import base.BrandBaseScreen
import base.navigation.NavIconType
import base.utils.getOrNull
import components.MessageBubble
import components.UserProfileImage
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
                    .fillMaxSize(),
                verticalArrangement = Arrangement.Bottom,
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
                        val isPreviousMessageSameAuthor = messages.getOrNull(index + 1)?.authorPublicId == data?.authorPublicId
                        val isNextMessageSameAuthor = messages.getOrNull(index - 1)?.authorPublicId == data?.authorPublicId

                        Row(
                            modifier = Modifier
                                .padding(start = if(isCurrentUser) 50.dp else 12.dp)
                                .fillMaxWidth()
                                .padding(
                                    top = if(isPreviousMessageSameAuthor) 1.dp else LocalTheme.current.shapes.betweenItemsSpace.div(2),
                                    bottom = if(isNextMessageSameAuthor) 1.dp else LocalTheme.current.shapes.betweenItemsSpace.div(2)
                                )
                                .animateItem(),
                            horizontalArrangement = if(isCurrentUser) Arrangement.End else Arrangement.Start,
                            verticalAlignment = if(isPreviousMessageSameAuthor) Alignment.Top else Alignment.CenterVertically
                        ) {
                            if(!isCurrentUser && !isNextMessageSameAuthor) {
                                UserProfileImage(
                                    modifier = Modifier.size(32.dp),
                                    model = data?.user?.photoUrl,
                                    tag = data?.user?.tag
                                )
                                Spacer(Modifier.width(LocalTheme.current.shapes.betweenItemsSpace))
                            }else if(isPreviousMessageSameAuthor || isNextMessageSameAuthor) {
                                Spacer(Modifier.width(
                                    LocalTheme.current.shapes.betweenItemsSpace + 32.dp
                                ))
                            }
                            MessageBubble(
                                data = data,
                                isCurrentUser = isCurrentUser,
                                hasPrevious = isPreviousMessageSameAuthor,
                                hasNext = isNextMessageSameAuthor
                            )
                        }
                    }
                }
            }
            SendMessagePanel(
                modifier = Modifier
                    .background(
                        color = LocalTheme.current.colors.backgroundDark,
                        shape = RoundedCornerShape(
                            topStart = LocalTheme.current.shapes.componentCornerRadius,
                            topEnd = LocalTheme.current.shapes.componentCornerRadius
                        )
                    )
                    .padding(WindowInsets.navigationBars.asPaddingValues())
                    .onSizeChanged {
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

/** Number of network items within one screen to be shimmered */
private const val MESSAGES_SHIMMER_ITEM_COUNT = 24
