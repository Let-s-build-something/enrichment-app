package ui.conversation.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Label
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.outlined.Brush
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Face
import androidx.compose.material.icons.outlined.FaceRetouchingOff
import androidx.compose.material.icons.outlined.PersonAddAlt
import androidx.compose.material.icons.outlined.PersonRemove
import androidx.compose.material.icons.outlined.QuestionAnswer
import androidx.compose.material.icons.outlined.SensorOccupied
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import augmy.composeapp.generated.resources.accessibility_add_new_member
import augmy.composeapp.generated.resources.accessibility_change_avatar
import augmy.composeapp.generated.resources.accessibility_change_username
import augmy.composeapp.generated.resources.account_sign_out_message
import augmy.composeapp.generated.resources.button_block
import augmy.composeapp.generated.resources.button_confirm
import augmy.composeapp.generated.resources.button_dismiss
import augmy.composeapp.generated.resources.button_verify
import augmy.composeapp.generated.resources.button_yes
import augmy.composeapp.generated.resources.conversation_action_invite_message
import augmy.composeapp.generated.resources.conversation_action_invite_title
import augmy.composeapp.generated.resources.conversation_action_leave
import augmy.composeapp.generated.resources.conversation_action_leave_hint
import augmy.composeapp.generated.resources.conversation_kick_message
import augmy.composeapp.generated.resources.conversation_kick_title
import augmy.composeapp.generated.resources.conversation_left_message
import augmy.composeapp.generated.resources.conversation_members
import augmy.composeapp.generated.resources.device_verification_success
import augmy.composeapp.generated.resources.items_see_more
import augmy.composeapp.generated.resources.leave_app_dialog_title
import augmy.composeapp.generated.resources.message_user_verification_awaiting
import augmy.composeapp.generated.resources.message_user_verification_canceled
import augmy.composeapp.generated.resources.message_user_verification_ready
import augmy.composeapp.generated.resources.message_user_verification_start
import augmy.composeapp.generated.resources.screen_conversation_settings
import augmy.interactive.shared.ext.brandShimmerEffect
import augmy.interactive.shared.ext.scalingClickable
import augmy.interactive.shared.ui.base.LocalNavController
import augmy.interactive.shared.ui.base.LocalSnackbarHost
import augmy.interactive.shared.ui.components.MinimalisticComponentIcon
import augmy.interactive.shared.ui.components.MinimalisticFilledIcon
import augmy.interactive.shared.ui.components.MinimalisticIcon
import augmy.interactive.shared.ui.components.OutlinedButton
import augmy.interactive.shared.ui.components.dialog.AlertDialog
import augmy.interactive.shared.ui.components.dialog.ButtonState
import augmy.interactive.shared.ui.components.input.CustomTextField
import augmy.interactive.shared.ui.theme.LocalTheme
import augmy.interactive.shared.ui.theme.SharedColors
import base.BrandBaseScreen
import base.navigation.NavIconType
import base.navigation.NavigationArguments
import base.navigation.NavigationNode
import base.utils.getOrNull
import collectResult
import components.AvatarImage
import components.network.NetworkItemRow
import data.NetworkProximityCategory
import data.io.base.BaseResponse
import data.io.matrix.room.event.ConversationRoomMember
import korlibs.math.toInt
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.folivo.trixnity.client.verification.ActiveVerificationState
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.context.loadKoinModules
import org.koin.core.parameter.parametersOf
import ui.conversation.settings.ConversationSettingsModel.Companion.MAX_MEMBERS_COUNT
import ui.conversation.settings.ConversationSettingsModel.Companion.SHIMMER_ITEM_COUNT
import ui.conversation.settings.ConversationSettingsModel.Companion.isFinished
import ui.network.components.ScalingIcon
import ui.network.components.user_detail.AddToCircleAction
import ui.network.components.user_detail.UserDetailDialog
import kotlin.uuid.ExperimentalUuidApi

