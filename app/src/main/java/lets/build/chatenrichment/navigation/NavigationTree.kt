package lets.build.chatenrichment.navigation

import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.serializer

/** Main navigation Component */
@Serializable
sealed class NavigationTree {

    /** navigation identifier */
    val id: Int
        @OptIn(InternalSerializationApi::class)
        get() = this::class.serializer().hashCode()

    /** home page of the application with shorter information span with higher variety */
    @Serializable
    data object Home: NavigationTree()

    /** login and signup screen */
    @Serializable
    data object Login : NavigationTree()

    /** login and signup screen via email and password */
    @Serializable
    data object LoginPassword : NavigationTree()

    /** screen with general navigation and information about the user */
    @Serializable
    data object AccountDashboard: NavigationTree()
}