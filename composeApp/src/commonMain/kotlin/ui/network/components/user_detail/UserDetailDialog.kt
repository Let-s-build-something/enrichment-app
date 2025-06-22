package ui.network.components.user_detail

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Chat
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.IosShare
import androidx.compose.material.icons.outlined.TrackChanges
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import augmy.composeapp.generated.resources.Res
import augmy.composeapp.generated.resources.accessibility_cancel
import augmy.composeapp.generated.resources.accessibility_share
import augmy.composeapp.generated.resources.button_confirm
import augmy.composeapp.generated.resources.user_profile_add_to_circle
import augmy.composeapp.generated.resources.user_profile_interact
import augmy.composeapp.generated.resources.user_profile_last_active
import augmy.composeapp.generated.resources.user_profile_not_found
import augmy.interactive.shared.ext.brandShimmerEffect
import augmy.interactive.shared.ui.base.LocalNavController
import augmy.interactive.shared.ui.base.LocalSnackbarHost
import augmy.interactive.shared.ui.components.BrandHeaderButton
import augmy.interactive.shared.ui.components.ComponentHeaderButton
import augmy.interactive.shared.ui.components.MinimalisticComponentIcon
import augmy.interactive.shared.ui.components.dialog.AlertDialog
import augmy.interactive.shared.ui.theme.LocalTheme
import augmy.interactive.shared.ui.theme.SharedColors
import augmy.interactive.shared.utils.DateUtils
import augmy.interactive.shared.utils.DateUtils.formatAsRelative
import base.navigation.NavigationNode
import components.UserProfileImage
import data.NetworkProximityCategory
import data.io.matrix.room.event.ConversationRoomMember
import data.io.user.NetworkItemIO
import net.folivo.trixnity.core.model.events.m.Presence
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.context.loadKoinModules
import org.koin.core.parameter.parametersOf
import ui.account.shareProfile
import ui.conversation.components.ErrorInfoBox
import ui.network.add_new.NetworkAddNewModel
import ui.network.add_new.networkAddNewModule
import ui.network.components.ProximityPicker