@Composable
fun ConversationSettingsScreen(conversationId: String?) {
    BrandBaseScreen(
        title = stringResource(Res.string.screen_conversation_settings),
        navIconType = NavIconType.CLOSE
    ) {
        val showLeaveDialog = remember { mutableStateOf(false) }
        loadKoinModules(conversationSettingsModule)
        val model = koinViewModel<ConversationSettingsModel>(
            key = conversationId,
            parameters = {
                parametersOf(conversationId ?: "")
            }
        )

        if(showLeaveDialog.value) {
            val reasonState = remember { TextFieldState() }

            AlertDialog(
                title = stringResource(Res.string.leave_app_dialog_title),
                message = AnnotatedString(stringResource(Res.string.account_sign_out_message)),
                confirmButtonState = ButtonState(
                    text = stringResource(Res.string.button_yes),
                    onClick = {
                        model.leaveRoom(reason = reasonState.text)
                    }
                ),
                additionalContent = {
                    CustomTextField(
                        modifier = Modifier
                            .padding(top = 8.dp)
                            .fillMaxWidth(),
                        hint = stringResource(Res.string.conversation_action_leave_hint),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Text,
                            imeAction = ImeAction.Done
                        ),
                        paddingValues = PaddingValues(start = 16.dp),
                        prefixIcon = Icons.Outlined.QuestionAnswer,
                        state = reasonState
                    )
                },
                dismissButtonState = ButtonState(text = stringResource(Res.string.button_dismiss)),
                icon = Icons.AutoMirrored.Outlined.Logout,
                onDismissRequest = {
                    showLeaveDialog.value = false
                }
            )
        }

        val conversation = model.conversation.collectAsState(null)

        ConversationSettingsContent(
            modifier = Modifier
                .background(color = LocalTheme.current.colors.backgroundDark)
                .fillMaxSize()
                .padding(horizontal = 6.dp),
            conversationId = conversationId,
            model = model,
            additionalContent = {
                if (conversation.value?.data?.summary?.isDirect == false) {
                    item {
                        val ongoingChange = model.ongoingChange.collectAsState()
                        val isLoading = ongoingChange.value is ConversationSettingsModel.ChangeType.Leave
                                && ongoingChange.value?.state is BaseResponse.Loading

                        OutlinedButton(
                            modifier = Modifier.padding(top = 32.dp),
                            activeColor = SharedColors.RED_ERROR_50,
                            inactiveColor = SharedColors.RED_ERROR.copy(alpha = 0.25f),
                            enabled = !isLoading,
                            text = stringResource(Res.string.conversation_action_leave),
                            content = {
                                AnimatedVisibility(isLoading) {
                                    CircularProgressIndicator(
                                        modifier = Modifier
                                            .padding(start = 4.dp)
                                            .requiredSize(24.dp),
                                        color = SharedColors.RED_ERROR_50,
                                        trackColor = LocalTheme.current.colors.disabled
                                    )
                                }
                            }
                        ) {
                            showLeaveDialog.value = true
                        }
                    }
                }
            }
        )
    }
}

