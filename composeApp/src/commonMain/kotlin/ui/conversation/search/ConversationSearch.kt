package ui.conversation.search

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.cash.paging.compose.collectAsLazyPagingItems
import augmy.composeapp.generated.resources.Res
import augmy.composeapp.generated.resources.accessibility_search_down
import augmy.composeapp.generated.resources.accessibility_search_up
import augmy.composeapp.generated.resources.button_search
import augmy.composeapp.generated.resources.screen_conversation_search
import augmy.interactive.shared.ext.scalingClickable
import augmy.interactive.shared.ui.components.input.CustomTextField
import augmy.interactive.shared.ui.theme.LocalTheme
import base.BrandBaseScreen
import base.utils.extractSnippetAroundHighlight
import base.utils.getOrNull
import components.network.NetworkItemRow
import data.io.user.NetworkItemIO
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.context.loadKoinModules
import org.koin.core.parameter.parametersOf


@Composable
fun ConversationSearchScreen(
    conversationId: String?
) {
    loadKoinModules(conversationSearchModule)
    val model: ConversationSearchModel = koinViewModel(
        key = conversationId,
        parameters = {
            parametersOf(conversationId)
        }
    )

    val messages = model.messages.collectAsLazyPagingItems()
    val searchFieldState = remember { TextFieldState() }
    val highlight = searchFieldState.text.toString().lowercase()


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
                    highlightTitle = false,
                    data = NetworkItemIO(
                        userId = message?.author?.userId,
                        displayName = message?.author?.displayName,
                        avatarUrl = message?.author?.avatarUrl,
                        lastMessage = if (highlight.isNotBlank()) {
                            extractSnippetAroundHighlight(message?.message?.content, highlight)
                        }else message?.message?.content
                    ),
                    highlight = highlight
                )
            }
        }
    }
}

@Composable
private fun SearchBar(
    model: ConversationSearchModel,
    searchFieldState: TextFieldState
) {
    val density = LocalDensity.current
    val focusRequester = remember { FocusRequester() }

    val searchResultMeta = model.searchResultMeta.collectAsState()

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

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
            isClearable = true,
            focusRequester = focusRequester,
            hint = stringResource(Res.string.button_search),
            state = searchFieldState,
            showBorders = false,
            lineLimits = TextFieldLineLimits.SingleLine,
            shape = LocalTheme.current.shapes.rectangularActionShape
        )

        AnimatedVisibility(searchResultMeta.value != null) {
            searchResultMeta.value?.let { meta ->
                Row(
                    modifier = Modifier
                        .background(
                            color = LocalTheme.current.colors.backgroundDark,
                            shape = LocalTheme.current.shapes.rectangularActionShape
                        )
                        .padding(horizontal = 4.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        modifier = Modifier.padding(start = 8.dp),
                        text = "${meta.position}/${meta.size}",
                        style = LocalTheme.current.styles.regular
                    )
                    Icon(   // move up
                        modifier = Modifier
                            .size(with(density) { 38.sp.toDp() })
                            .scalingClickable {
                                model.scrollInMeta(1)
                            }
                            .padding(2.dp),
                        imageVector = Icons.Outlined.KeyboardArrowUp,
                        contentDescription = stringResource(Res.string.accessibility_search_up),
                        tint = LocalTheme.current.colors.secondary
                    )
                    Icon(   // move down
                        modifier = Modifier
                            .size(with(density) { 38.sp.toDp() })
                            .scalingClickable {
                                model.scrollInMeta(-1)
                            }
                            .padding(2.dp),
                        imageVector = Icons.Outlined.KeyboardArrowDown,
                        contentDescription = stringResource(Res.string.accessibility_search_down),
                        tint = LocalTheme.current.colors.secondary
                    )
                }
            }
        }
    }
}
