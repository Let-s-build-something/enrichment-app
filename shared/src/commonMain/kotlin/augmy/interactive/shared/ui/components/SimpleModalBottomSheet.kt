package augmy.interactive.shared.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.SheetValue
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import augmy.interactive.shared.ui.theme.LocalTheme
import kotlinx.coroutines.flow.collectLatest

/**
 * Simple bottom sheet layout
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SimpleModalBottomSheet(
    modifier: Modifier = Modifier,
    onDismissRequest: () -> Unit,
    contentPadding: PaddingValues = PaddingValues(
        start = 12.dp, end = 12.dp, bottom = 12.dp
    ),
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    windowInsets: @Composable () -> WindowInsets = { WindowInsets.navigationBars },
    dragHandle: @Composable (() -> Unit)? = {
        BottomSheetDefaults.DragHandle(color = LocalTheme.current.colors.secondary)
    },
    scrollEnabled: Boolean = true,
    verticalArrangement: Arrangement.Vertical = Arrangement.Top,
    horizontalAlignment: Alignment.Horizontal = Alignment.Start,
    content: @Composable ColumnScope.() -> Unit = {}
) {

    // hotfix, native onDismissRequest doesn't work when collapsing by drag
    val previousValue = remember { mutableStateOf(sheetState.currentValue) }

    LaunchedEffect(Unit) {
        sheetState.expand()
    }

    LaunchedEffect(sheetState) {
        snapshotFlow { sheetState.currentValue }.collectLatest { currentState ->
            if(previousValue.value != SheetValue.Hidden && currentState == SheetValue.Hidden) {
                onDismissRequest()
            }
            previousValue.value = currentState
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        content = {
            Column(
                modifier = modifier
                    .padding(contentPadding)
                    .navigationBarsPadding()
                    .then(
                        if(scrollEnabled) {
                            Modifier.verticalScroll(rememberScrollState())
                        }else Modifier
                    ),
                verticalArrangement = verticalArrangement,
                horizontalAlignment = horizontalAlignment
            ) {
                content()
            }
        },
        sheetState = sheetState,
        containerColor = LocalTheme.current.colors.backgroundLight,
        shape = RoundedCornerShape(
            topStart = LocalTheme.current.shapes.componentCornerRadius,
            topEnd = LocalTheme.current.shapes.componentCornerRadius
        ),
        tonalElevation = LocalTheme.current.styles.actionElevation,
        dragHandle = dragHandle,
        contentWindowInsets = windowInsets
    )
}