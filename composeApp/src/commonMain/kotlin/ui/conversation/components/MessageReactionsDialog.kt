package ui.conversation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Functions
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import augmy.composeapp.generated.resources.Res
import augmy.composeapp.generated.resources.accessibility_reactions_all
import augmy.interactive.shared.ui.components.MultiChoiceSwitchMinimalistic
import augmy.interactive.shared.ui.components.dialog.AlertDialog
import augmy.interactive.shared.ui.components.rememberMultiChoiceState
import augmy.interactive.shared.ui.theme.LocalTheme
import data.io.social.network.conversation.MessageReactionIO
import data.io.user.NetworkItemIO
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.compose.resources.stringResource

/** Dialog displaying categorized reactions of a singular message */
@Composable
fun MessageReactionsDialog(
    reactions: MutableState<List<Pair<String?, Pair<List<NetworkItemIO>, Boolean>>>>,
    reactionsRaw: List<MessageReactionIO>,
    messageContent: String?,
    initialEmojiSelection: String?,
    users: List<NetworkItemIO>,
    onDismissRequest: () -> Unit
) {
    val density = LocalDensity.current
    val coroutineScope = rememberCoroutineScope()

    val selectedIndex = remember {
        mutableStateOf(reactions.value.indexOfFirst { it.first == initialEmojiSelection } + 1)
    }
    val tabs = remember {
        reactions.value.mapNotNull { it.first }.toMutableList().apply {
            add(0, TabEmojisAll)
        }
    }

    val pagerState = rememberPagerState(
        initialPage = selectedIndex.value,
        pageCount = { tabs.size }
    )
    val switchState = rememberMultiChoiceState(
        selectedTabIndex = selectedIndex,
        onSelectionChange = {},
        tabs = tabs
    )

    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.currentPage }.collect {
            switchState.selectedTabIndex.value = it
        }
    }

    AlertDialog(
        additionalContent = {
            Column(
                modifier = Modifier
                    .fillMaxWidth(.8f)
                    .fillMaxHeight(.5f),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    modifier = Modifier
                        .padding(bottom = 24.dp)
                        .background(
                            color = LocalTheme.current.colors.component,
                            shape = RoundedCornerShape(24.dp)
                        )
                        .padding(
                            vertical = 10.dp,
                            horizontal = 14.dp
                        ),
                    text = messageContent ?: "",
                    style = LocalTheme.current.styles.category
                )
                if(tabs.size > 2) {
                    MultiChoiceSwitchMinimalistic(
                        modifier = Modifier.fillMaxWidth(),
                        state = switchState,
                        onClick = {
                            coroutineScope.launch {
                                pagerState.animateScrollToPage(it)
                            }
                        },
                        onItemCreation = { _, index, _ ->
                            if(index == 0) {
                                Icon(
                                    modifier = Modifier
                                        .padding(vertical = 6.dp)
                                        .fillMaxWidth()
                                        .size(
                                            with(density) { LocalTheme.current.styles.subheading.fontSize.toDp() }
                                        ),
                                    imageVector = Icons.Outlined.Functions,
                                    contentDescription = stringResource(Res.string.accessibility_reactions_all),
                                    tint = LocalTheme.current.colors.secondary
                                )
                            }else {
                                Text(
                                    modifier = Modifier
                                        .padding(vertical = 6.dp)
                                        .fillMaxWidth(),
                                    text = tabs.getOrNull(index) ?: "",
                                    style = LocalTheme.current.styles.subheading.copy(
                                        color = LocalTheme.current.colors.secondary,
                                        textAlign = TextAlign.Center
                                    )
                                )
                            }
                        }
                    )
                }
                HorizontalPager(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    state = pagerState,
                    beyondViewportPageCount = 1
                ) { index ->
                    val list = remember(index, reactions.value) {
                        mutableStateOf(listOf<Pair<String?, NetworkItemIO?>>())
                    }

                    LaunchedEffect(index, reactions.value) {
                        withContext(Dispatchers.Default) {
                            list.value = if (index == 0) {
                                reactionsRaw.map { reaction ->
                                    reaction.content to users.find { it.publicId == reaction.authorPublicId }
                                }
                            } else {
                                reactions.value.find { reaction -> reaction.first == tabs[index] }?.second?.first.orEmpty().map {
                                    tabs[index] to it
                                }
                            }
                        }
                    }

                    LazyColumn(
                        modifier = Modifier
                            .padding(top = 16.dp)
                            .fillMaxHeight(),
                        verticalArrangement = Arrangement.spacedBy(
                            space = LocalTheme.current.shapes.betweenItemsSpace,
                            alignment = Alignment.Top
                        )
                    ) {
                        items(list.value) { reaction ->
                            Row(
                                modifier = Modifier
                                    .padding(start = 4.dp)
                                    .fillMaxWidth()
                                    .animateItem(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    modifier = Modifier.padding(4.dp),
                                    text = reaction.first ?: "",
                                    style = LocalTheme.current.styles.category,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    modifier = Modifier.padding(start = 6.dp),
                                    text = reaction.second?.displayName ?: "",
                                    style = LocalTheme.current.styles.category
                                )
                            }
                        }
                    }
                }
            }

        },
        onDismissRequest = onDismissRequest
    )
}

private const val TabEmojisAll = "ALL"