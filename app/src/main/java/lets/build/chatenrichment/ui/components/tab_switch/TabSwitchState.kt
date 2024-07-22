package lets.build.chatenrichment.ui.components.tab_switch

import androidx.compose.foundation.ScrollState
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf

/** State for communication with [MultiChoiceSwitch] */
data class TabSwitchState(
    /** currently selected tab index */
    val selectedTabIndex: MutableState<Int> = mutableIntStateOf(0),

    /** called whenever tab selection is changed */
    val onSelectionChange: (index: Int) -> Unit = {},

    /** list of all tabs that will be displayed */
    val tabs: MutableList<String> = mutableListOf(),

    /** button scroll state */
    val scrollState: ScrollState
)