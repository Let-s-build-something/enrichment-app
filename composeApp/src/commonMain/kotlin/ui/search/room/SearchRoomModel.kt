package ui.search.room

import androidx.lifecycle.viewModelScope
import data.shared.SharedModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import net.folivo.trixnity.clientserverapi.model.rooms.GetPublicRoomsResponse
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

internal val searchRoomModule = module {
    factory { SearchRoomRepository(get()) }
    viewModelOf(::SearchRoomModel)
}

/** Communication between the UI, the control layers, and control and data layers */
class SearchRoomModel(
    private val repository: SearchRoomRepository
): SharedModel() {

    companion object {
        const val ITEMS_COUNT = 10
    }

    //TODO pagination
    private val _rooms = MutableStateFlow<List<GetPublicRoomsResponse.PublicRoomsChunk>?>(null)

    val rooms = _rooms.asStateFlow()

    fun queryRooms(prompt: CharSequence, homeserver: String) {
        if(prompt.isBlank()) return

        viewModelScope.launch(Dispatchers.Default) {
            _rooms.value = repository.queryRooms(
                limit = ITEMS_COUNT,
                homeserver = this@SearchRoomModel.homeserver,
                query = prompt.toString(),
                queryHomeserver = homeserver
            ).data?.let { res ->
                res.chunk
            }
        }
    }

    fun joinRoom(room: GetPublicRoomsResponse.PublicRoomsChunk) {
        viewModelScope.launch {
            //TODO
        }
    }
}
