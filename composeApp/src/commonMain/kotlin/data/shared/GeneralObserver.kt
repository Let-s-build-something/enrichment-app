package data.shared

import data.io.social.network.conversation.message.FullConversationMessage
import data.io.social.network.conversation.message.MessageReactionIO
import ui.conversation.ConversationState

sealed class GeneralObserver<D> {
    abstract fun invoke(data: D)

    class MessageObserver(
        private val invocation: (FullConversationMessage) -> Unit
    ): GeneralObserver<FullConversationMessage>() {
        override fun invoke(data: FullConversationMessage) = invocation(data)
    }

    class ReactionsObserver(
        private val invocation: (MessageReactionIO) -> Unit
    ): GeneralObserver<MessageReactionIO>() {
        override fun invoke(data: MessageReactionIO) = invocation(data)
    }

    class ConversationStateObserver(
        private val invocation: (ConversationState) -> Unit
    ): GeneralObserver<ConversationState>() {
        override fun invoke(data: ConversationState) = invocation(data)
    }
}