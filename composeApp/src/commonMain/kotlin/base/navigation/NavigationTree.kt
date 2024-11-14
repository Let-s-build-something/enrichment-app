package base.navigation

import kotlinx.serialization.Serializable

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
    data object Login: NavigationNode() {
        override val deepLink: String = "login"
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
        val conversationUid: String? = null,

        /** unique identifier of the recipient user */
        val userUid: String? = null
    ): NavigationNode() {
        override val deepLink: String = "messages?conversation=$conversationUid&user=$userUid"
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
        override val deepLink: String = ""
    }

    companion object {
        val allNodes = listOf(
            Login,
            Water,
            Conversation(),
            AccountDashboard,
            NetworkManagement()
        )
    }
}