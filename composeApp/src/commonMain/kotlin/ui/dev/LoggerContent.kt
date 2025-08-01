package ui.dev

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material.icons.outlined.Remove
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import augmy.interactive.com.BuildKonfig
import augmy.interactive.shared.ext.draggable
import augmy.interactive.shared.ext.scalingClickable
import augmy.interactive.shared.ui.components.ErrorHeaderButton
import augmy.interactive.shared.ui.components.MultiChoiceSwitch
import augmy.interactive.shared.ui.components.dialog.AlertDialog
import augmy.interactive.shared.ui.components.input.CustomTextField
import augmy.interactive.shared.ui.components.input.DELAY_BETWEEN_TYPING_SHORT
import augmy.interactive.shared.ui.components.rememberMultiChoiceState
import augmy.interactive.shared.ui.theme.LocalTheme
import augmy.interactive.shared.ui.theme.SharedColors
import base.theme.Colors
import base.utils.withPlainText
import io.ktor.http.HttpMethod
import io.ktor.http.headers
import korlibs.logger.Logger
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import utils.DeveloperUtils

@Composable
internal fun ColumnScope.LoggerContent(
    model: DeveloperConsoleModel,
    isCompact: Boolean
) {
    val coroutineScope = rememberCoroutineScope()

    val selectedTabIndex = rememberSaveable {
        mutableStateOf(0)
    }

    val pagerState = rememberPagerState(
        pageCount = { 3 },
        initialPage = selectedTabIndex.value
    )
    val switchThemeState = rememberMultiChoiceState(
        items = mutableListOf(
            "General",
            "Logs",
            "Http"
        ),
        onSelectionChange = {
            coroutineScope.launch {
                pagerState.scrollToPage(it)
            }
        },
        selectedTabIndex = selectedTabIndex
    )

    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.currentPage }.collectLatest {
            switchThemeState.selectedTabIndex.value = it
        }
    }

    MultiChoiceSwitch(
        modifier = Modifier.fillMaxWidth(),
        state = switchThemeState,
        shape = RectangleShape
    )
    HorizontalPager(
        modifier = Modifier
            .padding(horizontal = 8.dp)
            .padding(end = if(isCompact) 0.dp else 32.dp)
            .weight(1f),
        state = pagerState
    ) { index ->
        when(index) {
            0 -> GeneralContent(model)
            1 -> LogsContent(model)
            2 -> HttpContent(model)
        }
    }
}