@OptIn(ExperimentalUuidApi::class)
@Composable
fun ConversationSettingsContent(
    modifier: Modifier = Modifier,
    conversationId: String?,
    model: ConversationSettingsModel  = koinViewModel(
        key = conversationId,
        parameters = {
            parametersOf(conversationId ?: "")
        }
    ),
    inviteEnabled: Boolean = true,
    additionalContent: LazyListScope.() -> Unit = {}
) {
    val navController = LocalNavController.current
    val snackbarHost = LocalSnackbarHost.current
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    val detail = model.conversation.collectAsState(null)
    val ongoingChange = model.ongoingChange.collectAsState()
    val selectedUserToInvite = model.selectedInvitedUser.collectAsState()
    val directUser = detail.value?.members?.firstOrNull()?.toNetworkItem()
    val members = model.members.collectAsLazyPagingItems()

    val isLoadingInitialPage = members.loadState.refresh is LoadState.Loading
            || (members.itemCount == 0 && !members.loadState.append.endOfPaginationReached)

    val showPictureChangeDialog = remember { mutableStateOf(false) }
    val selectedMemberId = remember { mutableStateOf<String?>(null) }
    val selectedMemberDetail = remember { mutableStateOf<ConversationRoomMember?>(null) }
    val kickMemberUserId = remember { mutableStateOf<String?>(null) }
    val enableMembersPaging = rememberSaveable { mutableStateOf(false) }


    navController?.collectResult<String?>(
        key = NavigationArguments.SEARCH_USER_ID,
        defaultValue = null,
        listener = { userId ->
            if (userId != null) model.selectInvitedUser(userId)
        }
    )

    LaunchedEffect(Unit) {
        snapshotFlow { listState.firstVisibleItemIndex }.collectLatest {
            // TODO filtering
            if(it > 2) {
                // show members filter
            }else {
                // if members filter was visible, stop the pagination
            }
        }
    }

    LaunchedEffect(Unit) {
        model.ongoingChange.collectLatest { change ->
            if(change is ConversationSettingsModel.ChangeType.Leave && change.state is BaseResponse.Success) {
                CoroutineScope(Job()).launch {
                    snackbarHost?.showSnackbar(getString(Res.string.conversation_left_message))
                }
                navController?.currentBackStackEntry?.savedStateHandle?.set(
                    NavigationArguments.CONVERSATION_LEFT,
                    true
                )
            }
        }
    }


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

    selectedUserToInvite.value?.let { user ->
        AlertDialog(
            title = stringResource(Res.string.conversation_action_invite_title),
            confirmButtonState = ButtonState(
                text = stringResource(Res.string.button_confirm),
                onClick = {
                    model.inviteMembers(user.userId ?: "")
                }
            ),
            additionalContent = {
                NetworkItemRow(data = user)
                Text(
                    modifier = Modifier.padding(top = 4.dp),
                    text = stringResource(Res.string.conversation_action_invite_message),
                    style = LocalTheme.current.styles.regular
                )
            },
            dismissButtonState = ButtonState(text = stringResource(Res.string.button_dismiss)),
            icon = Icons.Outlined.Face,
            onDismissRequest = {
                model.selectInvitedUser(null)
            }
        )
    }

    selectedMemberDetail.value?.let {
        UserDetailDialog(
            member = it,
            onDismissRequest = {
                selectedMemberDetail.value = null
            }
        )
    }

    LazyColumn(
        modifier = modifier,
        state = listState,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        item {
            Box(modifier = Modifier.padding(top = 6.dp)) {
                AvatarImage(
                    modifier = Modifier.fillMaxWidth(.5f),
                    media = detail.value?.avatar ?: directUser?.avatar,
                    tag = detail.value?.tag ?: directUser?.tag,
                    name = detail.value?.name ?: directUser?.displayName
                )
                AnimatedVisibility(
                    modifier = Modifier.align(Alignment.BottomEnd),
                    visible = detail.value != null
                ) {
                    MinimalisticFilledIcon(
                        modifier = Modifier.padding(bottom = 8.dp, end = 8.dp),
                        onTap = {
                            showPictureChangeDialog.value = true
                        },
                        imageVector = Icons.Outlined.Brush,
                        contentDescription = stringResource(Res.string.accessibility_change_avatar)
                    )
                }
            }
        }
        item {
            RoomNameContent(
                model = model,
                roomName = detail.value?.name ?: directUser?.displayName
            )
        }
        if(detail.value?.data?.summary?.isDirect != true) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        modifier = Modifier.padding(start = 6.dp),
                        text = stringResource(Res.string.conversation_members),
                        style = LocalTheme.current.styles.title.copy(
                            color = LocalTheme.current.colors.disabled
                        )
                    )
                    if (inviteEnabled) {
                        MinimalisticIcon(
                            imageVector = Icons.Outlined.PersonAddAlt,
                            contentDescription = stringResource(Res.string.accessibility_add_new_member),
                            onTap = {
                                scope.launch {
                                    navController?.navigate(
                                        NavigationNode.SearchUser(
                                            awaitingResult = true,
                                            excludeUsers = withContext(Dispatchers.Default) {
                                                members.itemSnapshotList.joinToString(",") { it?.userId ?: "" }
                                            }
                                        )
                                    )
                                }
                            }
                        )
                    }
                }
            }
            items(
                count = when {
                    members.itemCount == 0 && isLoadingInitialPage -> SHIMMER_ITEM_COUNT
                    enableMembersPaging.value -> members.itemCount
                    else -> members.itemCount.coerceAtMost(MAX_MEMBERS_COUNT)
                },
                //key = { index -> members.getOrNull(index)?.id ?: Uuid.random().toString() }
            ) { index ->
                val member = members.getOrNull(index)
                val isSelected = selectedMemberId.value == member?.id
                val itemModifier = Modifier
                    .animateItem()
                    .scalingClickable(
                        hoverEnabled = !isSelected,
                        scaleInto = .95f,
                        onTap = {
                            model.checkVerificationState(userId = member?.id)
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
                    onAvatarClick = {
                        selectedMemberDetail.value = member
                    },
                    actions = {
                        Column {
                            val verifications = model.verifications.collectAsState()
                            val verification = verifications.value[member?.id]

                            AnimatedVisibility(verification != null) {
                                Row(
                                    modifier = Modifier
                                        .background(
                                            color = LocalTheme.current.colors.backgroundDark,
                                            shape = LocalTheme.current.shapes.rectangularActionShape
                                        )
                                        .fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = stringResource(
                                            when(verification?.state?.value) {
                                                is ActiveVerificationState.Done -> Res.string.device_verification_success
                                                is ActiveVerificationState.Ready -> Res.string.message_user_verification_ready
                                                is ActiveVerificationState.Start -> Res.string.message_user_verification_start
                                                is ActiveVerificationState.Cancel -> Res.string.message_user_verification_canceled
                                                else -> Res.string.message_user_verification_awaiting
                                            }
                                        ),
                                        style = LocalTheme.current.styles.regular
                                    )

                                    AnimatedVisibility(verification?.state?.value?.isFinished() == false) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.requiredSize(24.dp),
                                            color = LocalTheme.current.colors.disabled,
                                            trackColor = LocalTheme.current.colors.disabledComponent
                                        )
                                    }
                                }
                            }
                            Row(modifier = Modifier.padding(bottom = 4.dp)) {
                                Spacer(Modifier.width(6.dp))
                                ScalingIcon(
                                    color = SharedColors.RED_ERROR.copy(.6f),
                                    imageVector = Icons.Outlined.FaceRetouchingOff,
                                    contentDescription = stringResource(Res.string.button_block),
                                    onClick = {
                                        kickMemberUserId.value = member?.userId
                                    }
                                )
                                AnimatedVisibility(ongoingChange.value !is ConversationSettingsModel.ChangeType.VerifyMember) {
                                    ScalingIcon(
                                        color = SharedColors.YELLOW.copy(.6f),
                                        imageVector = Icons.Outlined.SensorOccupied,
                                        contentDescription = stringResource(Res.string.button_verify),
                                        onClick = {
                                            model.verifyUser(userId = member?.userId)
                                        }
                                    )
                                }
                            }
                        }
                    }
                )
            }
            if (!isLoadingInitialPage
                && members.itemCount > MAX_MEMBERS_COUNT
                && !enableMembersPaging.value
            ) {
                item {
                    Text(
                        modifier = Modifier.scalingClickable {
                            selectedMemberId.value = null
                            enableMembersPaging.value = true
                        },
                        text = stringResource(
                            Res.string.items_see_more,
                            (detail.value?.data?.summary?.joinedMemberCount
                                ?: detail.value?.members?.size)?.minus(MAX_MEMBERS_COUNT)?.toString() ?: ""
                        ),
                        style = LocalTheme.current.styles.category.copy(
                            color = LocalTheme.current.colors.secondary
                        )
                    )
                }
            }
        }else {
            item {
                val socialCircleColors = model.socialCircleColors.collectAsState(initial = null)

                AnimatedVisibility(visible = directUser?.proximity != null) {
                    directUser?.proximity?.let { proximity ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val selectedCategory = NetworkProximityCategory.entries.firstOrNull { proximity in it.range }
                                ?: NetworkProximityCategory.Community

                            Text(
                                text = stringResource(selectedCategory.res),
                                style = LocalTheme.current.styles.regular
                            )

                            Box(
                                modifier = Modifier.weight(1f),
                                contentAlignment = Alignment.Center
                            ) {
                                var previousShares = 0.0
                                val entries = NetworkProximityCategory.entries

                                entries.toList().forEach { category ->
                                    val zIndex = entries.size - entries.indexOf(category) + 1f
                                    val shares = (if (selectedCategory == category) .7f else .3f / entries.size) + previousShares

                                    Box(
                                        modifier = Modifier
                                            .background(
                                                (socialCircleColors.value?.get(category) ?: category.color).copy(
                                                    alpha = if (selectedCategory == category) 1f else .5f
                                                )
                                            )
                                            .zIndex(zIndex)
                                            .fillMaxWidth(shares.toFloat())
                                            .height(8.dp)
                                    )
                                    previousShares = shares
                                }
                            }
                        }
                    }
                }
            }
            item {
                if (directUser != null) {
                    AddToCircleAction(
                        modifier = Modifier.padding(top = 16.dp),
                        user = directUser
                    )
                }
            }
        }
        additionalContent()
    }
}

