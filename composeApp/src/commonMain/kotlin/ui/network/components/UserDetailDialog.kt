package ui.network.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewModelScope
import augmy.composeapp.generated.resources.Res
import augmy.composeapp.generated.resources.user_profile_last_active
import augmy.interactive.shared.ui.components.dialog.AlertDialog
import augmy.interactive.shared.ui.theme.LocalTheme
import augmy.interactive.shared.ui.theme.SharedColors
import augmy.interactive.shared.utils.DateUtils
import augmy.interactive.shared.utils.DateUtils.formatAsRelative
import components.UserProfileImage
import data.io.matrix.room.event.ConversationRoomMember
import data.io.user.NetworkItemIO
import data.shared.SharedModel
import database.dao.NetworkItemDao
import database.dao.matrix.PresenceEventDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.folivo.trixnity.core.model.events.m.Presence
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.context.loadKoinModules
import org.koin.core.module.dsl.viewModelOf
import org.koin.core.parameter.parametersOf
import org.koin.dsl.module

private val userDetailModule = module {
    factory { UserDetailRepository(get(), get()) }
    factory { UserDetailModel(get(), get(), get()) }
    viewModelOf(::UserDetailModel)
}

@Composable
fun UserDetailDialog(
    member: ConversationRoomMember? = null,
    networkItem: NetworkItemIO? = null,
    onDismissRequest: () -> Unit
) {
    loadKoinModules(userDetailModule)
    val model: UserDetailModel = koinViewModel(
        parameters = {
            parametersOf(member?.userId, networkItem)
        }
    )

    val user = model.user.collectAsState()

    AlertDialog(
        additionalContent = {
            UserProfileImage(
                media = user.value?.avatar,
                name = user.value?.displayName ?: user.value?.userId,
                tag = user.value?.tag,
                animate = user.value?.presence?.presence == Presence.ONLINE
            )
            Text(
                modifier = Modifier
                    .padding(top = 2.dp)
                    .align(Alignment.CenterHorizontally),
                text = buildAnnotatedString {
                    user.value?.displayName?.let { append(it) }
                    withStyle(SpanStyle(color = LocalTheme.current.colors.disabled)) {
                        append(" (${user.value?.userId ?: ""})")
                    }
                },
                style = LocalTheme.current.styles.regular
            )

            user.value?.presence?.let { presence ->
                Row(
                    modifier = Modifier
                        .padding(top = 2.dp)
                        .align(Alignment.CenterHorizontally),
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
            user.value?.proximity
            user.value?.lastMessage //?
        },
        onDismissRequest = onDismissRequest
    )
}

class UserDetailModel(
    private val repository: UserDetailRepository,
    userId: String?,
    networkItem: NetworkItemIO?
): SharedModel() {
    private val _user = MutableStateFlow<NetworkItemIO?>(null)
    val user = _user.asStateFlow()

    init {
        if (userId != null) getUser(userId) else if (networkItem != null) {
            _user.value = networkItem
        }
    }

    private fun getUser(userId: String) {
        viewModelScope.launch {
            _user.value = repository.getUser(userId, matrixUserId)
        }
    }
}

class UserDetailRepository(
    private val networkItemDao: NetworkItemDao,
    private val presenceEventDao: PresenceEventDao
) {

    suspend fun getUser(
        userId: String,
        ownerPublicId: String?
    ): NetworkItemIO? = withContext(Dispatchers.IO) {
        networkItemDao.get(userId = userId, ownerPublicId = ownerPublicId)?.copy(
            presence = presenceEventDao.get(userId)?.content
        )
    }
}
