package ui.conversation.settings

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Label
import androidx.compose.material.icons.outlined.Brush
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.FaceRetouchingOff
import androidx.compose.material.icons.outlined.PersonRemove
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.paging.LoadState
import app.cash.paging.compose.collectAsLazyPagingItems
import augmy.composeapp.generated.resources.Res
import augmy.composeapp.generated.resources.accessibility_change_avatar
import augmy.composeapp.generated.resources.accessibility_change_username
import augmy.composeapp.generated.resources.button_block
import augmy.composeapp.generated.resources.button_confirm
import augmy.composeapp.generated.resources.button_dismiss
import augmy.composeapp.generated.resources.conversation_kick_message
import augmy.composeapp.generated.resources.conversation_kick_title
import augmy.composeapp.generated.resources.items_see_more
import augmy.composeapp.generated.resources.screen_conversation_settings
import augmy.interactive.shared.ext.scalingClickable
import augmy.interactive.shared.ui.components.MinimalisticComponentIcon
import augmy.interactive.shared.ui.components.MinimalisticFilledIcon
import augmy.interactive.shared.ui.components.dialog.AlertDialog
import augmy.interactive.shared.ui.components.dialog.ButtonState
import augmy.interactive.shared.ui.components.input.CustomTextField
import augmy.interactive.shared.ui.theme.LocalTheme
import augmy.interactive.shared.ui.theme.SharedColors
import base.BrandBaseScreen
import base.navigation.NavIconType
import base.utils.getOrNull
import components.UserProfileImage
import components.network.NetworkItemRow
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.context.loadKoinModules
import org.koin.core.parameter.parametersOf
import ui.conversation.settings.ConversationSettingsModel.Companion.MAX_MEMBERS_COUNT
import ui.network.components.ScalingIcon
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@Composable
fun ConversationSettingsScreen(conversationId: String?) {
    BrandBaseScreen(
        title = stringResource(Res.string.screen_conversation_settings),
        navIconType = NavIconType.CLOSE
    ) {
        ConversationSettingsContent(conversationId = conversationId)
    }
}

@OptIn(ExperimentalUuidApi::class)
@Composable
fun ConversationSettingsContent(conversationId: String?) {
    loadKoinModules(conversationSettingsModule)
    val model: ConversationSettingsModel = koinViewModel(
        key = conversationId,
        parameters = {
            parametersOf(conversationId ?: "")
        }
    )

    val detail = model.conversation.collectAsState(null)
    val members = model.members.collectAsLazyPagingItems()
    val listState = rememberLazyListState()

    val isLoadingInitialPage = members.loadState.refresh is LoadState.Loading
            || (members.itemCount == 0 && !members.loadState.append.endOfPaginationReached)

    val showPictureChangeDialog = remember { mutableStateOf(false) }
    val selectedMemberId = remember { mutableStateOf<String?>(null) }
    val kickMemberUserId = remember { mutableStateOf<String?>(null) }
    val enableMembersPaging = rememberSaveable { mutableStateOf(false) }

    kickMemberUserId.value?.let { memberId ->
        AlertDialog(
            title = stringResource(Res.string.conversation_kick_title),
            message = AnnotatedString(stringResource(Res.string.conversation_kick_message)),
            icon = Icons.Outlined.PersonRemove,
            confirmButtonState = ButtonState(text = stringResource(Res.string.button_confirm)) {
                model.kickMember(memberId)
            },
            dismissButtonState = ButtonState(text = stringResource(Res.string.button_dismiss)),
            onDismissRequest = {
                kickMemberUserId.value = null
            }
        )
    }

    if(showPictureChangeDialog.value) {
        DialogChangeRoomAvatar(
            detail = detail.value,
            model = model,
            onDismissRequest = {
                showPictureChangeDialog.value = false
            }
        )
    }

    LaunchedEffect(Unit) {
        snapshotFlow { listState.firstVisibleItemIndex }.collectLatest {
            if(it > 2) {
                // TODO show members filter
            }else {
                // TODO if members filter was visible, stop the pagination
            }
        }
    }

    LazyColumn(
        modifier = Modifier
            .background(color = LocalTheme.current.colors.backgroundDark)
            .fillMaxSize()
            .padding(horizontal = 6.dp),
        state = listState,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        item {
            Box(modifier = Modifier.padding(top = 6.dp)) {
                UserProfileImage(
                    modifier = Modifier
                        .zIndex(5f)
                        .fillMaxWidth(.5f),
                    media = detail.value?.summary?.avatar,
                    tag = detail.value?.summary?.tag,
                    name = detail.value?.summary?.roomName
                )
                MinimalisticFilledIcon(
                    modifier = Modifier
                        .padding(bottom = 8.dp, end = 8.dp)
                        .align(Alignment.BottomEnd),
                    onTap = {
                        showPictureChangeDialog.value = true
                    },
                    imageVector = Icons.Outlined.Brush,
                    contentDescription = stringResource(Res.string.accessibility_change_avatar)
                )
            }
        }
        item {
            RoomNameContent(
                model = model,
                roomName = detail.value?.summary?.roomName ?: ""
            )
        }
        items(
            count = when {
                members.itemCount == 0 && isLoadingInitialPage -> MAX_MEMBERS_COUNT
                enableMembersPaging.value -> members.itemCount
                else -> MAX_MEMBERS_COUNT
            },
            key = { index -> members.getOrNull(index)?.id ?: Uuid.random().toString() }
        ) { index ->
            val member = members.getOrNull(index)
            val isSelected = selectedMemberId.value == member?.id
            val itemModifier = Modifier
                .animateItem()
                .scalingClickable(
                    hoverEnabled = !isSelected,
                    scaleInto = .9f,
                    onTap = {
                        selectedMemberId.value = if(selectedMemberId.value == member?.id) null else member?.id
                    }
                )
                .fillMaxWidth()
                .then(
                    (if (selectedMemberId.value != null && isSelected) {
                        Modifier
                            .background(
                                color = LocalTheme.current.colors.backgroundLight,
                                shape = LocalTheme.current.shapes.rectangularActionShape
                            )
                            .border(
                                width = 2.dp,
                                color = LocalTheme.current.colors.backgroundDark,
                                shape = LocalTheme.current.shapes.rectangularActionShape
                            )
                    }else Modifier)
                )

            NetworkItemRow(
                modifier = itemModifier,
                data = member?.toNetworkItem(),
                isSelected = isSelected,
                actions = {
                    Row(modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)) {
                        ScalingIcon(
                            color = SharedColors.RED_ERROR.copy(.6f),
                            imageVector = Icons.Outlined.FaceRetouchingOff,
                            contentDescription = stringResource(Res.string.button_block),
                            onClick = {
                                kickMemberUserId.value = member?.userId
                            }
                        )
                    }
                }
            )
        }
        if (!isLoadingInitialPage && members.itemCount > MAX_MEMBERS_COUNT && !enableMembersPaging.value) {
            item {
                Text(
                    modifier = Modifier.scalingClickable {
                        selectedMemberId.value = null
                        enableMembersPaging.value = true
                    },
                    text = stringResource(
                        Res.string.items_see_more,
                        (detail.value?.summary?.joinedMemberCount ?: 0) - MAX_MEMBERS_COUNT
                    ),
                    style = LocalTheme.current.styles.category.copy(
                        color = LocalTheme.current.colors.secondary
                    )
                )
            }
        }
    }
}

