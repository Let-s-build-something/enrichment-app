package ui.network.components

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.lifecycle.viewModelScope
import augmy.composeapp.generated.resources.Res
import augmy.composeapp.generated.resources.user_profile_last_active
import augmy.interactive.shared.ui.components.dialog.AlertDialog
import augmy.interactive.shared.ui.theme.LocalTheme
import augmy.interactive.shared.ui.theme.SharedColors
import augmy.interactive.shared.utils.DateUtils
import augmy.interactive.shared.utils.DateUtils.formatAsRelative
import base.utils.tagToColor
import components.UserProfileImage
import data.NetworkProximityCategory
import data.io.matrix.room.event.ConversationRoomMember
import data.io.user.NetworkItemIO
import data.shared.SharedModel
import database.dao.matrix.PresenceEventDao
import database.dao.matrix.RoomMemberDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.folivo.trixnity.core.model.events.m.Presence
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.context.loadKoinModules
import org.koin.core.module.dsl.viewModel
import org.koin.core.parameter.parametersOf
import org.koin.dsl.module

private val userDetailModule = module {
    factory { UserDetailRepository(get(), get()) }
    factory { (userId: String?, itemIO: NetworkItemIO?) ->
        UserDetailModel(userId, itemIO, get())
    }
    viewModel { (userId: String?, itemIO: NetworkItemIO?) ->
        UserDetailModel(userId, itemIO, get())
    }
}

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

    val user = model.user.collectAsState()
    val socialCircleColors = model.socialCircleColors.collectAsState(initial = null)

    AlertDialog(
        additionalContent = {
            user.value?.proximity?.let { proximity ->
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

            Row(
                modifier = Modifier.padding(top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                UserProfileImage(
                    modifier = Modifier.sizeIn(
                        maxWidth = 125.dp,
                        maxHeight = 125.dp
                    ),
                    media = user.value?.avatar,
                    name = user.value?.displayName ?: user.value?.userId,
                    tag = user.value?.tag,
                    animate = user.value?.presence?.presence == Presence.ONLINE
                )
                Column(modifier = Modifier.fillMaxWidth(.8f)) {
                    Text(
                        modifier = Modifier
                            .padding(top = 2.dp)
                            .align(Alignment.CenterHorizontally),
                        text = buildAnnotatedString {
                            user.value?.displayName?.let {
                                withStyle(SpanStyle(fontSize = LocalTheme.current.styles.subheading.fontSize)) {
                                    append(it)
                                }
                            }
                            user.value?.userId?.let { userId ->
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
            }
            user.value?.presence?.let { presence ->
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
        },
        onDismissRequest = onDismissRequest
    )
}

class UserDetailModel(
    userId: String?,
    networkItem: NetworkItemIO?,
    private val repository: UserDetailRepository
): SharedModel() {
    private val _user = MutableStateFlow<NetworkItemIO?>(null)
    val user = _user.asStateFlow()

    /** Customized social circle colors */
    val socialCircleColors: Flow<Map<NetworkProximityCategory, Color>> = localSettings.map { settings ->
        withContext(Dispatchers.Default) {
            settings?.networkColors?.mapIndexedNotNull { index, s ->
                tagToColor(s)?.let { color ->
                    NetworkProximityCategory.entries[index] to color
                }
            }.orEmpty().toMap()
        }
    }

    init {
        if (userId != null) getUser(userId) else if (networkItem != null) {
            _user.value = networkItem
        }
    }

    private fun getUser(userId: String) {
        viewModelScope.launch {
            _user.value = repository.getUser(userId)
        }
    }
}

class UserDetailRepository(
    private val roomMemberDao: RoomMemberDao,
    private val presenceEventDao: PresenceEventDao
) {

    suspend fun getUser(
        userId: String
    ): NetworkItemIO? = withContext(Dispatchers.IO) {
        roomMemberDao.get(userId = userId)?.toNetworkItem()?.copy(
            presence = presenceEventDao.get(userId)?.content
        )
    }
}
