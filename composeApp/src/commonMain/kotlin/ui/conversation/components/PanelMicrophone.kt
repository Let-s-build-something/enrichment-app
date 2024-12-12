package ui.conversation.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.outlined.Pause
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableLongState
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import augmy.composeapp.generated.resources.Res
import augmy.composeapp.generated.resources.accessibility_audio_action_delete
import augmy.composeapp.generated.resources.accessibility_audio_action_lock
import augmy.composeapp.generated.resources.accessibility_audio_action_send
import augmy.composeapp.generated.resources.accessibility_message_action_audio
import augmy.composeapp.generated.resources.accessibility_pause
import augmy.composeapp.generated.resources.accessibility_resume
import augmy.composeapp.generated.resources.conversation_action_delete
import augmy.composeapp.generated.resources.conversation_action_send
import augmy.interactive.shared.ui.base.LocalScreenSize
import augmy.interactive.shared.ui.components.DEFAULT_ANIMATION_LENGTH_SHORT
import augmy.interactive.shared.ui.components.navigation.ActionBarIcon
import augmy.interactive.shared.ui.theme.LocalTheme
import augmy.interactive.shared.ui.theme.SharedColors
import base.isDarkTheme
import base.theme.Colors
import base.utils.AudioRecorder
import base.utils.PermissionType
import base.utils.rememberAudioRecorder
import base.utils.rememberPermissionRequesterState
import future_shared_module.ext.scalingDraggable
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import org.jetbrains.compose.resources.stringResource
import kotlin.math.PI
import kotlin.math.absoluteValue
import kotlin.math.cos
import kotlin.math.sin

/**
 * Interactable microphone component for recording audio with ability to save, lock, and discard the recording.
 * The locked mode has the same functionality with additional stop function.
 */
@Composable
fun PanelMicrophone(
    modifier: Modifier = Modifier,
    onSaveRequest: (ByteArray) -> Unit
) {
    val screenSize = LocalScreenSize.current
    val focusManager = LocalFocusManager.current
    val draggableArea = screenSize.width.coerceAtMost(screenSize.height).times(.5f).coerceAtMost(400f)
    val cancellableCoroutineScope = rememberCoroutineScope()
    val expectedBarsCount = remember(screenSize.width) { ((screenSize.width - 32) / 8) }
    val waveformHeight = remember { 50.dp }

    val isRecording = rememberSaveable {
        mutableStateOf(false)
    }
    val isLockedMode = rememberSaveable {
        mutableStateOf(false)
    }
    val startTime = remember { mutableLongStateOf(0L) }
    val millisecondsElapsed = remember {
        mutableLongStateOf(0L)
    }
    val recorder = rememberAudioRecorder(
        barsCount = expectedBarsCount,
        secondsPerBar = 0.1
    )
    val startRecording = {
        startTime.value = Clock.System.now().toEpochMilliseconds() - millisecondsElapsed.longValue
        isRecording.value = true
        recorder.startRecording()
    }
    val stopRecording = {
        startTime.longValue = 0L
        millisecondsElapsed.longValue = 0L
        isRecording.value = false
        recorder.stopRecording()
    }
    val selectedIndex = remember { mutableStateOf(-1) }
    val coordinates = remember { mutableStateOf(Offset.Zero) }

    LaunchedEffect(isRecording.value) {
        if (isRecording.value) {
            cancellableCoroutineScope.coroutineContext.cancelChildren()
            cancellableCoroutineScope.launch {
                while (isRecording.value) {
                    millisecondsElapsed.longValue += Clock.System.now().toEpochMilliseconds() - startTime.longValue - millisecondsElapsed.longValue
                    if (millisecondsElapsed.longValue >= MAX_RECORDING_LENGTH_MILLIS) {
                        recorder.pauseRecording()
                        isRecording.value = false
                    }
                    delay(100L)
                }
            }
        }
    }

    LaunchedEffect(isLockedMode.value) {
        if(isLockedMode.value) {
            focusManager.clearFocus()
        }
    }

    AnimatedVisibility(
        modifier = Modifier.zIndex(5f),
        visible = isLockedMode.value,
        enter = slideInVertically (
            initialOffsetY = { it },
            animationSpec = tween(DEFAULT_ANIMATION_LENGTH_SHORT)
        ),
        exit = slideOutVertically (
            targetOffsetY = { it },
            animationSpec = tween(DEFAULT_ANIMATION_LENGTH_SHORT)
        ),
    ) {
        AudioWaveForm(
            millisecondsElapsed = millisecondsElapsed,
            recorder = recorder,
            waveformHeight = waveformHeight,
            onSaveRequest = onSaveRequest,
            isRecording = isRecording,
            isLockedMode = isLockedMode,
            startRecording = startRecording,
            stopRecording = stopRecording
        )
    }

    ActionsForDrag(
        modifier = modifier,
        draggableArea = draggableArea.dp,
        isRecording = isRecording,
        isLockedMode = isLockedMode,
        selectedIndex = selectedIndex,
        coordinates = coordinates,
    )

    MicrophoneIcon(
        modifier = modifier,
        draggableArea = draggableArea.dp,
        waveformHeight = waveformHeight,
        isRecording = isRecording,
        isLockedMode = isLockedMode,
        selectedIndex = selectedIndex,
        coordinates = coordinates,
        startRecording = startRecording,
        stopRecording = stopRecording,
        onSaveRequest = onSaveRequest,
        recorder = recorder,
        millisecondsElapsed = millisecondsElapsed
    )
}