@Composable
private fun RoomNameContent(
    modifier: Modifier = Modifier,
    model: ConversationSettingsModel,
    roomName: String
) {
    val isLoading = model.isLoading.collectAsState()
    val showNameChangeLauncher = remember(roomName) { mutableStateOf(false) }

    Crossfade(targetState = showNameChangeLauncher.value) { change ->
        Row(
            modifier = Modifier.padding(vertical = 8.dp, horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            if(change) {
                val focusRequester = remember { FocusRequester() }
                val roomNameState = remember {
                    TextFieldState(roomName)
                }
                val isValid = remember {
                    derivedStateOf {
                        roomName != roomNameState.text && !isLoading.value
                    }
                }

                LaunchedEffect(roomNameState.text) {
                    if(roomNameState.text.isBlank()) showNameChangeLauncher.value = false
                }

                LaunchedEffect(Unit) {
                    delay(200)
                    focusRequester.requestFocus()
                }

                CustomTextField(
                    modifier = Modifier.weight(1f),
                    focusRequester = focusRequester,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Text,
                        imeAction = ImeAction.Done
                    ),
                    enabled = !isLoading.value,
                    onKeyboardAction = {
                        if(isValid.value) model.requestRoomNameChange(roomNameState.text)
                    },
                    paddingValues = PaddingValues(start = 16.dp),
                    prefixIcon = Icons.AutoMirrored.Outlined.Label,
                    state = roomNameState,
                    isClearable = !isLoading.value
                )

                Crossfade(isLoading.value) {
                    if(isLoading.value) {
                        CircularProgressIndicator(
                            modifier = Modifier.requiredSize(32.dp),
                            color = LocalTheme.current.colors.brandMain,
                            trackColor = LocalTheme.current.colors.tetrial
                        )
                    }else {
                        MinimalisticComponentIcon(
                            imageVector = Icons.Outlined.Check,
                            tint = if(isValid.value) {
                                LocalTheme.current.colors.brandMain
                            }else LocalTheme.current.colors.disabled,
                            contentDescription = stringResource(Res.string.accessibility_change_username),
                            onTap = {
                                if(isValid.value) model.requestRoomNameChange(roomNameState.text)
                            }
                        )
                    }
                }
            }else {
                Text(
                    modifier = Modifier.padding(end = 8.dp),
                    text = roomName,
                    style = LocalTheme.current.styles.subheading
                )
                MinimalisticComponentIcon(
                    imageVector = Icons.Outlined.Edit,
                    contentDescription = stringResource(Res.string.accessibility_change_username),
                    onTap = {
                        showNameChangeLauncher.value = true
                    }
                )
            }
        }
    }
}
