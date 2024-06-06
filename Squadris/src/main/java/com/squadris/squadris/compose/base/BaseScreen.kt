package com.squadris.squadris.compose.base

import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring.StiffnessVeryLow
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FabPosition
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarVisuals
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.squadris.squadris.compose.components.collapsing_layout.CollapsingBehavior
import com.squadris.squadris.compose.components.collapsing_layout.CollapsingLayout
import com.squadris.squadris.compose.components.collapsing_layout.CollapsingLayoutState
import com.squadris.squadris.compose.components.collapsing_layout.rememberCollapsingLayout
import com.squadris.squadris.compose.components.navigation.CustomizableAppBar
import com.squadris.squadris.compose.theme.Colors
import com.squadris.squadris.compose.theme.LocalTheme

/** current navigation tree and controller */
val LocalNavController = staticCompositionLocalOf<NavController?> { null }

/** current snackbar host for showing snackbars */
val LocalSnackbarHost = staticCompositionLocalOf<SnackbarHostState?> { null }

/** whether device is a tablet or not */
val LocalIsTablet = staticCompositionLocalOf { false }

/** Currently displayed, hosting activity */
val LocalActivity = staticCompositionLocalOf<Activity?> { null }

/**
 * Most basic all-in-one implementation of a screen with action bar, without bottom bar
 * @param navigationIcon what type of navigation icon screen should have
 * @param title capital title of the screen
 * @param subtitle lower case subtitle of the screen
 * @param actionIcons right side actions to be displayed
 * @param content screen content under the action bar
 */
@Composable
fun BaseScreen(
    modifier: Modifier = Modifier,
    contentModifier: Modifier = Modifier,
    collapsingLayoutState: CollapsingLayoutState = rememberCollapsingLayout(),
    navigationIcon: Pair<ImageVector, String>? = null,
    title: String? = null,
    subtitle: String? = null,
    onBackPressed: () -> Boolean = { true },
    actionIcons: @Composable RowScope.() -> Unit = {},
    appBarVisible: Boolean = true,
    onNavigationIconClick: (() -> Unit)? = null,
    containerColor: Color? = null,
    contentColor: Color = Color.Transparent,
    floatingActionButtonPosition: FabPosition = FabPosition.End,
    floatingActionButton: @Composable () -> Unit = {},
    content: @Composable () -> Unit,
) {
    val navController = LocalNavController.current
    val density = LocalDensity.current
    val focusManager = LocalFocusManager.current
    val configuration = LocalConfiguration.current
    val onBackPressedDispatcher = LocalOnBackPressedDispatcherOwner.current

    val previousSnackbarHostState = LocalSnackbarHost.current
    val snackbarHostState = remember {
        previousSnackbarHostState ?: SnackbarHostState()
    }
    val actionBarHeight = remember {
        androidx.compose.animation.core.Animatable(56f)
    }


    collapsingLayoutState.elements.firstOrNull()?.let { appbar ->
        LaunchedEffect(appbar.offset.doubleValue) {
            actionBarHeight.animateTo(
                appbar.offset.doubleValue.toFloat() + appbar.height.doubleValue.toFloat()
            )
        }
    }

    BackHandler(navController?.previousBackStackEntry != null) {
        if(onBackPressed()) navController?.popBackStack()
    }

    CompositionLocalProvider(LocalSnackbarHost provides snackbarHostState) {
        Scaffold(
            modifier = modifier
                .pointerInput(Unit) {
                    detectTapGestures(onTap = {
                        focusManager.clearFocus()
                    })
                },
            snackbarHost = {
                if(previousSnackbarHostState == null) {
                    BaseSnackbarHost(hostState = snackbarHostState)
                }
            },
            containerColor = Color.Transparent,
            contentColor = contentColor,
            floatingActionButton = floatingActionButton,
            floatingActionButtonPosition = floatingActionButtonPosition,
            content = { paddingValues ->
                Box {
                    // black bottom background in case of paddings of content
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .then(
                                if (containerColor != null) {
                                    Modifier.background(containerColor)
                                } else Modifier
                            )
                            .fillMaxWidth()
                            .height(configuration.screenHeightDp.div(2).dp)
                    )
                    CollapsingLayout(
                        modifier = Modifier.padding(paddingValues),
                        state = collapsingLayoutState,
                        content = listOf(
                            @Composable {
                                AnimatedVisibility(visible = appBarVisible) {
                                    CustomizableAppBar(
                                        title = title,
                                        navigationIcon = navigationIcon,
                                        subtitle = subtitle,
                                        actions = actionIcons,
                                        onNavigationIconClick = {
                                            onNavigationIconClick?.invoke()
                                                ?: onBackPressedDispatcher?.onBackPressedDispatcher
                                                    ?.onBackPressed()
                                        }
                                    )
                                }
                            } to CollapsingBehavior.ALWAYS,
                            @Composable {
                                val appbar = collapsingLayoutState.elements.firstOrNull()
                                val isCollapsed = remember {
                                    derivedStateOf {
                                        appbar?.offset?.doubleValue == appbar?.height?.doubleValue?.times(-1)
                                    }
                                }

                                val cornerRadius = animateDpAsState(
                                    targetValue = if(isCollapsed.value) {
                                        0.dp
                                    }else 24.dp,
                                    label = "cornerRadiusContent",
                                    animationSpec = spring(
                                        stiffness = StiffnessVeryLow
                                    )
                                )

                                Box(
                                    modifier = contentModifier
                                        .padding(bottom = with(density) { actionBarHeight.value.toDp() })
                                        .fillMaxSize()
                                        .then(
                                            if (containerColor != null) {
                                                Modifier
                                                    .background(
                                                        color = containerColor,
                                                        shape = if (!isCollapsed.value) {
                                                            RoundedCornerShape(
                                                                topEnd = cornerRadius.value,
                                                                topStart = cornerRadius.value
                                                            )
                                                        } else RectangleShape
                                                    )
                                                    .clip(
                                                        if (!isCollapsed.value) {
                                                            RoundedCornerShape(
                                                                topEnd = cornerRadius.value,
                                                                topStart = cornerRadius.value
                                                            )
                                                        } else RectangleShape
                                                    )
                                            } else Modifier
                                        )
                                ) {
                                    content()
                                }
                            } to CollapsingBehavior.NONE
                        )
                    )
                }
            }
        )
    }
}

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
                    Colors.RED_ERROR
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

data class CustomSnackbarVisuals(
    override val actionLabel: String?,
    override val duration: SnackbarDuration = if (actionLabel == null) SnackbarDuration.Short else SnackbarDuration.Long,
    override val message: String,
    val isError: Boolean = false,
    override val withDismissAction: Boolean = true
): SnackbarVisuals