package ui.home

import androidx.lifecycle.viewModelScope
import com.russhwolf.settings.set
import components.pull_refresh.RefreshableViewModel
import data.NetworkProximityCategory
import data.io.app.SettingsKeys.KEY_NETWORK_CATEGORIES
import data.io.user.NetworkItemIO
import data.shared.SharedViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module
import ui.home.DemoData.proximityDemoData
import ui.network.list.NetworkListRepository.Companion.demoData
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

internal val homeModule = module {
    single { HomeDataManager() }
    factory { HomeViewModel(get()) }
    viewModelOf(::HomeViewModel)
}

/** Communication between the UI, the control layers, and control and data layers */
class HomeViewModel(
    private val dataManger: HomeDataManager
): SharedViewModel(), RefreshableViewModel {

    override val isRefreshing = MutableStateFlow(false)
    override var lastRefreshTimeMillis = 0L

    override suspend fun onDataRequest(isSpecial: Boolean, isPullRefresh: Boolean) {}

    /** Last selected network categories */
    var defaultChoices = settings.getStringOrNull(KEY_NETWORK_CATEGORIES)
        ?.split(",")
        ?.mapNotNull {
            NetworkProximityCategory.entries.firstOrNull { category -> category.name == it }
        }
        private set

    init {
        filterNetworkItems(categories = defaultChoices)
    }

    /** filtered currently downloaded network items */
    val networkItems = dataManger.networkItems.asStateFlow()

    /** Filters currently downloaded network items */
    fun filterNetworkItems(
        categories: List<NetworkProximityCategory>? = null,
        query: String? = null
    ) {
        viewModelScope.launch(Dispatchers.Default) {
            dataManger.networkItems.value = proximityDemoData
                .filter { data ->
                    categories?.any { it.range.contains(data.proximity ?: 1f) } == true
                            && (query == null || data.displayName?.contains(query, ignoreCase = true) == true)
                }
                .sortedByDescending {
                    it.proximity
                }

            defaultChoices = categories
            settings[KEY_NETWORK_CATEGORIES] = categories?.joinToString(",")
        }
    }
}

private object DemoData {
    private val family = listOf(
        NetworkItemIO(proximity = 10.1f, displayName = "Dad", photoUrl = "https://picsum.photos/100", tag = "2098d6", publicId = "20l98d6"),
        NetworkItemIO(proximity = 10.7f, displayName = "Mom", photoUrl = "https://picsum.photos/101", tag = "2098d6", publicId = "2098d6d"),
        NetworkItemIO(proximity = 10.9f, displayName = "Sister", photoUrl = "https://picsum.photos/102", tag = "2098d6", publicId = "2098dc6d"),
        NetworkItemIO(proximity = 10.4f, displayName = "Brother", photoUrl = "https://picsum.photos/103", tag = "2098d6", publicId = "2098db6d"),
        NetworkItemIO(proximity = 10.9f, displayName = "Son", photoUrl = "https://picsum.photos/104", tag = "2098d6", publicId = "2098d6ed"),
        NetworkItemIO(proximity = 10.2f, displayName = "Grandma", photoUrl = "https://picsum.photos/105", tag = "2098d6", publicId = "2098dg6d"),
        NetworkItemIO(proximity = 10.1f, displayName = "Grandpa", photoUrl = "https://picsum.photos/106", tag = "2098d6", publicId = "2098d6sd")
    )

    private val friends = listOf(
        NetworkItemIO(proximity = 9.9f, displayName = "Jack", photoUrl = "https://picsum.photos/107", tag = "2098d6", publicId = "20f98d6"),
        NetworkItemIO(proximity = 9.3f, displayName = "Peter", photoUrl = "https://picsum.photos/108", tag = "2098d6", publicId = "2098df6dl"),
        NetworkItemIO(proximity = 9.2f, displayName = "James", photoUrl = "https://picsum.photos/109", tag = "2098d6", publicId = "20l98fdc6d"),
        NetworkItemIO(proximity = 9.6f, displayName = "Mark", photoUrl = "https://picsum.photos/110", tag = "2098d6", publicId = "20f98dbl6d"),
        NetworkItemIO(proximity = 9.8f, displayName = "Carl", photoUrl = "https://picsum.photos/111", tag = "2098d6", publicId = "209l8d6efd"),
        NetworkItemIO(proximity = 9.1f, displayName = "Arnold", photoUrl = "https://picsum.photos/112", tag = "2098d6", publicId = "2098ldfg6d"),
    )

    private val acquaintances = listOf(
        NetworkItemIO(proximity = 8.5f, displayName = "Jack", photoUrl = "https://picsum.photos/113", tag = "2098d6", publicId = "2098sd6"),
        NetworkItemIO(proximity = 8.3f, displayName = "Peter", photoUrl = "https://picsum.photos/114", tag = "2098d6", publicId = "209s8d6dl"),
        NetworkItemIO(proximity = 8.77f, displayName = "James", photoUrl = "https://picsum.photos/115", tag = "2098d6", publicId = "s20l98dc6d"),
        NetworkItemIO(proximity = 8.7f, displayName = "Mark", photoUrl = "https://picsum.photos/116", tag = "2098d6", publicId = "20s98dbl6d"),
        NetworkItemIO(proximity = 8.8f, displayName = "Carl", photoUrl = "https://picsum.photos/117", tag = "2098d6", publicId = "209l8d6eds"),
        NetworkItemIO(proximity = 8.2f, displayName = "Arnold", photoUrl = "https://picsum.photos/118", tag = "2098d6", publicId = "2098ldg6sd"),
    )

    @OptIn(ExperimentalUuidApi::class)
    private val community = demoData.map { it.copy(proximity = (40..70).random().div(10f), publicId = Uuid.random().toString()) }
    private val strangers = demoData

    val proximityDemoData = family + friends + acquaintances + community + strangers
}
