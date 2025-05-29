package ui.dev

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import augmy.interactive.shared.ext.draggable
import augmy.interactive.shared.ui.components.ComponentHeaderButton
import augmy.interactive.shared.ui.components.MinimalisticComponentIcon
import augmy.interactive.shared.ui.components.MultiChoiceSwitchMinimalistic
import augmy.interactive.shared.ui.components.SimpleModalBottomSheet
import augmy.interactive.shared.ui.components.input.CustomTextField
import augmy.interactive.shared.ui.components.rememberMultiChoiceState
import augmy.interactive.shared.ui.theme.LocalTheme
import data.io.base.BaseResponse
import data.sensor.SensorDelay

@Composable
internal fun ColumnScope.BiometricContent(model: DeveloperConsoleModel) {
    val streamingUrlResponse = model.streamingUrlResponse.collectAsState()
    val availableSensors = model.availableSensors.collectAsState()
    val activeSensors = model.activeSensors.collectAsState()

    val streamingUrlState = remember(model) {
        TextFieldState(initialText = model.streamingUrl)
    }
    val showSensorsLauncher = remember(model) {
        mutableStateOf(false)
    }

    if(showSensorsLauncher.value) {
        SensorSelectionLauncher(model = model) {
            showSensorsLauncher.value = false
        }
    }

    Text(
        modifier = Modifier
            .fillMaxWidth()
            .background(color = LocalTheme.current.colors.backgroundLight)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        text = "Dashboard",
        style = LocalTheme.current.styles.subheading
    )

    Row(
        modifier = Modifier
            .padding(start = 8.dp)
            .fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            modifier = Modifier.align(Alignment.CenterVertically),
            text = "${activeSensors.value.size}/${availableSensors.value.size} sensors registered",
            style = LocalTheme.current.styles.category
        )
        ComponentHeaderButton(
            text = "Modify",
            onClick = {
                showSensorsLauncher.value = true
            }
        )
    }

    Text(
        modifier = Modifier
            .fillMaxWidth()
            .background(color = LocalTheme.current.colors.backgroundLight)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        text = "Data streaming",
        style = LocalTheme.current.styles.subheading
    )
    HorizontalDivider(
        modifier = Modifier.fillMaxWidth(),
        color = LocalTheme.current.colors.disabled
    )
    Row(verticalAlignment = Alignment.CenterVertically) {
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
                model.selectStreamingUrl(streamingUrlState.text)
            },
            hint = "Target server url",
            state = streamingUrlState,
            errorText = (streamingUrlResponse.value as? BaseResponse.Error)?.message
        )
        MinimalisticComponentIcon(
            imageVector = Icons.Outlined.Check,
            tint = LocalTheme.current.colors.secondary,
            onTap = {
                model.selectStreamingUrl(streamingUrlState.text)
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SensorSelectionLauncher(
    model: DeveloperConsoleModel,
    onDismissRequest: () -> Unit
) {
    val availableSensors = model.availableSensors.collectAsState()
    val activeSensors = model.activeSensors.collectAsState()

    val sensorListState = rememberLazyListState()

    SimpleModalBottomSheet(
        onDismissRequest = onDismissRequest,
        scrollEnabled = false,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false),
        content = {

            Row {
                // TODO select all, deselect all
            }

            // TODO scrollbar
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .draggable(sensorListState, orientation = Orientation.Vertical),
                state = sensorListState,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(
                    items = availableSensors.value,
                    key = { it.uid }
                ) { sensor ->
                    val selectedDelayIndex = rememberSaveable(sensor) {
                        mutableStateOf(sensor.delay.ordinal)
                    }
                    val onCheck = {
                        if (activeSensors.value.contains(sensor.uid)) {
                            model.unRegisterSensor(sensor)
                        }else model.registerSensor(
                            sensor = sensor,
                            delay = SensorDelay.entries[selectedDelayIndex.value]
                        )
                    }

                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                modifier = Modifier
                                    .padding(top = 6.dp)
                                    .clickable(interactionSource = null, indication = null) {
                                        onCheck()
                                    },
                                text = sensor.name,
                                style = LocalTheme.current.styles.subheading
                            )
                            Switch(
                                modifier = Modifier.padding(horizontal = 8.dp),
                                colors = LocalTheme.current.styles.switchColorsDefault,
                                onCheckedChange = {
                                    onCheck()
                                },
                                checked = activeSensors.value.contains(sensor.uid)
                            )
                        }

                        sensor.maximumRange?.let {
                            Text(
                                modifier = Modifier.padding(start = 6.dp),
                                text = "Maximum range: $it",
                                style = LocalTheme.current.styles.regular
                            )
                        }
                        sensor.resolution?.let {
                            Text(
                                modifier = Modifier.padding(start = 6.dp),
                                text = "Resolution: $it",
                                style = LocalTheme.current.styles.regular
                            )
                        }
                        Row(
                            modifier = Modifier
                                .padding(bottom = 6.dp, start = 6.dp)
                                .fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Data collection speed: ",
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
                                    model.changeSensorDelay(sensor, SensorDelay.entries[index])
                                },
                                onItemCreation = { _, index, _ ->
                                    Text(
                                        modifier = Modifier
                                            .weight(1f)
                                            .padding(horizontal = 6.dp),
                                        text = SensorDelay.entries[index].name,
                                        style = LocalTheme.current.styles.subheading.copy(
                                            textAlign = TextAlign.Center
                                        )
                                    )
                                }
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
