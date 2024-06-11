package com.squadris.squadris.compose.base

import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.squadris.squadris.compose.theme.LocalTheme
import com.squadris.squadris.compose.theme.SharedColors

/**
 * Themed snackbar host with custom snackbar and possibility to display in error version
 */
@Composable
fun BaseSnackbarHost(
    modifier: Modifier = Modifier,
    hostState: SnackbarHostState
) {
    SnackbarHost(
        modifier = modifier,
        hostState = hostState,
        snackbar = { data ->
            Snackbar(
                data,
                shape = LocalTheme.current.shapes.componentShape,
                containerColor = if((data.visuals as? CustomSnackbarVisuals)?.isError == true) {
                    SharedColors.RED_ERROR
                }else LocalTheme.current.colors.brandMainDark,
                contentColor = if((data.visuals as? CustomSnackbarVisuals)?.isError == true) {
                    Color.White
                }else LocalTheme.current.colors.tetrial,
                actionColor = if((data.visuals as? CustomSnackbarVisuals)?.isError == true) {
                    Color.White
                }else LocalTheme.current.colors.tetrial,
                dismissActionContentColor = if((data.visuals as? CustomSnackbarVisuals)?.isError == true) {
                    Color.White
                }else LocalTheme.current.colors.tetrial,
            )
        }
    )
}