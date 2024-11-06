package ui.dev

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Text
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import augmy.composeapp.generated.resources.Res
import augmy.composeapp.generated.resources.account_dashboard_fcm
import augmy.interactive.shared.ui.base.LocalDeviceType
import augmy.interactive.shared.ui.components.MultiChoiceSwitch
import augmy.interactive.shared.ui.components.rememberTabSwitchState
import augmy.interactive.shared.ui.theme.LocalTheme
import data.io.social.UserPrivacy
import data.shared.SharedViewModel
import future_shared_module.ext.scalingClickable
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import ui.network.received.NetworkReceivedContent

@Composable
fun DeveloperContent(viewModel: SharedViewModel = koinViewModel()) {
    val isCompact = LocalDeviceType.current == WindowWidthSizeClass.Compact
    val coroutineScope = rememberCoroutineScope()

    val consoleSize = remember {
        Animatable(
            initialValue = viewModel.developerConsoleSize.value
        )
    }

    Box(
        modifier = Modifier
            .then(
                if(isCompact) {
                    Modifier
                        .fillMaxWidth()
                        .height(kotlin.math.max(consoleSize.value, 24f).dp)
                } else Modifier
                    .fillMaxHeight()
                    .width(kotlin.math.max(consoleSize.value, 24f).dp)
            )
            .background(color = LocalTheme.current.colors.backgroundDark)
    ) {
        Box(
            modifier = (if(isCompact) {
                Modifier.fillMaxWidth(.3f)
            } else Modifier.fillMaxHeight(.15f))
                .draggable(
                    orientation = if(isCompact) Orientation.Vertical else Orientation.Horizontal,
                    state = rememberDraggableState { delta ->
                        viewModel.changeDeveloperConsole(
                            size = viewModel.developerConsoleSize.value + delta
                        )
                        coroutineScope.launch {
                            consoleSize.animateTo(viewModel.developerConsoleSize.value + delta)
                        }
                    }
                )
                .padding(8.dp)
                .background(
                    color = LocalTheme.current.colors.secondary,
                    shape = RoundedCornerShape(12.dp)
                )
                .align(if(isCompact) Alignment.BottomCenter else Alignment.CenterEnd)
                .padding(4.dp)
        )

        if(consoleSize.value > 50f) {
            Column(
                modifier = Modifier
                    .padding(horizontal = 8.dp)
                    .verticalScroll(rememberScrollState())
                    .then(if(!isCompact) Modifier.padding(end = 32.dp) else Modifier)
            ) {
                Spacer(Modifier.height(8.dp))
                InformationContent(viewModel = viewModel)
            }
        }
    }
}

@Composable
private fun ColumnScope.InformationContent(viewModel: SharedViewModel) {
    val currentUser = viewModel.currentUser.collectAsState()
    val firebaseUser = viewModel.firebaseUser.collectAsState()
    val localSettings = viewModel.localSettings.collectAsState()

    val clipboardManager = LocalClipboardManager.current
    val coroutineScope = rememberCoroutineScope()

    val selectedTabIndex = rememberSaveable {
        mutableStateOf(0)
    }

    val pagerState = rememberPagerState(
        pageCount = {
            if(currentUser.value?.configuration?.privacy != UserPrivacy.PUBLIC) 2 else 1
        },
        initialPage = selectedTabIndex.value
    )
    val switchThemeState = rememberTabSwitchState(
        tabs = mutableListOf(
            "Information",
            "HTTP",
            "Logs"
        ),
        onSelectionChange = {
            coroutineScope.launch {
                pagerState.animateScrollToPage(it)
            }
        },
        selectedTabIndex = selectedTabIndex
    )

    MultiChoiceSwitch(
        modifier = Modifier.fillMaxWidth(),
        state = switchThemeState
    )

    HorizontalPager(
        modifier = Modifier.weight(1f),
        state = pagerState,
        beyondViewportPageCount = 1
    ) { index ->
        if(index == 0) {
            Column {
                Text(
                    modifier = Modifier.padding(top = LocalTheme.current.shapes.betweenItemsSpace),
                    text = "User data",
                    style = LocalTheme.current.styles.subheading
                )
                if(localSettings.value?.fcmToken != null) {
                    Column {
                        Text(
                            stringResource(Res.string.account_dashboard_fcm),
                            style = LocalTheme.current.styles.category
                        )
                        Text(
                            modifier = Modifier.scalingClickable(
                                onTap = {
                                    clipboardManager.setText(
                                        AnnotatedString(localSettings.value?.fcmToken ?: "")
                                    )
                                }
                            ),
                            text = localSettings.value?.fcmToken ?: "",
                            style = LocalTheme.current.styles.title,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                Text(
                    modifier = Modifier.padding(top = LocalTheme.current.shapes.betweenItemsSpace),
                    text = "Firebase",
                    style = LocalTheme.current.styles.subheading
                )
                if(currentUser.value?.idToken != null) {
                    Column {
                        Text(
                            "Id token",
                            style = LocalTheme.current.styles.category
                        )
                        Text(
                            modifier = Modifier.scalingClickable(
                                onTap = {
                                    clipboardManager.setText(
                                        AnnotatedString(currentUser.value?.idToken ?: "")
                                    )
                                }
                            ),
                            text = currentUser.value?.idToken ?: "",
                            style = LocalTheme.current.styles.title,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                Text(
                    modifier = Modifier.padding(top = LocalTheme.current.shapes.betweenItemsSpace),
                    text = "Configuration",
                    style = LocalTheme.current.styles.subheading
                )
            }
        }else {
            NetworkReceivedContent()
        }
    }
}