@Composable
private fun RoomNameContent(
    modifier: Modifier = Modifier,
    model: ConversationSettingsModel,
    roomName: String?
) {
    val ongoingChange = model.ongoingChange.collectAsState()
    val isLoading = ongoingChange.value?.state is BaseResponse.Loading
    val showNameChangeLauncher = remember(roomName) { mutableStateOf(false) }

    Crossfade(
        modifier = modifier,
        targetState = when {
            roomName == null -> 2
            else -> showNameChangeLauncher.value.toInt()
        }
    ) { stateIndex ->
        Row(
            modifier = Modifier.padding(vertical = 8.dp, horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            when (stateIndex) {
                0 -> {
                    Text(
                        modifier = Modifier.padding(end = 8.dp),
                        text = roomName ?: "",
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
                1 -> {
                    val focusRequester = remember { FocusRequester() }
                    val roomNameState = remember {
                        TextFieldState(roomName ?: "")
                    }
                    val isValid = remember {
                        derivedStateOf {
                            roomName != roomNameState.text && !isLoading
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
                        enabled = !isLoading,
                        onKeyboardAction = {
                            if(isValid.value) model.requestRoomNameChange(roomNameState.text)
                        },
                        paddingValues = PaddingValues(start = 16.dp),
                        prefixIcon = Icons.AutoMirrored.Outlined.Label,
                        state = roomNameState,
                        isClearable = !isLoading
                    )

                    Crossfade(isLoading && ongoingChange.value is ConversationSettingsModel.ChangeType.Name) {
                        if(isLoading) {
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
                }
                2 -> Text(
                    modifier = Modifier
                        .fillMaxWidth(.4f)
                        .brandShimmerEffect(),
                    text = "",
                    style = LocalTheme.current.styles.subheading
                )
            }
        }
    }
}
