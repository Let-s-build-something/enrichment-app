package lets.build.chatenrichment.navigation

import kotlinx.serialization.Serializable

/** Main navigation Component */
sealed class NavigationComponent {

    @Serializable
    data object Home: NavigationComponent()
}