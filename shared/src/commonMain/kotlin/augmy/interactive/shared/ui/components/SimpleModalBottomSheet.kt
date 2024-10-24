package augmy.interactive.shared.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import augmy.interactive.shared.ui.theme.LocalTheme

/**
 * Simple bottom sheet layout
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SimpleModalBottomSheet(
    modifier: Modifier = Modifier,
    onDismissRequest: () -> Unit,
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    windowInsets: @Composable () -> WindowInsets = { WindowInsets.navigationBars },
    content: @Composable ColumnScope.() -> Unit = {}
) {
    // hotfix, native onDismissRequest doesn't work when collapsing by drag
    val previousValue = remember { mutableStateOf(sheetState.currentValue) }
    LaunchedEffect(sheetState.currentValue) {
        if(previousValue.value != SheetValue.Hidden && sheetState.currentValue == SheetValue.Hidden) {
            onDismissRequest()
        }
        previousValue.value = sheetState.currentValue
    }

    ModalBottomSheet(
        modifier = modifier,
        onDismissRequest = onDismissRequest,
        content = {
            Column(
                modifier = Modifier
                    .padding(start = 12.dp, end = 12.dp, bottom = 12.dp)
                    .navigationBarsPadding()
            ) {
                content()
            }
        },
        sheetState = sheetState,
        containerColor = LocalTheme.current.colors.backgroundDark,
        shape = RoundedCornerShape(
            topStart = LocalTheme.current.shapes.componentCornerRadius,
            topEnd = LocalTheme.current.shapes.componentCornerRadius
        ),
        tonalElevation = LocalTheme.current.styles.actionElevation,
        dragHandle = {
            BottomSheetDefaults.DragHandle(color = LocalTheme.current.colors.secondary)
        },
        contentWindowInsets = windowInsets
    )
}