@Composable
private fun MicrophoneIcon(
    modifier: Modifier = Modifier,
    draggableArea: Dp,
    waveformHeight: Dp,
    isRecording: MutableState<Boolean>,
    isLockedMode: MutableState<Boolean>,
    selectedIndex: MutableState<Int>,
    coordinates: MutableState<Offset>,
    startRecording: () -> Unit,
    stopRecording: () -> Unit,
    onSaveRequest: (ByteArray) -> Unit,
    recorder: AudioRecorder,
    millisecondsElapsed: MutableLongState
) {
    val density = LocalDensity.current
    val screenSize = LocalScreenSize.current
    val coroutineScope = rememberCoroutineScope()
    val animCoroutineScope = rememberCoroutineScope()

    val permissionsRequester = rememberPermissionRequesterState(
        type = PermissionType.AUDIO_RECORD,
        onResponse = {
            if(it) {
                isLockedMode.value = true
                startRecording()
            }
        }
    )
    val animatedOffsetX = remember { Animatable(0f) }
    val animatedOffsetY = remember { Animatable(0f) }
    val offset = remember { mutableStateOf(Offset.Zero) }
    val microphoneSize = remember {
        mutableStateOf(IntSize.Zero)
    }

    Box(
        modifier = modifier
            .zIndex(1f)
            .padding(end = 16.dp)
            .onSizeChanged {
                microphoneSize.value = it
            }
            .heightIn(min = 44.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .offset(
                    x = with(density) { animatedOffsetX.value.toDp() }.coerceIn(
                        minimumValue = -draggableArea,
                        maximumValue = 0.dp
                    ),
                    y = with(density) { animatedOffsetY.value.toDp() }.coerceIn(
                        minimumValue = -draggableArea,
                        maximumValue = 0.dp
                    )
                )
                .then(
                    if(isRecording.value && !isLockedMode.value) {
                        Modifier.background(
                            color = LocalTheme.current.colors.backgroundDark,
                            shape = LocalTheme.current.shapes.componentShape
                        )
                    }else Modifier
                ),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            AnimatedVisibility(visible = isRecording.value && !isLockedMode.value) {
                Column(
                    modifier = Modifier
                        .padding(top = 6.dp, start = 8.dp, end = 8.dp)
                        .animateContentSize(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        modifier = Modifier
                            .align(Alignment.CenterHorizontally)
                            .padding(bottom = 4.dp),
                        text = "${formatTime(millisecondsElapsed.longValue)}/${
                            formatTime(
                                MAX_RECORDING_LENGTH_MILLIS
                            )
                        }",
                        style = LocalTheme.current.styles.regular.copy(
                            color = if(millisecondsElapsed.longValue < MAX_RECORDING_LENGTH_MILLIS) {
                                LocalTheme.current.colors.primary
                            }else SharedColors.RED_ERROR
                        )
                    )
                    Row(
                        modifier = Modifier.padding(bottom = waveformHeight / 1.5f, top = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        recorder.barPeakAmplitudes.value.takeLast(4).forEach { bar ->
                            Box(
                                modifier = Modifier
                                    .heightIn(min = 6.dp, max = waveformHeight)
                                    .width(4.dp)
                                    .background(
                                        color = LocalTheme.current.colors.secondary,
                                        shape = RoundedCornerShape(4.dp)
                                    )
                                    .height((bar.first / recorder.peakMedian.value * waveformHeight.value).dp)
                                    .animateContentSize()
                            )
                        }
                    }
                }
            }
            Box(
                modifier = Modifier.heightIn(min = 44.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    modifier = Modifier
                        .scalingDraggable(
                            onDrag = { dragged ->
                                if (dragged) {
                                    if (permissionsRequester.isGranted) {
                                        startRecording()
                                    }else {
                                        permissionsRequester.requestPermission()
                                    }
                                }else if (permissionsRequester.isGranted) {
                                    when(selectedIndex.value) {
                                        0 -> {
                                            isLockedMode.value = false
                                            stopRecording()
                                        }
                                        1 -> {
                                            isLockedMode.value = false
                                            coroutineScope.launch {
                                                recorder.saveRecording()?.let {
                                                    onSaveRequest(it)
                                                }
                                                stopRecording()
                                            }
                                        }
                                        -1, 2 -> isLockedMode.value = true
                                    }
                                    offset.value = Offset.Zero
                                    animCoroutineScope.coroutineContext.cancelChildren()
                                    animCoroutineScope.launch {
                                        animatedOffsetX.animateTo(offset.value.y)
                                    }
                                    animCoroutineScope.launch {
                                        animatedOffsetY.animateTo(offset.value.x)
                                    }
                                }
                            },
                            onDragChange = { _, dragAmount ->
                                if(permissionsRequester.isGranted) {
                                    offset.value += dragAmount
                                    animCoroutineScope.launch {
                                        animatedOffsetX.animateTo(
                                            offset.value.x.plus(
                                                animatedOffsetX.value.absoluteValue / screenSize.width * microphoneSize.value.width / 2
                                            )
                                        )
                                    }
                                    animCoroutineScope.launch {
                                        animatedOffsetY.animateTo(
                                            offset.value.y
                                                .plus(
                                                    animatedOffsetY.value.absoluteValue / screenSize.height * microphoneSize.value.height / 2
                                                )
                                                .minus(microphoneSize.value.height.div(2))
                                        )
                                    }
                                }
                            }
                        )
                        .onGloballyPositioned {
                            coordinates.value = it.positionInRoot()
                        }
                        .size(38.dp)
                        .zIndex(1f)
                        .background(
                            color = LocalTheme.current.colors.brandMainDark,
                            shape = LocalTheme.current.shapes.componentShape
                        )
                        .padding(6.dp),
                    imageVector = Icons.Outlined.Mic,
                    contentDescription = stringResource(Res.string.accessibility_message_action_audio),
                    tint = Color.White
                )
            }
        }
    }
}

