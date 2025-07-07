package ui.search.room

import androidx.compose.runtime.Composable
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.context.loadKoinModules

@Composable
fun SearchRoomScreen() {
    loadKoinModules(searchRoomModule)
    val model = koinViewModel<SearchRoomModel>()


}
