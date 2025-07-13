package ui.search.room

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Fingerprint
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.LocalPostOffice
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.RoomService
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.paging.LoadState
import app.cash.paging.compose.collectAsLazyPagingItems
import augmy.composeapp.generated.resources.Res
import augmy.composeapp.generated.resources.accessibility_change_homeserver
import augmy.composeapp.generated.resources.action_clear
import augmy.composeapp.generated.resources.action_search_users
import augmy.composeapp.generated.resources.room_search_empty_text
import augmy.composeapp.generated.resources.screen_search_user
import augmy.interactive.shared.ext.scalingClickable
import augmy.interactive.shared.ui.components.dialog.ButtonState
import augmy.interactive.shared.ui.components.input.CustomTextField
import augmy.interactive.shared.ui.theme.LocalTheme
import base.BrandBaseScreen
import base.navigation.NavIconType
import base.utils.getOrNull
import components.EmptyLayout
import components.network.NetworkItemRow
import data.io.base.BaseResponse
import data.io.social.network.conversation.message.MediaIO
import data.io.user.NetworkItemIO
import net.folivo.trixnity.clientserverapi.model.rooms.GetPublicRoomsResponse
import net.folivo.trixnity.core.model.events.m.room.JoinRulesEventContent
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.context.loadKoinModules
import ui.conversation.settings.ConversationDetailDialog
import ui.search.room.SearchRoomModel.Companion.ITEMS_COUNT
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
@Composable
fun SearchRoomScreen() {
    loadKoinModules(searchRoomModule)
    val model = koinViewModel<SearchRoomModel>()

    val searchState = remember { TextFieldState() }
    val showHomeServerPicker = remember { mutableStateOf(false) }
    val selectedRoom = remember { mutableStateOf<GetPublicRoomsResponse.PublicRoomsChunk?>(null) }

    val rooms = model.rooms.collectAsLazyPagingItems()
    val homeserver = model.selectedHomeserver.collectAsState()

    val isLoadingInitialPage = (rooms.loadState.refresh is LoadState.Loading
            || (rooms.itemCount == 0 && !rooms.loadState.append.endOfPaginationReached))
            && searchState.text.isBlank()


    LaunchedEffect(searchState.text) {
        model.queryRooms(prompt = searchState.text)
    }

    if(showHomeServerPicker.value) {
        /*MatrixHomeserverPicker(
            homeserver = homeserver.value,
            onDismissRequest = { showHomeServerPicker.value = false },
            onSelect = {
                model.selectHomeserver(it)
            }
        )*/
    }

    selectedRoom.value?.let { room ->
        // TODO dialog actions, can be just composable parameter
        ConversationDetailDialog(
            conversationId = room.roomId.full,
            onDismissRequest = {
                selectedRoom.value = null
            }
        )
    }

    BrandBaseScreen(
        title = stringResource(Res.string.screen_search_user),
        navIconType = NavIconType.CLOSE
    ) {
        LazyColumn {
            stickyHeader(key = "searchBar") {
                val state = model.state.collectAsState()

                Row(
                    modifier = Modifier
                        .zIndex(1f)
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CustomTextField(
                        modifier = Modifier
                            .weight(1f)
                            .background(
                                color = LocalTheme.current.colors.backgroundDark,
                                shape = LocalTheme.current.shapes.rectangularActionShape
                            )
                            .padding(horizontal = 4.dp, vertical = 2.dp),
                        shape = LocalTheme.current.shapes.rectangularActionShape,
                        hint = stringResource(Res.string.action_search_users),
                        showBorders = false,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Text,
                            imeAction = ImeAction.Search
                        ),
                        prefixIcon = Icons.Outlined.Search,
                        state = searchState,
                        enabled = state.value !is BaseResponse.Loading,
                        isClearable = true
                    )

                    Crossfade(state.value is BaseResponse.Loading) { isLoading ->
                        Box(
                            modifier = Modifier
                                .scalingClickable { showHomeServerPicker.value = true }
                                .background(
                                    color = LocalTheme.current.colors.backgroundDark,
                                    shape = LocalTheme.current.shapes.rectangularActionShape
                                )
                                .padding(12.dp)
                        ) {
                            if (isLoading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.requiredSize(24.dp),
                                    color = LocalTheme.current.colors.disabled,
                                    trackColor = LocalTheme.current.colors.disabledComponent
                                )
                            } else {
                                Icon(
                                    modifier = Modifier.size(24.dp),
                                    imageVector = Icons.Outlined.Home,
                                    tint = LocalTheme.current.colors.disabled,
                                    contentDescription = stringResource(Res.string.accessibility_change_homeserver)
                                )
                            }
                        }
                    }
                }
            }
            item(key = "emptyLayout") {
                val state = model.state.collectAsState()
                val isEmpty = state.value is BaseResponse.Idle && !isLoadingInitialPage
                        && rooms.itemCount == 0

                AnimatedVisibility(isEmpty) {
                    EmptyLayout(
                        modifier = Modifier.fillMaxWidth(),
                        title = stringResource(Res.string.room_search_empty_text),
                        secondaryAction = ButtonState(
                            stringResource(Res.string.action_clear)
                        ) {
                            searchState.setTextAndPlaceCursorAtEnd("")
                        }
                    )
                }
            }
            items(
                count = if (isLoadingInitialPage) ITEMS_COUNT else rooms.itemCount,
                key = { rooms.getOrNull(it)?.roomId?.full ?: Uuid.random().toString() }
            ) { index ->
                val room = rooms.getOrNull(index)

                Row(
                    modifier = Modifier
                        .padding(horizontal = 16.dp)
                        .animateItem(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        modifier = Modifier.size(18.dp),
                        imageVector = when (room?.joinRule) {
                            JoinRulesEventContent.JoinRule.Public -> Icons.Outlined.Language
                            JoinRulesEventContent.JoinRule.Invite -> Icons.Outlined.LocalPostOffice
                            JoinRulesEventContent.JoinRule.Knock,
                            JoinRulesEventContent.JoinRule.KnockRestricted -> Icons.Outlined.RoomService
                            JoinRulesEventContent.JoinRule.Restricted -> Icons.Outlined.Fingerprint
                            else -> Icons.Outlined.Lock
                        },
                        tint = LocalTheme.current.colors.disabled,
                        contentDescription = room?.joinRule?.name
                    )

                    NetworkItemRow(
                        modifier = Modifier
                            .weight(1f)
                            .scalingClickable(
                                scaleInto = .95f,
                                enabled = room != null
                            ) {
                                selectedRoom.value = room
                            }
                            .fillMaxWidth(),
                        highlight = searchState.text.toString().lowercase(),
                        data = NetworkItemIO(
                            displayName = room?.name ?: room?.roomId?.full,
                            avatar = room?.avatarUrl?.let { MediaIO(it) },
                            userId = room?.roomId?.full,
                            lastMessage = room?.topic
                        )
                    )
                }
            }
        }
    }
}
