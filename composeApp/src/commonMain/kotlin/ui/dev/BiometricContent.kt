package ui.dev

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.CleaningServices
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Deselect
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.SelectAll
import androidx.compose.material.icons.outlined.Stop
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import augmy.composeapp.generated.resources.Res
import augmy.composeapp.generated.resources.button_confirm
import augmy.composeapp.generated.resources.button_dismiss
import augmy.interactive.shared.ext.scalingClickable
import augmy.interactive.shared.ui.components.BrandHeaderButton
import augmy.interactive.shared.ui.components.ContrastHeaderButton
import augmy.interactive.shared.ui.components.ErrorHeaderButton
import augmy.interactive.shared.ui.components.LoadingHeaderButton
import augmy.interactive.shared.ui.components.MultiChoiceSwitchMinimalistic
import augmy.interactive.shared.ui.components.ProgressPressableContainer
import augmy.interactive.shared.ui.components.dialog.AlertDialog
import augmy.interactive.shared.ui.components.dialog.ButtonState
import augmy.interactive.shared.ui.components.input.CustomTextField
import augmy.interactive.shared.ui.components.rememberMultiChoiceState
import augmy.interactive.shared.ui.theme.LocalTheme
import augmy.interactive.shared.ui.theme.SharedColors
import augmy.interactive.shared.utils.DateUtils
import augmy.interactive.shared.utils.DateUtils.formatAs
import components.ScrollBarProgressIndicator
import data.io.base.BaseResponse
import data.sensor.SensorDelay
import data.sensor.SensorEventListener
import io.github.vinceglb.filekit.compose.rememberFileSaverLauncher
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun BiometricContent(model: DeveloperConsoleModel) {
    StreamingSection(model)

    DashboardSection(model)
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun DashboardSection(model: DeveloperConsoleModel) {
    val sensorListState = rememberLazyListState()
    val availableSensors = model.availableSensors.collectAsState()
    val activeSensors = model.activeSensors.collectAsState()

    val showSensorDialog = remember {
        mutableStateOf<SensorEventListener?>(null)
    }

    showSensorDialog.value?.let { sensor ->
        val data = sensor.data.collectAsState()

        AlertDialog(
            title = sensor.name,
            dismissButtonState = ButtonState(text = stringResource(Res.string.button_dismiss)),
            icon = Icons.Outlined.History,
            onDismissRequest = {
                showSensorDialog.value = null
            },
            intrinsicContent = false,
            additionalContent = {
                LazyColumn(modifier = Modifier.animateContentSize()) {
                    items(items = data.value) { record ->
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .animateItem(),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            record.values?.let {
                                SelectionContainer {
                                    Text(
                                        text = it.joinToString(separator = ", "),
                                        style = LocalTheme.current.styles.category
                                    )
                                }
                            }
                            record.uiValues?.let {
                                val text = buildAnnotatedString {
                                    it.forEach { window ->
                                        withStyle(LocalTheme.current.styles.category.toSpanStyle()) {
                                            append("\n${window.key}")
                                        }
                                        append(": ${window.value}")
                                    }
                                }

                                SelectionContainer {
                                    Text(
                                        text = text,
                                        style = LocalTheme.current.styles.regular
                                    )
                                }
                            }
                            SelectionContainer {
                                Text(
                                    text = "Timestamp: ${DateUtils.fromMillis(record.timestamp).formatAs("HH:mm:ss")}",
                                    style = LocalTheme.current.styles.regular
                                )
                            }

                            HorizontalDivider(
                                modifier = Modifier.fillMaxWidth(),
                                color = LocalTheme.current.colors.disabled
                            )
                        }
                    }
                }
            }
        )
    }

    Text(
        modifier = Modifier
            .padding(top = 16.dp)
            .fillMaxWidth()
            .background(color = LocalTheme.current.colors.appbarBackground)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        text = "Dashboard",
        style = LocalTheme.current.styles.subheading
    )

    Text(
        text = "${activeSensors.value.size}/${availableSensors.value.size} sensors registered",
        style = LocalTheme.current.styles.regular
    )

    ScrollBarProgressIndicator(
        modifier = Modifier.fillMaxWidth(),
        state = sensorListState
    )
    LazyColumn(
        modifier = Modifier.animateContentSize(),
        state = sensorListState,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        stickyHeader {
            val showDialog = remember {
                mutableStateOf(false)
            }

            if(showDialog.value) {
                AlertDialog(
                    title = "Reset all",
                    dismissButtonState = ButtonState(text = stringResource(Res.string.button_dismiss)),
                    confirmButtonState = ButtonState(
                        text = stringResource(Res.string.button_confirm),
                        onClick = {
                            model.resetAllSensors()
                        }
                    ),
                    onDismissRequest = {
                        showDialog.value = false
                    }
                )
            }

            val filePicker = rememberFileSaverLauncher(
                onResult = { file ->
                    model.exportData(file)
                }
            )
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ErrorHeaderButton(
                    text = "Reset all",
                    contentPadding = PaddingValues(vertical = 6.dp, horizontal = 10.dp),
                    endImageVector = Icons.Outlined.CleaningServices,
                    onClick = {
                        showDialog.value = true
                    }
                )
                ErrorHeaderButton(
                    text = "Deselect",
                    contentPadding = PaddingValues(vertical = 6.dp, horizontal = 10.dp),
                    endImageVector = Icons.Outlined.Deselect,
                    onClick = {
                        model.unregisterAllSensors()
                    }
                )
                ContrastHeaderButton(
                    text = "Select all",
                    contentPadding = PaddingValues(vertical = 6.dp, horizontal = 10.dp),
                    contentColor = LocalTheme.current.colors.brandMainDark,
                    containerColor = LocalTheme.current.colors.brandMain,
                    endImageVector = Icons.Outlined.SelectAll,
                    onClick = {
                        model.registerAllSensors()
                    }
                )
                BrandHeaderButton(
                    text = "Export",
                    endImageVector = Icons.Outlined.Download,
                    onClick = {
                        filePicker.launch(extension = "txt", baseName = "log-sensory-augmy")
                    }
                )
            }
        }
        items(
            items = availableSensors.value,
            key = { it.uid }
        ) { sensor ->
            val selectedDelayIndex = rememberSaveable(sensor) {
                mutableStateOf(sensor.delay.ordinal)
            }

            val data = sensor.data.collectAsState()

            Column {
                Row(
                    modifier = Modifier.animateItem(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ProgressPressableContainer(
                        modifier = Modifier
                            .padding(end = 16.dp)
                            .requiredSize(36.dp),
                        onFinish = {
                            sensor.data.value = listOf()
                        },
                        trackColor = LocalTheme.current.colors.disabled,
                        progressColor = SharedColors.RED_ERROR
                    ) {
                        Icon(
                            modifier = Modifier.size(32.dp),
                            imageVector = Icons.Outlined.Delete,
                            contentDescription = null,
                            tint = SharedColors.RED_ERROR
                        )
                    }

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .scalingClickable(scaleInto = .95f) {
                                showSensorDialog.value = sensor
                            },
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            modifier = Modifier.padding(start = 8.dp),
                            text = sensor.name,
                            style = LocalTheme.current.styles.category
                        )
                        sensor.maximumRange?.let {
                            Text(
                                modifier = Modifier.padding(start = 12.dp),
                                text = "Maximum range: $it",
                                style = LocalTheme.current.styles.regular
                            )
                        }
                        sensor.resolution?.let {
                            Text(
                                modifier = Modifier.padding(start = 12.dp),
                                text = "Resolution: $it",
                                style = LocalTheme.current.styles.regular
                            )
                        }
                        Text(
                            modifier = Modifier.padding(start = 12.dp),
                            text = "Collected: ${data.value.size}",
                            style = LocalTheme.current.styles.regular
                        )
                        Text(
                            modifier = Modifier.padding(start = 12.dp),
                            text = "Last record: ${data.value.firstOrNull()?.let { value ->
                                value.values?.toList() ?: value.uiValues
                            }}",
                            style = LocalTheme.current.styles.regular
                        )

                        Row(
                            modifier = Modifier
                                .padding(bottom = 6.dp)
                                .fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Speed: ",
                                style = LocalTheme.current.styles.regular
                            )
                            MultiChoiceSwitchMinimalistic(
                                state = rememberMultiChoiceState(
                                    selectedTabIndex = selectedDelayIndex,
                                    items = SensorDelay.entries.map { it.name }.toMutableList()
                                ),
                                onClick = { index ->
                                    selectedDelayIndex.value = index
                                    model.changeSensorDelay(sensor, SensorDelay.entries[index])
                                },
                                onItemCreation = { _, index, _ ->
                                    Text(
                                        modifier = Modifier.fillMaxWidth(),
                                        text = SensorDelay.entries[index].name,
                                        style = LocalTheme.current.styles.category.copy(
                                            textAlign = TextAlign.Center
                                        )
                                    )
                                }
                            )
                        }
                    }

                    Switch(
                        modifier = Modifier
                            .align(Alignment.CenterVertically)
                            .padding(end = 16.dp),
                        colors = LocalTheme.current.styles.switchColorsDefault,
                        onCheckedChange = {
                            if (activeSensors.value.contains(sensor.uid)) {
                                model.unRegisterSensor(sensor)
                            }else model.registerSensor(
                                sensor = sensor,
                                delay = SensorDelay.entries[selectedDelayIndex.value]
                            )
                        },
                        checked = activeSensors.value.contains(sensor.uid)
                    )
                }

                HorizontalDivider(
                    modifier = Modifier.fillMaxWidth(),
                    color = LocalTheme.current.colors.disabled
                )
            }
        }
        item {
            Spacer(Modifier.height(120.dp))
        }
    }
}

