package ui.conversation.search

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.GraphicEq
import androidx.compose.material.icons.outlined.Photo
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Videocam
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import app.cash.paging.compose.collectAsLazyPagingItems
import augmy.composeapp.generated.resources.Res
import augmy.composeapp.generated.resources.accessibility_more_options
import augmy.composeapp.generated.resources.button_search
import augmy.composeapp.generated.resources.message_media_file
import augmy.composeapp.generated.resources.screen_conversation_search
import augmy.interactive.shared.ext.scalingClickable
import augmy.interactive.shared.ui.base.LocalDeviceType
import augmy.interactive.shared.ui.base.LocalNavController
import augmy.interactive.shared.ui.components.input.CustomTextField
import augmy.interactive.shared.ui.theme.LocalTheme
import augmy.interactive.shared.utils.DateUtils.formatAsRelative
import base.BrandBaseScreen
import base.navigation.NavigationArguments
import base.navigation.NavigationNode
import base.utils.MediaType
import base.utils.extractSnippetAroundHighlight
import base.utils.getOrNull
import components.network.NetworkItemRow
import data.io.user.NetworkItemIO
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.context.loadKoinModules
import org.koin.core.parameter.parametersOf
import ui.network.components.user_detail.UserDetailDialog


@Composable
fun ConversationSearchScreen(
    conversationId: String?,
    searchQuery: String?
) {
    val navController = LocalNavController.current
    val isCompact = LocalDeviceType.current == WindowWidthSizeClass.Compact
    loadKoinModules(conversationSearchModule)
    val model: ConversationSearchModel = koinViewModel(
        key = conversationId,
        parameters = {
            parametersOf(conversationId)
        }
    )

    val messages = model.messages.collectAsLazyPagingItems()

    val searchFieldState = remember { TextFieldState(initialText = searchQuery ?: "") }
    val selectedUser = remember { mutableStateOf<NetworkItemIO?>(null) }

    val highlight = searchFieldState.text.toString().lowercase()


    selectedUser.value?.let { user ->
        UserDetailDialog(
            networkItem = user,
            userId = user.userId,
            onDismissRequest = {
                selectedUser.value = null
            }
        )
    }

    BrandBaseScreen(
        title = stringResource(Res.string.screen_conversation_search)
    ) {
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            stickyHeader(key = "searchHeader") {
                SearchBar(model, searchFieldState)
            }
            items(
                count = messages.itemCount,
                key = { index -> messages.getOrNull(index)?.id ?: index }
            ) { index ->
                val message = messages.getOrNull(index)

                NetworkItemRow(
                    modifier = Modifier
                        .padding(horizontal = 16.dp)
                        .animateItem()
                        .scalingClickable(scaleInto = .95f) {
                            if (isCompact) {
                                if (navController?.previousBackStackEntry?.destination?.route == NavigationNode.Conversation().route) {
                                    navController?.popBackStack()
                                } else {
                                    navController?.navigate(
                                        NavigationNode.Conversation(
                                            conversationId = conversationId,
                                            scrollTo = message?.id
                                        )
                                    )
                                }
                            } else navController?.currentBackStackEntry?.savedStateHandle?.set(
                                key = NavigationArguments.CONVERSATION_SCROLL_TO,
                                value = message?.id
                            )
                        },
                    highlightTitle = false,
                    onAvatarClick = {
                        message?.author?.let { selectedUser.value = it.toNetworkItem() }
                    },
                    data = NetworkItemIO(
                        userId = message?.author?.userId,
                        displayName = message?.author?.displayName,
                        avatarUrl = message?.author?.avatarUrl,
                        lastMessage = if (highlight.isNotBlank()) {
                            extractSnippetAroundHighlight(message?.data?.content, highlight)
                        }else message?.data?.content
                    ),
                    highlight = highlight
                ) {
                    Row(modifier = Modifier.padding(start = 6.dp)) {
                        Column(
                            horizontalAlignment = Alignment.End,
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = message?.data?.sentAt.formatAsRelative(),
                                style = LocalTheme.current.styles.regular
                            )
                            message?.data?.media?.firstOrNull()?.let { media ->
                                Text(
                                    modifier = Modifier
                                        .background(
                                            color = LocalTheme.current.colors.backgroundDark,
                                            shape = LocalTheme.current.shapes.circularActionShape
                                        )
                                        .padding(vertical = 4.dp, horizontal = 6.dp),
                                    text = media.mimetype ?: stringResource(Res.string.message_media_file)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchBar(
    model: ConversationSearchModel,
    searchFieldState: TextFieldState
) {
    val focusRequester = remember { FocusRequester() }
    val isExpanded = remember { mutableStateOf(false) }
    val availableMediaTypes = remember {
        listOf(MediaType.AUDIO, MediaType.IMAGE, MediaType.VIDEO)
    }
    val selectedMediaTypes = model.selectedMediaTypes.collectAsState()


    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    LaunchedEffect(searchFieldState.text) {
        isExpanded.value = false
        model.querySearch(searchFieldState.text)
    }

    Column {
        Row(
            modifier = Modifier
                .animateContentSize()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            CustomTextField(
                modifier = Modifier
                    .background(
                        LocalTheme.current.colors.backgroundDark,
                        shape = LocalTheme.current.shapes.rectangularActionShape
                    )
                    .padding(horizontal = 4.dp, vertical = 2.dp)
                    .weight(1f),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Text,
                    imeAction = ImeAction.Search
                ),
                prefixIcon = Icons.Outlined.Search,
                isClearable = true,
                focusRequester = focusRequester,
                hint = stringResource(Res.string.button_search),
                state = searchFieldState,
                showBorders = false,
                lineLimits = TextFieldLineLimits.SingleLine,
                shape = LocalTheme.current.shapes.rectangularActionShape
            )

            Row(
                modifier = Modifier
                    .animateContentSize()
                    .background(
                        color = LocalTheme.current.colors.backgroundDark,
                        shape = LocalTheme.current.shapes.rectangularActionShape
                    )
                    .padding(horizontal = 6.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                val rotation: Float by animateFloatAsState(
                    targetValue = if (isExpanded.value) 225f else 0f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessLow
                    )
                )

                Icon(
                    modifier = Modifier
                        .size(32.dp)
                        .rotate(rotation)
                        .scalingClickable {
                            isExpanded.value = !isExpanded.value
                        }
                        .padding(2.dp),
                    imageVector = Icons.Outlined.Add,
                    contentDescription = stringResource(Res.string.accessibility_more_options),
                    tint = LocalTheme.current.colors.secondary
                )

                AnimatedVisibility(isExpanded.value) {
                    Row {
                        availableMediaTypes.minus(selectedMediaTypes.value).forEach { mediaType ->
                            Icon(
                                modifier = Modifier
                                    .size(32.dp)
                                    .scalingClickable {
                                        isExpanded.value = false
                                        model.selectMediaType(mediaType)
                                    }
                                    .padding(2.dp),
                                imageVector = when (mediaType) {
                                    MediaType.AUDIO -> Icons.Outlined.GraphicEq
                                    MediaType.VIDEO -> Icons.Outlined.Videocam
                                    else -> Icons.Outlined.Photo
                                },
                                contentDescription = mediaType.name,
                                tint = LocalTheme.current.colors.secondary
                            )
                        }
                    }
                }
            }
        }

        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                Spacer(Modifier.width(16.dp))
            }
            items(
                items = selectedMediaTypes.value,
                key = { it.name }
            ) { mediaType ->
                Row(
                    modifier = Modifier
                        .scalingClickable {
                            isExpanded.value = false
                            model.selectMediaType(mediaType)
                        }
                        .background(
                            color = LocalTheme.current.colors.brandMain,
                            shape = LocalTheme.current.shapes.circularActionShape
                        )
                        .padding(start = 8.dp, top = 4.dp, bottom = 4.dp, end = 6.dp)
                        .animateItem(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = mediaType.name.lowercase(),
                        style = LocalTheme.current.styles.category.copy(
                            color = Color.White
                        )
                    )
                    Icon(
                        modifier = Modifier.size(20.dp),
                        imageVector = Icons.Outlined.Close,
                        contentDescription = null,
                        tint = Color.White
                    )
                }
            }
        }
    }
}