@Composable
private fun ActionsForDrag(
    modifier: Modifier = Modifier,
    draggableArea: Dp,
    isRecording: MutableState<Boolean>,
    isLockedMode: MutableState<Boolean>,
    selectedIndex: MutableState<Int>,
    coordinates: MutableState<Offset>
) {
    val density = LocalDensity.current
    val actionOffsets = calculateActionOffsets(with(density) { draggableArea.toPx() })

    Layout(
        modifier = (if(isRecording.value && !isLockedMode.value) {
            modifier.background(
                color = LocalTheme.current.colors.backgroundDark.copy(.7f),
                shape = RoundedCornerShape(topStartPercent = 100)
            )
        }else modifier).size(draggableArea),
        content = {
            for (index in actionOffsets.indices) {
                AnimatedVisibility(
                    modifier = Modifier.requiredSize(48.dp),
                    visible = isRecording.value && !isLockedMode.value
                ) {
                    val positionInRoot = remember(index) {
                        mutableStateOf(Offset.Zero)
                    }
                    val isInArea = with(density) {
                        val startX = positionInRoot.value.x - 60.dp.toPx()
                        val endX = positionInRoot.value.x + 44.dp.toPx()
                        val startY = positionInRoot.value.y - 60.dp.toPx()
                        val endY = positionInRoot.value.y + 34.dp.toPx()

                        (coordinates.value.x - 19.dp.toPx()) in startX..endX &&
                                (coordinates.value.y - 19.dp.toPx()) in startY..endY
                    }
                    val tintColor = animateColorAsState(
                        targetValue = if (isInArea) {
                            when (index) {
                                0 -> SharedColors.RED_ERROR
                                1 -> LocalTheme.current.colors.brandMainDark
                                else -> LocalTheme.current.colors.secondary
                            }
                        } else Colors.GrayLight
                    )
                    val backgroundColor = animateColorAsState(
                        targetValue = if (isInArea) Colors.GrayLight
                        else when (index) {
                            0 -> SharedColors.RED_ERROR.copy(if(isDarkTheme) .5f else .8f)
                            1 -> LocalTheme.current.colors.brandMainDark.copy(if(isDarkTheme) .5f else .8f)
                            else -> LocalTheme.current.colors.backgroundLight
                        }
                    )
                    val imageVector = when (index) {
                        0 -> Icons.Outlined.Delete to Res.string.accessibility_audio_action_delete
                        1 -> Icons.AutoMirrored.Outlined.Send to Res.string.accessibility_audio_action_send
                        else -> Icons.Outlined.Lock to Res.string.accessibility_audio_action_lock
                    }

                    LaunchedEffect(isInArea) {
                        if (isInArea) {
                            selectedIndex.value = index
                        } else if (selectedIndex.value == index) {
                            selectedIndex.value = -1
                        }
                    }

                    Box(
                        modifier = Modifier
                            .background(
                                color = LocalTheme.current.colors.backgroundDark,
                                shape = CircleShape
                            )
                            .padding(4.dp)
                    ) {
                        Image(
                            modifier = Modifier
                                .zIndex(100f)
                                .onGloballyPositioned {
                                    positionInRoot.value = it.positionInRoot()
                                }
                                .size(48.dp)
                                .background(
                                    color = backgroundColor.value,
                                    shape = CircleShape
                                )
                                .padding(8.dp)
                                .animateContentSize(),
                            imageVector = imageVector.first,
                            contentDescription = stringResource(imageVector.second),
                            colorFilter = ColorFilter.tint(tintColor.value)
                        )
                    }
                }
            }
        }
    ) { measurables, constraints ->
        val placeables = measurables.map { measurable ->
            measurable.measure(constraints)
        }

        layout(constraints.maxWidth, constraints.minHeight) {
            placeables.forEachIndexed { index, placeable ->
                actionOffsets.getOrNull(index)?.let { offset ->
                    placeable.placeRelative(
                        x = offset.x.toInt() - placeable.width/2,
                        y = offset.y.toInt() - placeable.height/2
                    )
                }
            }
        }
    }
}