@Composable
private fun LogsContent(model: DeveloperConsoleModel) {
    val coroutineScope = rememberCoroutineScope()

    val logs = model.logData.collectAsState(initial = listOf())
    val filter = model.logFilter.collectAsState()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        state = rememberLazyListState(),
        verticalArrangement = Arrangement.spacedBy(6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        headers {
            item {
                Column(modifier = Modifier.padding(top = 8.dp)) {
                    val filterState = remember { TextFieldState() }

                    LaunchedEffect(filterState.text) {
                        coroutineScope.coroutineContext.cancelChildren()
                        coroutineScope.launch {
                            delay(DELAY_BETWEEN_TYPING_SHORT)
                            model.filterLogs(filterState.text.toString())
                        }
                    }

                    CustomTextField(
                        modifier = Modifier.padding(horizontal = 8.dp),
                        hint = "Filter anything",
                        isClearable = true,
                        backgroundColor = LocalTheme.current.colors.backgroundLight,
                        paddingValues = PaddingValues(start = 16.dp),
                        state = filterState
                    )

                    val levelsState = rememberScrollState()
                    Row(
                        modifier = Modifier
                            .padding(top = 4.dp)
                            .fillMaxWidth()
                            .horizontalScroll(levelsState)
                            .draggable(levelsState, orientation = Orientation.Horizontal),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Spacer(Modifier.width(8.dp))
                        SortChip(
                            text = "Date",
                            isAsc = filter.value.second,
                            onClick = {
                                model.filterLogs(isAsc = !filter.value.second)
                            }
                        )
                        Logger.Level.entries.forEach { level ->
                            CheckChip(
                                text = level.name,
                                isChecked = filter.value.third == level,
                                onClick = {
                                    model.filterLogs(
                                        level = if (filter.value.third == level) null else level
                                    )
                                }
                            )
                        }
                        Spacer(Modifier.width(8.dp))
                    }
                }
            }
        }

        itemsIndexed(
            items = logs.value,
            key = { index, item -> item.uid }
        ) { index, log ->
            if (log.message != null) {
                val backgroundColor = when(log.level) {
                    Logger.Level.FATAL, Logger.Level.ERROR -> SharedColors.RED_ERROR_50
                    Logger.Level.WARN -> SharedColors.YELLOW_50
                    else -> LocalTheme.current.colors.backgroundLight
                }

                Column(modifier = Modifier.animateItem()) {
                    Row(
                        modifier = Modifier
                            .padding(1.dp)
                            .background(
                                color = backgroundColor,
                                shape = RoundedCornerShape(2.dp)
                            )
                            .padding(vertical = 2.dp, horizontal = 4.dp)
                            .fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            modifier = Modifier
                                .wrapContentWidth()
                                .padding(end = 4.dp),
                            text = log.level.name,
                            style = LocalTheme.current.styles.category
                        )
                        SelectionContainer {
                            Text(
                                modifier = Modifier
                                    .padding(2.dp)
                                    .weight(1f)
                                    .fillMaxWidth(),
                                text = log.message.toString(),
                                style = LocalTheme.current.styles.regular
                            )
                        }
                    }
                    if(index != logs.value.lastIndex) {
                        HorizontalDivider(
                            modifier = Modifier.fillMaxWidth(),
                            color = LocalTheme.current.colors.disabledComponent,
                            thickness = .3.dp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun GeneralContent(model: DeveloperConsoleModel) {
    val currentUser = model.currentUser.collectAsState()
    val localSettings = model.localSettings.collectAsState()

    val hostState = remember { TextFieldState(model.hostOverride ?: "") }

    LaunchedEffect(hostState.text) {
        model.changeHost(hostState.text)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            modifier = Modifier.padding(vertical = LocalTheme.current.shapes.betweenItemsSpace),
            text = "User data",
            style = LocalTheme.current.styles.subheading
        )
        RowInformation(title = "user_id: ", currentUser.value?.matrixUserId)
        RowInformation(title = "access_token: ", currentUser.value?.accessToken)
        RowInformation(title = "device_id: ", localSettings.value?.deviceId)
        RowInformation(title = "pickle_key: ", localSettings.value?.pickleKey)
        RowInformation(title = "privacy: ", currentUser.value?.configuration?.privacy)
        RowInformation(title = "visibility: ", currentUser.value?.configuration?.visibility)
        RowInformation(title = "homeserver: ", currentUser.value?.matrixHomeserver)
        Text(
            modifier = Modifier.padding(vertical = LocalTheme.current.shapes.betweenItemsSpace),
            text = "Firebase",
            style = LocalTheme.current.styles.subheading
        )
        RowInformation(title = "FCM token: ", localSettings.value?.fcmToken)

        Spacer(Modifier.height(8.dp))

        // actions and configuration
        Text(
            modifier = Modifier
                .padding(vertical = LocalTheme.current.shapes.betweenItemsSpace),
            text = "Configuration",
            style = LocalTheme.current.styles.subheading
        )
        RowInformation(title = "Client status: ", localSettings.value?.clientStatus)
        RowInformation(title = "Theme: ", localSettings.value?.theme)

        CustomTextField(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp),
            hint = "Host, default: ${BuildKonfig.HttpsHostName}",
            backgroundColor = LocalTheme.current.colors.backgroundLight,
            state = hostState,
            isClearable = true,
            paddingValues = PaddingValues(start = 16.dp)
        )

        val showDeleteDialog = remember {
            mutableStateOf(false)
        }
        if(showDeleteDialog.value) {
            AlertDialog(
                title = "Do you know what you're doing?",
                message = AnnotatedString("This may make your app a bit unfunctional before the app acquires all the necessary data to function again."),
                onDismissRequest = {
                    showDeleteDialog.value = true
                }
            )
        }

        ErrorHeaderButton(
            modifier = Modifier
                .padding(top = 32.dp)
                .fillMaxWidth(),
            text = "Delete local data and sign out",
            endImageVector = Icons.Outlined.Remove,
            onClick = {
                model.deleteLocalData()
            }
        )
    }
}

@Composable
private fun HttpContent(model: DeveloperConsoleModel) {
    val coroutineScope = rememberCoroutineScope()

    val logs = model.httpLogData.collectAsState(initial = listOf())
    val filter = model.httpLogFilter.collectAsState()

    LazyColumn(
        modifier = Modifier
            .padding(top = 8.dp)
            .fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        headers {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val filterState = remember { TextFieldState() }

                    LaunchedEffect(filterState.text) {
                        coroutineScope.coroutineContext.cancelChildren()
                        coroutineScope.launch {
                            delay(DELAY_BETWEEN_TYPING_SHORT)
                            model.filterHttpLogs(filterState.text.toString())
                        }
                    }

                    CustomTextField(
                        modifier = Modifier.weight(1f),
                        hint = "Filter anything",
                        isClearable = true,
                        backgroundColor = LocalTheme.current.colors.backgroundLight,
                        paddingValues = PaddingValues(start = 16.dp),
                        state = filterState
                    )
                    SortChip(
                        text = "Date",
                        isAsc = filter.value.second,
                        onClick = {
                            model.filterHttpLogs(isAsc = !filter.value.second)
                        }
                    )
                }
            }
        }
        itemsIndexed(
            items = logs.value,
            key = { _, item -> item.id }
        ) { index, item ->
            HttpDetail(
                modifier = Modifier.animateItem(),
                log = item
            )
            if(index != logs.value.lastIndex) {
                HorizontalDivider(
                    modifier = Modifier.fillMaxWidth(),
                    color = LocalTheme.current.colors.disabledComponent,
                    thickness = .3.dp
                )
            }
        }
        item {
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun SortChip(
    modifier: Modifier = Modifier,
    text: String,
    isAsc: Boolean?,
    onClick: () -> Unit
) {
    Row(
        modifier = modifier
            .scalingClickable {
                onClick()
            }
            .background(
                color = LocalTheme.current.colors.tetrial,
                shape = LocalTheme.current.shapes.componentShape
            )
            .padding(vertical = 2.dp, horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = text,
            style = LocalTheme.current.styles.category.copy(
                color = LocalTheme.current.colors.brandMainDark
            )
        )
        if(isAsc != null) {
            Crossfade(isAsc) { asc ->
                Icon(
                    imageVector = if(asc) Icons.Outlined.KeyboardArrowUp else Icons.Outlined.KeyboardArrowDown,
                    contentDescription = null,
                    tint = LocalTheme.current.colors.brandMainDark
                )
            }
        }
    }
}

@Composable
private fun CheckChip(
    modifier: Modifier = Modifier,
    text: String,
    isChecked: Boolean,
    onClick: () -> Unit
) {
    val backgroundColor = animateColorAsState(
        targetValue = if(isChecked) LocalTheme.current.colors.brandMainDark else LocalTheme.current.colors.tetrial
    )
    val contentColor = animateColorAsState(
        targetValue = if(isChecked) LocalTheme.current.colors.tetrial else LocalTheme.current.colors.brandMainDark
    )

    Row(
        modifier = modifier
            .scalingClickable {
                onClick()
            }
            .background(
                color = backgroundColor.value,
                shape = LocalTheme.current.shapes.componentShape
            )
            .padding(vertical = 4.dp, horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = text,
            style = LocalTheme.current.styles.regular.copy(color = contentColor.value)
        )
    }
}

@Composable
private fun HttpDetail(
    modifier: Modifier = Modifier,
    log: DeveloperUtils.HttpCall
) {
    val isExpanded = remember(log.id) {
        mutableStateOf(false)
    }

    Column(modifier) {
        Row(
            modifier = Modifier.clickable {
                isExpanded.value = !isExpanded.value
            },
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val backgroundColor = when(log.method) {
                HttpMethod.Get -> SharedColors.GREEN_CORRECT
                HttpMethod.Post -> Color.Blue
                HttpMethod.Put -> Color.Yellow
                HttpMethod.Delete -> SharedColors.RED_ERROR
                else -> Color.Gray
            }
            Box(modifier = Modifier
                .heightIn(min = 32.dp)
                .widthIn(min = 50.dp)
                .background(
                    color = backgroundColor,
                    shape = LocalTheme.current.shapes.rectangularActionShape
                )
                .padding(vertical = 2.dp, horizontal = 6.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = log.method?.value.toString(),
                    style = LocalTheme.current.styles.category.copy(
                        color = if(backgroundColor.luminance() > .5f) Colors.Coffee else Colors.GrayLight
                    )
                )
            }

            Text(
                modifier = Modifier.weight(1f),
                text = log.url ?: "",
                style = LocalTheme.current.styles.title
            )

            if(log.responseCode == null) {
                CircularProgressIndicator(
                    modifier = Modifier.requiredSize(32.dp),
                    color = LocalTheme.current.colors.brandMainDark,
                    trackColor = LocalTheme.current.colors.tetrial
                )
            }else {
                Box(modifier = Modifier
                    .heightIn(min = 32.dp)
                    .background(
                        color = when(log.responseCode) {
                            in 200..299 -> SharedColors.GREEN_CORRECT
                            in 300..399 -> Color.Yellow
                            else -> SharedColors.RED_ERROR
                        },
                        shape = LocalTheme.current.shapes.rectangularActionShape
                    )
                    .padding(vertical = 2.dp, horizontal = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = log.responseCode.toString(),
                        style = LocalTheme.current.styles.category.copy(color = Color.White)
                    )
                }
            }
        }
        AnimatedVisibility(isExpanded.value) {
            Column(
                modifier = Modifier.padding(top = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Created at: ${log.createdAt}",
                    style = LocalTheme.current.styles.category
                )
                Text(
                    text = "Time elapsed: ${log.responseSeconds}s",
                    style = LocalTheme.current.styles.category
                )
                Row {
                    Text(
                        text = "Headers:",
                        style = LocalTheme.current.styles.category
                    )
                    SelectionContainer(modifier = Modifier.weight(1f)) {
                        Text(
                            modifier = Modifier
                                .background(color = LocalTheme.current.colors.backgroundLight)
                                .padding(12.dp),
                            text = log.headers?.mapIndexed { index, s ->
                                if(index != 0) "\n$s" else s
                            }?.joinToString { it } ?: "",
                            style = LocalTheme.current.styles.title
                        )
                    }
                }
                if(!log.requestBody.isNullOrBlank()) {
                    Row {
                        Text(
                            text = "Request:",
                            style = LocalTheme.current.styles.category
                        )
                        SelectionContainer(modifier = Modifier.weight(1f)) {
                            Text(
                                modifier = Modifier
                                    .background(color = LocalTheme.current.colors.backgroundLight)
                                    .padding(12.dp),
                                text = log.requestBody ?: "",
                                style = LocalTheme.current.styles.title
                            )
                        }
                    }
                }
                Row {
                    Text(
                        text = "Response:",
                        style = LocalTheme.current.styles.category
                    )
                    SelectionContainer(modifier = Modifier.weight(1f)) {
                        Text(
                            modifier = Modifier
                                .background(color = LocalTheme.current.colors.backgroundLight)
                                .padding(12.dp),
                            text = log.responseBody ?: "",
                            style = LocalTheme.current.styles.title
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RowInformation(title: String, info: Any?) {
    val clipboard = LocalClipboard.current
    val scope = rememberCoroutineScope()

    Row {
        Text(
            text = title,
            style = LocalTheme.current.styles.category
        )
        Text(
            modifier = Modifier.weight(1f).scalingClickable(scaleInto = .92f) {
                scope.launch {
                    clipboard.withPlainText(info.toString())
                }
            },
            text = info.toString(),
            style = LocalTheme.current.styles.title,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}