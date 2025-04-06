package ui.conversation.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Brush
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.FaceRetouchingOff
import androidx.compose.material.icons.outlined.PersonRemove
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
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
import augmy.interactive.shared.ui.base.LocalNavController
import augmy.interactive.shared.ui.components.MinimalisticComponentIcon
import augmy.interactive.shared.ui.components.MinimalisticFilledIcon
import augmy.interactive.shared.ui.components.dialog.AlertDialog
import augmy.interactive.shared.ui.components.dialog.ButtonState
import augmy.interactive.shared.ui.theme.LocalTheme
import augmy.interactive.shared.ui.theme.SharedColors
import base.BrandBaseScreen
import base.navigation.NavIconType
import components.UserProfileImage
import components.network.NetworkItemRow
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.context.loadKoinModules
import org.koin.core.parameter.parametersOf
import ui.network.components.ScalingIcon

@Composable
fun ConversationSettingsScreen(conversationId: String?) {
    BrandBaseScreen(
        title = stringResource(Res.string.screen_conversation_settings),
        navIconType = NavIconType.CLOSE
    ) {
        ConversationSettingsContent(conversationId = conversationId)
    }
}

@Composable
fun ConversationSettingsContent(conversationId: String?) {
    loadKoinModules(conversationSettingsModule)
    val model: ConversationSettingsModel = koinViewModel(
        key = conversationId,
        parameters = {
            parametersOf(conversationId ?: "")
        }
    )
    val navController = LocalNavController.current

    val detail = model.conversation.collectAsState(null)
    val members = model.members.collectAsState()

    val showPictureChangeDialog = remember { mutableStateOf(false) }
    val showNameChangeLauncher = remember { mutableStateOf(false) }
    val selectedMemberId = remember { mutableStateOf<String?>(null) }
    val kickMemberUserId = remember { mutableStateOf<String?>(null) }

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
        // TODO change profile picture
    }
    if(showNameChangeLauncher.value) {
        // TODO change alias
    }

    LazyColumn(
        modifier = Modifier
            .background(color = LocalTheme.current.colors.backgroundDark)
            .fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        item {
            Box {
                UserProfileImage(
                    modifier = Modifier
                        .zIndex(5f)
                        .fillMaxWidth(.5f),
                    media = detail.value?.summary?.avatar,
                    tag = detail.value?.summary?.tag,
                    name = detail.value?.summary?.alias
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
            Row(
                modifier = Modifier.padding(vertical = 8.dp, horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    modifier = Modifier.padding(end = 8.dp),
                    text = detail.value?.summary?.alias ?: "",
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
        items(
            items = members.value.take(MAX_MEMBERS_COUNT),
            key = { member -> member.id }
        ) { member ->
            val itemModifier = Modifier
                .animateItem()
                .scalingClickable(
                    hoverEnabled = selectedMemberId.value != member.id,
                    scaleInto = .9f,
                    onTap = {
                        selectedMemberId.value = member.id
                    }
                )
                .fillMaxWidth()
                .then(
                    (if (selectedMemberId.value != null && selectedMemberId.value == member.id) {
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
                data = member.toNetworkItem(),
                isSelected = selectedMemberId.value == member.id,
                actions = {
                    Row {
                        ScalingIcon(
                            color = SharedColors.RED_ERROR.copy(.6f),
                            imageVector = Icons.Outlined.FaceRetouchingOff,
                            contentDescription = stringResource(Res.string.button_block),
                            onClick = {
                                kickMemberUserId.value = member.userId
                            }
                        )
                    }
                }
            )
        }
        if (members.value.size > MAX_MEMBERS_COUNT) {
            item {
                Text(
                    modifier = Modifier.scalingClickable {
                        selectedMemberId.value = null
                        model.paginateMembers(true)
                    },
                    text = stringResource(Res.string.items_see_more, members.value.size - MAX_MEMBERS_COUNT),
                    style = LocalTheme.current.styles.subheading.copy(
                        color = LocalTheme.current.colors.secondary
                    )
                )
            }
        }
    }
}

private const val MAX_MEMBERS_COUNT = 8
