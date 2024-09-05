package base.navigation

import kotlinx.serialization.Serializable

sealed class NavigationNode {

    /**
     * Equivalent of route within the navigation.
     * Can be used to check whether current destination matches a specific Node.
     */
    val route
        get() = this::class.qualifiedName

    @Serializable
    data object Login: NavigationNode()

    /** Easter-egg screen, just because */
    @Serializable
    data object Water: NavigationNode()

    @Serializable
    data object AccountDashboard: NavigationNode()

    @Serializable
    data object Home: NavigationNode()
}