@Composable
private fun StreamingSection(model: DeveloperConsoleModel) {
    val streamingUrlResponse = model.streamingUrlResponse.collectAsState()

    val streamingUrlState = remember(model) {
        TextFieldState(initialText = model.streamingUrl)
    }

    val filePicker = rememberFileSaverLauncher(
        onResult = { filePicker ->
            model.setUpLocalStream(filePicker)
        }
    )

    Text(
        modifier = Modifier
            .fillMaxWidth()
            .background(color = LocalTheme.current.colors.appbarBackground)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        text = "Data streaming",
        style = LocalTheme.current.styles.subheading
    )

    Row(
        modifier = Modifier.padding(top = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AnimatedVisibility(
            modifier = Modifier.weight(1f),
            visible = streamingUrlResponse.value !is BaseResponse.Success
        ) {
            CustomTextField(
                modifier = Modifier.padding(
                    horizontal = 8.dp,
                    vertical = 6.dp
                ),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Uri,
                    imeAction = ImeAction.Done
                ),
                lineLimits = TextFieldLineLimits.SingleLine,
                onKeyboardAction = {
                    model.setupRemoteStream(streamingUrlState.text)
                },
                hint = "Server url",
                state = streamingUrlState,
                shape = LocalTheme.current.shapes.componentShape,
                errorText = (streamingUrlResponse.value as? BaseResponse.Error)?.message
            )
        }
        Crossfade(targetState = streamingUrlResponse.value) { response ->
            when(response) {
                is BaseResponse.Success -> {
                    ContrastHeaderButton(
                        text = "Stop remote stream",
                        endImageVector = Icons.Outlined.Stop,
                        contentColor = Color.White,
                        containerColor = SharedColors.RED_ERROR,
                        onClick = {
                            model.stopRemoteStream()
                        }
                    )
                }
                else -> {
                    LoadingHeaderButton(
                        text = "Stream",
                        isLoading = streamingUrlResponse.value is BaseResponse.Loading,
                        isEnabled = streamingUrlResponse.value !is BaseResponse.Loading,
                        endImageVector = Icons.Outlined.Check,
                        onClick = {
                            model.setupRemoteStream(streamingUrlState.text)
                        }
                    )
                }
            }
        }
        AnimatedVisibility(streamingUrlResponse.value is BaseResponse.Success) {
            val selectedDelayIndex = rememberSaveable {
                mutableStateOf(model.remoteStreamDelay.ordinal)
            }

            Row(
                modifier = Modifier.padding(start = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Upload by: ",
                    style = LocalTheme.current.styles.regular
                )
                MultiChoiceSwitchMinimalistic(
                    modifier = Modifier.padding(start = 6.dp),
                    state = rememberMultiChoiceState(
                        selectedTabIndex = selectedDelayIndex,
                        items = SensorDelay.entries.map { it.name }.toMutableList()
                    ),
                    onClick = { index ->
                        selectedDelayIndex.value = index
                        model.remoteStreamDelay =  SensorDelay.entries[index]
                    },
                    onItemCreation = { _, index, _ ->
                        Text(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 6.dp),
                            text = when(SensorDelay.entries[index]) {
                                SensorDelay.Slow -> 50
                                SensorDelay.Normal -> 20
                                SensorDelay.Fast -> 1
                            }.toString(),
                            style = LocalTheme.current.styles.category.copy(
                                textAlign = TextAlign.Center
                            )
                        )
                    }
                )
            }
        }
    }

    val localRunning = model.isLocalStreamRunning.collectAsState()
    Crossfade(
        modifier = Modifier.padding(top = 8.dp),
        targetState = localRunning.value
    ) {
        if (it) {
            ContrastHeaderButton(
                text = "Stop local stream",
                endImageVector = Icons.Outlined.Stop,
                contentColor = Color.White,
                containerColor = SharedColors.RED_ERROR,
                onClick = {
                    model.stopLocalStream()
                }
            )
        }else {
            ContrastHeaderButton(
                text = "Stream locally",
                endImageVector = Icons.Outlined.Folder,
                contentColor = LocalTheme.current.colors.tetrial,
                containerColor = LocalTheme.current.colors.brandMainDark,
                onClick = {
                    filePicker.launch(extension = "txt", baseName = "stream-sensory-augmy")
                }
            )
        }
    }

    val streamLines = model.streamLines.collectAsState()
    AnimatedVisibility(
        modifier = Modifier
            .padding(top = 8.dp)
            .fillMaxWidth(),
        visible = streamLines.value.isNotEmpty()
    ) {
        val state = rememberLazyListState()

        LaunchedEffect(streamLines.value.size) {
            state.animateScrollToItem(0)
        }

        LazyColumn(
            modifier = Modifier
                .requiredHeight(50.dp)
                .padding(horizontal = 8.dp),
            state = state,
            verticalArrangement = Arrangement.spacedBy(6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            reverseLayout = true
        ) {
            items(items = streamLines.value) { line ->
                Text(
                    modifier = Modifier.animateItem(),
                    text = line,
                    style = LocalTheme.current.styles.regular.copy(
                        color = LocalTheme.current.colors.disabled
                    )
                )
            }
        }
    }
}
