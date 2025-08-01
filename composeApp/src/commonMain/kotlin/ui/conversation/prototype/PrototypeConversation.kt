package ui.conversation.prototype

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.cash.paging.compose.collectAsLazyPagingItems
import augmy.interactive.shared.ui.base.LocalDeviceType
import augmy.interactive.shared.ui.theme.LocalTheme
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.context.loadKoinModules
import org.koin.core.parameter.parametersOf
import ui.conversation.ConversationComponent

private const val MESSAGES_SHIMMER_ITEM_COUNT = 8

@Composable
fun PrototypeConversation(
    modifier: Modifier = Modifier,
    conversationId: String?
) {
    loadKoinModules(prototypeConversationModule)
    val viewModel: PrototypeConversationModel = koinViewModel(
        key = "prototypeConversation",
        parameters = {
            parametersOf(conversationId ?: "")
        }
    )

    val messages = viewModel.messages.collectAsLazyPagingItems()

    ConversationComponent(
        modifier = modifier.fillMaxSize(),
        listModifier = Modifier
            .padding(
                horizontal = if(LocalDeviceType.current == WindowWidthSizeClass.Compact) 0.dp else 16.dp
            )
            .fillMaxSize(),
        verticalArrangement = Arrangement.Top,
        shimmerItemCount = MESSAGES_SHIMMER_ITEM_COUNT,
        conversationId = conversationId,
        model = viewModel,
        messages = messages,
        lazyScope = {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            color = LocalTheme.current.colors.backgroundDark,
                            shape = RoundedCornerShape(
                                bottomEnd = LocalTheme.current.shapes.screenCornerRadius,
                                bottomStart = LocalTheme.current.shapes.screenCornerRadius
                            )
                        )
                        .padding(vertical = 16.dp, horizontal = 12.dp)
                ) {

                }
            }
        }
    )
}