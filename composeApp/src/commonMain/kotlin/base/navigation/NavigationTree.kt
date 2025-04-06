package base.navigation

import data.io.social.network.conversation.message.MediaIO
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

/** Main holder of all navigation nodes */
sealed class NavigationNode {

    /**
     * Equivalent of route within the navigation.
     * Can be used to check whether current destination matches a specific Node.
     */
    val route
        get() = this::class.qualifiedName

    /**
     * Deeplink is the appendix of a brand link, which is shared between all client apps.
     * Null if deeplink isn't supported
     */
    abstract val deepLink: String?

    /** screen for both login and signup */
    @Serializable
    data class Login(
        val nonce: String? = null,
        val loginToken: String? = null
    ): NavigationNode() {
        override val deepLink: String
            get() {
                val base = "login"
                return when {
                    nonce != null && loginToken != null -> "$base?nonce=$nonce&loginToken=$loginToken"
                    loginToken != null -> "$base?loginToken=$loginToken"
                    nonce != null -> "$base?nonce=$nonce"
                    else -> base
                }
            }
    }

    /** Easter-egg screen, just because */
    @Serializable
    data object Water: NavigationNode() {
        override val deepLink: String? = null
    }

    /** Conversation detail screen */
    @Serializable
    data class Conversation(
        /** unique identifier of the conversation */
        val conversationId: String? = null,

        /** Name of the conversation */
        val name: String? = null
    ): NavigationNode() {
        override val deepLink: String = "messages?conversation=$conversationId&name=$name"
    }

    @Serializable
    data class ConversationSettings(
        /** unique identifier of the conversation */
        val conversationId: String? = null
    ): NavigationNode() {
        override val deepLink: String = "settings?conversation=$conversationId"
    }

    /** Conversation detail screen */
    @Serializable
    data class ConversationInformation(
        /** unique identifier of the conversation */
        val conversationId: String? = null,

        /** Name of the conversation */
        val name: String? = null
    ): NavigationNode() {
        override val deepLink: String = "conversation?conversation=$conversationId&name=$name"
    }

    /** dashboard screen for user information and general user-related actions */
    @Serializable
    data object AccountDashboard: NavigationNode() {
        override val deepLink: String = "account/dashboard"
    }

    /** screen for searching within one's account, preferences, and settings */
    @Serializable
    data object SearchAccount: NavigationNode() {
        override val deepLink: String = "account/search"
    }

    /** Screen for searching within one's network */
    @Serializable
    data object SearchNetwork: NavigationNode() {
        override val deepLink: String = "network/search"
    }

    /** screen for managing social circle of this app, specific to the current user */
    @Serializable
    data class NetworkManagement(
        val displayName: String? = null,
        val tag: String? = null
    ): NavigationNode() {
        override val deepLink: String = "network"
    }

    /** home screen of the whole app */
    @Serializable
    data object Home: NavigationNode() {
        override val deepLink: String? = null
    }

    /** Detail screen of a single message, sort of a focus mode with replies,
     *  reactions and longer and scrollable content */
    @Serializable
    data class MessageDetail(
        val messageId: String? = null,
        val conversationId: String? = null,
        val title: String? = null
    ): NavigationNode() {
        override val deepLink: String? = null
    }

    /** Full screen media detail */
    @Serializable
    data class MediaDetail(
        @Transient
        val media: List<MediaIO> = listOf(),
        val title: String? = null,
        val subtitle: String? = null,
        val selectedIndex: Int = 0,
        private val encodedMedia: List<String> = media.map { "${it.name}|||${it.mimetype}|||${it.url}|||${it.path}" }
    ): NavigationNode() {
        override val deepLink: String? = null
    }

    companion object {
        val allNodes = listOf(
            Login(),
            Water,
            Conversation(),
            AccountDashboard,
            NetworkManagement(),
            MediaDetail(),
            MessageDetail()
        )
    }
}