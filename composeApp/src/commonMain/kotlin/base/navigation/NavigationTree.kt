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

    /** screen for both login and signup */
    @Serializable
    data object Login: NavigationNode()

    /** Easter-egg screen, just because */
    @Serializable
    data object Water: NavigationNode()

    /** dashboard screen for user information and general user-related actions */
    @Serializable
    data object AccountDashboard: NavigationNode()

    /** home screen of the whole app */
    @Serializable
    data object Home: NavigationNode()
}