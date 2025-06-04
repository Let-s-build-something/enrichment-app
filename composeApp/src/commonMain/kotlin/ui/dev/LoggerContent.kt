package ui.dev

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Divider
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material.icons.outlined.Remove
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import augmy.interactive.com.BuildKonfig
import augmy.interactive.shared.ext.scalingClickable
import augmy.interactive.shared.ui.components.ErrorHeaderButton
import augmy.interactive.shared.ui.components.MultiChoiceSwitch
import augmy.interactive.shared.ui.components.input.CustomTextField
import augmy.interactive.shared.ui.components.rememberMultiChoiceState
import augmy.interactive.shared.ui.theme.LocalTheme
import augmy.interactive.shared.ui.theme.SharedColors
import base.theme.Colors
import base.utils.withPlainText
import io.ktor.http.HttpMethod
import io.ktor.http.headers
import koin.DeveloperUtils
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@Composable
internal fun ColumnScope.LoggerContent(
    model: DeveloperConsoleModel,
    isCompact: Boolean
) {
    val currentUser = model.currentUser.collectAsState()
    val localSettings = model.localSettings.collectAsState()

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
            "Information",
            "HTTP"
        ),
        onSelectionChange = {
            coroutineScope.launch {
                pagerState.animateScrollToPage(it)
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
        state = pagerState,
        beyondViewportPageCount = 1
    ) { index ->
        when(index) {
            0 -> {
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
                    RowInformation(title = "publicId: ", currentUser.value?.publicId)
                    RowInformation(title = "displayName: ", currentUser.value?.displayName?.plus("#")?.plus(currentUser.value?.tag))
                    RowInformation(title = "privacy: ", currentUser.value?.configuration?.privacy)
                    RowInformation(title = "visibility: ", currentUser.value?.configuration?.visibility)
                    Text(
                        modifier = Modifier.padding(vertical = LocalTheme.current.shapes.betweenItemsSpace),
                        text = "Matrix",
                        style = LocalTheme.current.styles.subheading
                    )
                    RowInformation(title = "homeserver: ", currentUser.value?.matrixHomeserver)
                    RowInformation(title = "user_id: ", currentUser.value?.matrixUserId)
                    RowInformation(title = "access_token: ", currentUser.value?.accessToken)
                    RowInformation(title = "device_id: ", localSettings.value?.deviceId)
                    RowInformation(title = "pickle_key: ", localSettings.value?.pickleKey)
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
                        state = hostState,
                        isClearable = true,
                        paddingValues = PaddingValues(start = 16.dp)
                    )

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
            1 -> {
                val logs = model.httpLogData.collectAsState()

                val filteredLogs = remember {
                    mutableStateOf<List<DeveloperUtils.HttpCall>>(logs.value.httpCalls)
                }
                val isDateAsc = remember {
                    mutableStateOf(false)
                }
                val searchedText = remember {
                    mutableStateOf("")
                }

                LaunchedEffect(isDateAsc.value, searchedText.value, logs.value) {
                    filteredLogs.value = logs.value.httpCalls.filter {
                        it.url?.contains(searchedText.value) == true
                                || it.requestBody?.contains(searchedText.value) == true
                                || it.responseBody?.contains(searchedText.value) == true
                                || it.method?.value?.contains(searchedText.value) == true
                                || it.headers?.any { h -> h.contains(searchedText.value) } == true
                                || it.id.contains(searchedText.value)
                    }.sortedWith(
                        if(isDateAsc.value) {
                            compareBy { it.createdAt }
                        }else compareByDescending { it.createdAt }
                    )
                }

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
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.Bottom
                            ) {
                                val filterState = remember { TextFieldState() }

                                LaunchedEffect(filterState.text) {
                                    coroutineScope.coroutineContext.cancelChildren()
                                    coroutineScope.launch {
                                        delay(300)
                                        searchedText.value = filterState.text.toString()
                                    }
                                }

                                CustomTextField(
                                    hint = "Filter anything",
                                    isClearable = true,
                                    paddingValues = PaddingValues(start = 16.dp),
                                    state = filterState
                                )
                                SortChip(
                                    modifier = Modifier.padding(start = 4.dp),
                                    text = "Date",
                                    isAsc = isDateAsc.value,
                                    onClick = {
                                        isDateAsc.value = !isDateAsc.value
                                    }
                                )
                            }
                        }
                    }
                    items(
                        filteredLogs.value,
                        key = { it.id }
                    ) {
                        HttpDetail(
                            modifier = Modifier.animateItem(),
                            log = it
                        )
                        if(filteredLogs.value.size - 1 != index) {
                            Divider(
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
        }
    }
}

@Composable
private fun SortChip(
    modifier: Modifier = Modifier,
    text: String,
    isAsc: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = modifier
            .background(color = LocalTheme.current.colors.tetrial)
            .scalingClickable {
                onClick()
            }
            .padding(vertical = 2.dp, horizontal = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = text,
            style = LocalTheme.current.styles.category.copy(
                color = LocalTheme.current.colors.brandMainDark
            )
        )
        Crossfade(isAsc) { asc ->
            Icon(
                imageVector = if(asc) Icons.Outlined.KeyboardArrowUp else Icons.Outlined.KeyboardArrowDown,
                contentDescription = null,
                tint = LocalTheme.current.colors.brandMainDark
            )
        }
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