@Composable
fun UserDetailDialog(
    userId: String? = null,
    member: ConversationRoomMember? = null,
    networkItem: NetworkItemIO? = null,
    onDismissRequest: () -> Unit
) {
    loadKoinModules(userDetailModule)
    val model: UserDetailModel = koinViewModel(
        parameters = {
            parametersOf(userId ?: member?.userId, networkItem)
        }
    )
    val coroutineScope = rememberCoroutineScope()
    val clipboard = LocalClipboard.current
    val navController = LocalNavController.current
    val snackbarHostState = LocalSnackbarHost.current

    val user = model.response.collectAsState()
    val socialCircleColors = model.socialCircleColors.collectAsState(initial = null)

    AlertDialog(
        intrinsicContent = false,
        additionalContent = {
            AnimatedVisibility(
                modifier = Modifier.align(Alignment.CenterHorizontally),
                visible = user.value.error != null
            ) {
                ErrorInfoBox(
                    modifier = Modifier.fillMaxWidth(.7f),
                    message = stringResource(Res.string.user_profile_not_found)
                )
            }

            AnimatedVisibility(user.value.data?.proximity != null) {
                user.value.data?.proximity?.let { proximity ->
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

            Row(
                modifier = Modifier.padding(top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                UserProfileImage(
                    modifier = Modifier.sizeIn(
                        maxWidth = 125.dp,
                        maxHeight = 125.dp
                    ),
                    media = user.value.data?.avatar,
                    name = user.value.data?.displayName ?: user.value.data?.userId,
                    tag = user.value.data?.tag,
                    animate = user.value.data?.presence?.presence == Presence.ONLINE
                )
                Crossfade(user.value.isLoading) { isLoading ->
                    Text(
                        modifier = Modifier
                            .padding(top = 2.dp)
                            .then(
                                if (isLoading) {
                                    Modifier.fillMaxWidth(.7f).brandShimmerEffect()
                                }else Modifier
                            ),
                        text = buildAnnotatedString {
                            user.value.data?.displayName?.let {
                                withStyle(SpanStyle(fontSize = LocalTheme.current.styles.subheading.fontSize)) {
                                    append(it)
                                }
                            }
                            user.value.data?.userId?.let { userId ->
                                withStyle(SpanStyle(color = LocalTheme.current.colors.disabled)) {
                                    append(" (${userId})")
                                }
                            }
                        },
                        style = LocalTheme.current.styles.regular.copy(
                            color = LocalTheme.current.colors.secondary
                        )
                    )
                }
                MinimalisticComponentIcon(
                    modifier = Modifier.padding(start = 4.dp),
                    imageVector = Icons.Outlined.IosShare,
                    contentDescription = stringResource(Res.string.accessibility_share),
                    onTap = {
                        if (user.value.data != null) {
                            shareProfile(
                                coroutineScope = coroutineScope,
                                publicId = userId,
                                clipboard = clipboard,
                                snackbarHostState = snackbarHostState
                            )
                        }
                    }
                )
            }
            AnimatedVisibility(user.value.data?.presence != null) {
                user.value.data?.presence?.let { presence ->
                    Row(
                        modifier = Modifier
                            .padding(top = 2.dp)
                            .align(Alignment.Start),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        presence.statusMessage.takeIf { !it.isNullOrBlank() } ?: presence.lastActiveAgo?.let {
                            stringResource(
                                Res.string.user_profile_last_active,
                                DateUtils.fromMillis(it).formatAsRelative()
                            )
                        }?.let { statusMessage ->
                            Text(
                                text = statusMessage,
                                style = LocalTheme.current.styles.regular
                            )
                        }

                        val color = when (presence.presence) {
                            Presence.ONLINE -> SharedColors.GREEN_CORRECT
                            Presence.OFFLINE -> SharedColors.RED_ERROR_50
                            // Presence.UNAVAILABLE
                            else -> LocalTheme.current.colors.disabled
                        }
                        Box(
                            modifier = Modifier
                                .size(20.dp)
                                .background(color = color, shape = CircleShape)
                        )
                    }
                }
            }

            AnimatedVisibility(user.value.data != null) {
                user.value.data?.let {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.End)
                    ) {
                        AddToCircleAction(user = it)
                        ComponentHeaderButton(
                            endImageVector = Icons.AutoMirrored.Outlined.Chat,
                            text = stringResource(Res.string.user_profile_interact),
                            onClick = {
                                navController?.navigate(
                                    NavigationNode.Conversation(
                                        userId = it.userId,
                                        name = it.displayName ?: it.userId
                                    )
                                )
                                onDismissRequest()
                            }
                        )
                    }
                }
            }
        },
        onDismissRequest = onDismissRequest
    )
}

@Composable
private fun AddToCircleAction(
    modifier: Modifier = Modifier,
    user: NetworkItemIO
) {
    val selectedCategory = remember {
        mutableStateOf(NetworkProximityCategory.Public)
    }
    val showProximityChoice = remember {
        mutableStateOf(false)
    }

    Column(
        modifier = modifier.wrapContentHeight(),
        horizontalAlignment = Alignment.End
    ) {
        Crossfade(showProximityChoice.value) { proximity ->
            ComponentHeaderButton(
                endImageVector = if (proximity) Icons.Outlined.TrackChanges else Icons.Outlined.Close,
                text = stringResource(
                    if (proximity) Res.string.accessibility_cancel else Res.string.user_profile_add_to_circle
                ),
                onClick = {
                    showProximityChoice.value = !showProximityChoice.value
                }
            )
        }

        androidx.compose.animation.AnimatedVisibility(
            visible = showProximityChoice.value,
            enter = expandVertically() + fadeIn()
        ) {
            loadKoinModules(networkAddNewModule)
            val model = koinViewModel<NetworkAddNewModel>()

            ProximityPicker(
                model = model,
                selectedCategory = selectedCategory,
                newItem = user
            )
        }
        androidx.compose.animation.AnimatedVisibility(
            visible = showProximityChoice.value,
            enter = expandHorizontally() + fadeIn()
        ) {
            BrandHeaderButton(
                modifier = Modifier.padding(top = 4.dp),
                text = stringResource(Res.string.button_confirm),
                onClick = {
                    showProximityChoice.value = false
                }
            )
        }
    }
}
