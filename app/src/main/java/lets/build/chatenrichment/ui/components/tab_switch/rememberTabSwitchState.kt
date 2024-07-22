package lets.build.chatenrichment.ui.components.tab_switch

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope

/**
 * Remembers the current state
 * @param selectedTabIndex currently selected tab index
 * @param onSelectionChange called whenever tab selection is changed
 * @param tabs list of all tabs that will be displayed
 */
@Composable
fun rememberTabSwitchState(
    selectedTabIndex: Int = 0,
    onSelectionChange: (index: Int) -> Unit = { },
    tabs: MutableList<String> = mutableListOf(),
    scrollState: ScrollState = rememberScrollState()
): TabSwitchState {
    val scope = rememberCoroutineScope()
    val state = remember(scope) {
        TabSwitchState(
            selectedTabIndex = mutableIntStateOf(selectedTabIndex),
            onSelectionChange = onSelectionChange,
            tabs = tabs,
            scrollState = scrollState
        )
    }
    return state
}