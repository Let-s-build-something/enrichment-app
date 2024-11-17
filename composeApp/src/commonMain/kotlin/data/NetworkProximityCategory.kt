package data

import androidx.compose.ui.graphics.Color
import augmy.composeapp.generated.resources.Res
import augmy.composeapp.generated.resources.category_community
import augmy.composeapp.generated.resources.category_contacts
import augmy.composeapp.generated.resources.category_family
import augmy.composeapp.generated.resources.category_peers
import augmy.composeapp.generated.resources.category_public
import org.jetbrains.compose.resources.StringResource

/** Local categories of proximities */
enum class NetworkProximityCategory(
    val range: ClosedFloatingPointRange<Float>,
    val res: StringResource,
    val color: Color,
    val share: Float,
    val shareOverlay: Float
) {

    /**
     * Family: The closest, most intimate circle. These are the people with whom you have
     * the strongest bonds and most frequent interactions.
     */
    Family(
        range = 10f..10.9f,
        res = Res.string.category_family,
        color = Color(0xFFB57057),
        shareOverlay = .3164f,
        share = .3164f
    ),

    /**
     * Peers: Close friends and people with whom you share a similar social or professional
     * standing. These are individuals you interact with regularly and consider trusted confidants.
     */
    Peers(
        range = 8f..9.9f,
        res = Res.string.category_peers,
        color = Color(0xFFC7BA6F), //C68E6F
        shareOverlay = .5f,
        share = .1836f
    ),

    /**
     * Community: People you engage with within shared social, cultural, or professional
     * spaces, such as coworkers, neighbors, or acquaintances from groups you’re part of.
     */
    Community(
        range = 5f..7.9f,
        res = Res.string.category_community,
        color = Color(0xFFD5A497), // D6C598
        shareOverlay = .707f,
        share = .207f
    ),

    /**
     * Contacts: People you know but do not interact with frequently. This group includes
     * acquaintances, distant friends, or people you interact with on a more transactional basis
     * (e.g., work or events).
     */
    Contacts(
        range = 3f..4.9f,
        res = Res.string.category_contacts,
        color = Color(0xFF7BA6B2),
        shareOverlay = .866f,
        share = .159f
    ),

    /**
     * Public: The most distant group, consisting of people you recognize or may interact with
     * infrequently. This group includes strangers or individuals you may see occasionally in
     * public spaces, but don't have a personal relationship with.
     */
    Public(
        range = 1f..2.9f,
        res = Res.string.category_public,
        color = Color(0xFFA3CCE6),
        shareOverlay = 1f,
        share = .134f
    )
}
