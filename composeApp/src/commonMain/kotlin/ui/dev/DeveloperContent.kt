package ui.dev

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.WavingHand
import androidx.compose.material3.Icon
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import augmy.interactive.shared.ext.scalingClickable
import augmy.interactive.shared.ui.base.LocalDeviceType
import augmy.interactive.shared.ui.base.LocalScreenSize
import augmy.interactive.shared.ui.theme.LocalTheme
import kotlinx.coroutines.launch
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.context.loadKoinModules

private enum class ConsoleSection(val imageVector: ImageVector) {
    Logger(Icons.AutoMirrored.Outlined.ReceiptLong),
    Biometric(Icons.Outlined.WavingHand);

    override fun toString(): String {
        return when(this) {
            Logger -> "Logger"
            Biometric -> "Insight console"
        }
    }
}

@Composable
fun DeveloperContent(modifier: Modifier = Modifier) {
    loadKoinModules(developerConsoleModule)
    val model: DeveloperConsoleModel = koinViewModel()

    val coroutineScope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current
    val density = LocalDensity.current
    val screenSize = LocalScreenSize.current
    val isCompact = LocalDeviceType.current == WindowWidthSizeClass.Compact

    val consoleSize = model.developerConsoleSize.collectAsState()
    val selectedSectionIndex = rememberSaveable {
        mutableStateOf(0)
    }

    val sectionPagerState = rememberPagerState(
        initialPage = selectedSectionIndex.value,
        pageCount = {
            ConsoleSection.entries.size
        }
    )

    LaunchedEffect(selectedSectionIndex.value) {
        sectionPagerState.animateScrollToPage(selectedSectionIndex.value)
    }

    Box(
        modifier = modifier
            .pointerInput(Unit) {
                detectTapGestures(onTap = {
                    focusManager.clearFocus()
                })
            }
            .then(
                if(isCompact) {
                    Modifier
                        .fillMaxWidth()
                        .height(consoleSize.value.coerceIn(
                            minimumValue = 32f,
                            maximumValue = screenSize.height * 0.8f
                        ).dp)
                } else Modifier
                    .fillMaxHeight()
                    .width(consoleSize.value.coerceIn(
                        minimumValue = 32f,
                        maximumValue = screenSize.width * 0.8f
                    ).dp)
            )
            .background(color = LocalTheme.current.colors.backgroundDark),
        contentAlignment = Alignment.TopStart
    ) {
        Box(
            modifier = (if(isCompact) {
                Modifier.fillMaxWidth(.3f)
            } else Modifier.fillMaxHeight(.15f))
                .draggable(
                    orientation = if(isCompact) Orientation.Vertical else Orientation.Horizontal,
                    state = rememberDraggableState { delta ->
                        coroutineScope.launch {
                            model.changeDeveloperConsole(
                                size = model.developerConsoleSize.value + with(density) { delta.toDp().value }
                            )
                        }
                    }
                )
                .padding(12.dp)
                .background(
                    color = LocalTheme.current.colors.secondary,
                    shape = RoundedCornerShape(12.dp)
                )
                .align(if(isCompact) Alignment.BottomCenter else Alignment.CenterEnd)
                .padding(4.dp)
                .zIndex(100f)
        )

        if(consoleSize.value > ((if(isCompact) screenSize.height else screenSize.width) * 0.2f)) {
            Row(modifier = Modifier.fillMaxSize()) {
                Column(
                    modifier = Modifier
                        .padding(vertical = 16.dp, horizontal = 2.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ConsoleSection.entries.forEach { section ->
                        SectionIndicator(
                            isSelected = section.ordinal == selectedSectionIndex.value,
                            imageVector = section.imageVector,
                            onClick = {
                                selectedSectionIndex.value = section.ordinal
                            }
                        )
                    }
                }

                VerticalPager(
                    modifier = Modifier.padding(start = 4.dp),
                    state = sectionPagerState,
                    userScrollEnabled = false
                ) { index ->
                    val section = ConsoleSection.entries[index]

                    Column(modifier = Modifier.fillMaxSize()) {
                        Text(
                            modifier = Modifier.padding(vertical = 6.dp, horizontal = 4.dp),
                            text = section.name.toString(),
                            style = LocalTheme.current.styles.heading
                        )
                        when(section) {
                            ConsoleSection.Logger -> {
                                LoggerContent(model = model, isCompact = isCompact)
                            }
                            ConsoleSection.Biometric -> {
                                BiometricContent(model = model)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionIndicator(
    modifier: Modifier = Modifier,
    imageVector: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val tint = animateColorAsState(
        targetValue = if(isSelected) {
            LocalTheme.current.colors.brandMain
        }else LocalTheme.current.colors.disabled
    )

    Icon(
        modifier = modifier
            .size(52.dp)
            .scalingClickable {
                onClick()
            }
            .padding(6.dp)
            .padding(4.dp),
        imageVector = imageVector,
        tint = tint.value,
        contentDescription = null
    )
}