@Composable
private fun AudioWaveForm(
    millisecondsElapsed: MutableLongState,
    recorder: AudioRecorder,
    waveformHeight: Dp,
    onSaveRequest: (ByteArray) -> Unit,
    isRecording: MutableState<Boolean>,
    isLockedMode: MutableState<Boolean>,
    startRecording: () -> Unit,
    stopRecording: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(indication = null, interactionSource = null) {  }
            .zIndex(5f)
            .background(color = LocalTheme.current.colors.backgroundDark)
            .padding(horizontal = 16.dp, vertical = 24.dp)
            .padding(WindowInsets.navigationBars.asPaddingValues())
    ) {
        Text(
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .padding(bottom = 4.dp),
            text = "${formatTime(millisecondsElapsed.longValue)}/${
                formatTime(
                    MAX_RECORDING_LENGTH_MILLIS
                )
            }",
            style = LocalTheme.current.styles.regular.copy(
                color = if(millisecondsElapsed.longValue < MAX_RECORDING_LENGTH_MILLIS) {
                    LocalTheme.current.colors.primary
                }else SharedColors.RED_ERROR
            )
        )
        LazyRow(
            modifier = Modifier
                .height(waveformHeight)
                .fillMaxWidth(),
            reverseLayout = true,
            horizontalArrangement = Arrangement.spacedBy(
                space = 4.dp,
                alignment = Alignment.CenterHorizontally
            )
        ) {
            items(
                items = recorder.barPeakAmplitudes.value,
                key = { bar -> bar.second }
            ) { bar ->
                Box(
                    modifier = Modifier
                        .width(4.dp)
                        .height(waveformHeight)
                        .animateItem(),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .heightIn(min = 6.dp, max = waveformHeight)
                            .width(4.dp)
                            .background(
                                color = LocalTheme.current.colors.secondary,
                                shape = RoundedCornerShape(4.dp)
                            )
                            .height((bar.first / recorder.peakMedian.value * waveformHeight.value).dp)
                    )
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            ActionBarIcon(
                imageVector = Icons.Outlined.Delete,
                tint = SharedColors.RED_ERROR.copy(.7f),
                textTint = LocalTheme.current.colors.secondary,
                text = stringResource(Res.string.conversation_action_delete),
                onClick = {
                    isLockedMode.value = false
                    stopRecording()
                }
            )
            AnimatedVisibility(millisecondsElapsed.longValue < MAX_RECORDING_LENGTH_MILLIS) {
                ActionBarIcon(
                    modifier = Modifier.animateContentSize(),
                    imageVector = if(isRecording.value) Icons.Outlined.Pause else Icons.Outlined.Mic,
                    tint = SharedColors.YELLOW.copy(.7f),
                    textTint = LocalTheme.current.colors.secondary,
                    text = stringResource(
                        if(isRecording.value) Res.string.accessibility_pause else Res.string.accessibility_resume
                    ),
                    onClick = {
                        if(isRecording.value) {
                            isRecording.value = false
                            recorder.pauseRecording()
                        }else startRecording()
                    }
                )
            }
            ActionBarIcon(
                imageVector = Icons.AutoMirrored.Outlined.Send,
                tint = LocalTheme.current.colors.brandMain.copy(.7f),
                textTint = LocalTheme.current.colors.secondary,
                text = stringResource(Res.string.conversation_action_send),
                onClick = {
                    isLockedMode.value = false
                    coroutineScope.launch {
                        recorder.saveRecording()?.let {
                            onSaveRequest(it)
                        }
                    }
                    stopRecording()
                }
            )
        }
    }
}

private fun calculateActionOffsets(radius: Float): List<Offset> {
    val angles = listOf(190f, 225f, 260f)
    return angles.map { angle ->
        val radian = angle * PI.toFloat() / 180f
        Offset(
            x = (radius + radius * cos(radian)),
            y = (radius + radius * sin(radian))
        )
    }
}

private fun formatTime(millis: Long): String {
    val seconds = ((millis / 1000.0) % 60.0).toInt()
    val minutes = ((millis / (1000.0 * 60.0)) % 60.0).toInt()

    return "${if(minutes < 10) "0$minutes" else minutes}:${if(seconds < 10) "0$seconds" else seconds}"
}

private const val MAX_RECORDING_LENGTH_MILLIS = 4L * 60L * 1000L
