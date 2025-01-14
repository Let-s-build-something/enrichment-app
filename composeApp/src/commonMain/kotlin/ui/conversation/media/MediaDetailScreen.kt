package ui.conversation.media

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import augmy.composeapp.generated.resources.Res
import augmy.composeapp.generated.resources.accessibility_message_download
import augmy.composeapp.generated.resources.accessibility_message_forward
import augmy.composeapp.generated.resources.action_download_all
import augmy.composeapp.generated.resources.action_settings
import augmy.composeapp.generated.resources.action_share
import augmy.composeapp.generated.resources.ic_forward
import augmy.composeapp.generated.resources.navigate_close
import augmy.interactive.shared.ext.detectTransformTwoDown
import augmy.interactive.shared.ext.mouseDraggable
import augmy.interactive.shared.ext.scalingClickable
import augmy.interactive.shared.ui.base.LocalContentSize
import augmy.interactive.shared.ui.base.LocalDeviceType
import augmy.interactive.shared.ui.components.navigation.ActionBarIcon
import augmy.interactive.shared.ui.theme.LocalTheme
import base.BrandBaseScreen
import base.navigation.NavIconType
import base.utils.shareMessage
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import ui.conversation.components.ConversationKeyboardMode
import ui.conversation.components.MediaElement
import ui.conversation.components.audio.MediaProcessorModel

/**
 * Screen for displaying a list of media
 * @param urls list of remote urls to be displayed
 * @param selectedIndex initially selected index
 */
@Composable
fun MediaDetailScreen(
    processor: MediaProcessorModel = koinViewModel(),
    title: String?,
    subtitle: String?,
    urls: Array<out String?>,
    selectedIndex: Int
) {
    val coroutineScope = rememberCoroutineScope()

    val pagerState = rememberPagerState(
        initialPage = selectedIndex,
        pageCount = { urls.size }
    )
    val currentIndex = rememberSaveable(urls, selectedIndex) {
        mutableStateOf(selectedIndex)
    }
    val showOptions = rememberSaveable(urls, selectedIndex) {
        mutableStateOf(false)
    }
    val downloadState = rememberIndicationState(processor)

    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.currentPage }.collectLatest {
            currentIndex.value = it
        }
    }

    BrandBaseScreen(
        title = title + if(urls.size > 1) " | ${currentIndex.value + 1}/${urls.size}" else "",
        subtitle = subtitle,
        navIconType = NavIconType.CLOSE,
        actionIcons = { isExpanded ->
            Crossfade(showOptions.value) { optionsVisible ->
                ActionBarIcon(
                    text = if(isExpanded && LocalDeviceType.current != WindowWidthSizeClass.Compact) {
                        stringResource(if(optionsVisible) Res.string.navigate_close else Res.string.action_settings)
                    } else null,
                    imageVector = if(optionsVisible) Icons.Outlined.Close else Icons.Outlined.MoreHoriz,
                    onClick = {
                        showOptions.value = !showOptions.value
                    }
                )
            }
        }
    ) {
        val contentSize = LocalContentSize.current

        Column(modifier = Modifier.navigationBarsPadding()) {
            AnimatedVisibility(
                modifier = Modifier.zIndex(1f),
                visible = showOptions.value
            ) {
                Row(
                    modifier = Modifier
                        .zIndex(1f)
                        .background(color = LocalTheme.current.colors.backgroundLight)
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp, alignment = Alignment.End)
                ) {
                    ActionBarIcon(
                        text = stringResource(Res.string.action_download_all),
                        imageVector = Icons.Outlined.Download,
                        onClick = {
                            processor.processFiles(*urls)
                        }
                    )
                    ActionBarIcon(
                        text = stringResource(Res.string.action_share),
                        imageVector = Icons.Outlined.Share,
                        onClick = {
                            // TODO create new downloadable links, do not re-use
                            shareMessage(media = urls.filterNotNull())
                        }
                    )
                    ActionBarIcon(
                        text = stringResource(Res.string.accessibility_message_forward),
                        painter = painterResource(Res.drawable.ic_forward),
                        onClick = {
                            // TODO forward
                        }
                    )
                }
            }
            HorizontalPager(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .mouseDraggable(pagerState) {
                        currentIndex.value = ConversationKeyboardMode.entries[it].ordinal
                    },
                state = pagerState,
                beyondViewportPageCount = 1
            ) { index ->
                urls.getOrNull(index)?.let { url ->
                    val offset = remember(index) {
                        mutableStateOf(Offset(0f, 0f))
                    }
                    val scale = remember(index) {
                        Animatable(initialValue = 1f)
                    }

                    Box {
                        MediaElement(
                            modifier = Modifier
                                .fillMaxSize()
                                .graphicsLayer {
                                    scaleX = scale.value
                                    scaleY = scale.value
                                    translationX = offset.value.x
                                    translationY = offset.value.y
                                }
                                .pointerInput(Unit) {
                                    detectTapGestures(
                                        onDoubleTap = { tapCenter ->
                                            if (scale.value > 1f) {
                                                coroutineScope.launch {
                                                    scale.animateTo(1f)
                                                }
                                                offset.value = Offset(0f, 0f)
                                            } else {
                                                coroutineScope.launch {
                                                    scale.animateTo(2f)
                                                }
                                                val xDiff = contentSize.width - tapCenter.x
                                                val yDiff = contentSize.height - tapCenter.y
                                                offset.value = Offset(xDiff, yDiff)
                                            }
                                        }
                                    )
                                }
                                .pointerInput(Unit) {
                                    detectTransformTwoDown(
                                        isZoomed = {
                                            scale.value != 1f
                                        },
                                        onGesture = { _, panChange, zoomChange ->
                                            coroutineScope.launch {
                                                scale.animateTo(
                                                    (scale.value * zoomChange * zoomChange).coerceIn(
                                                        minimumValue = 1f,
                                                        maximumValue = 3.5f
                                                    )
                                                )
                                            }
                                            if(scale.value > 1f) {
                                                offset.value = Offset(
                                                    x = (offset.value.x + panChange.x * scale.value).coerceIn(
                                                        minimumValue = -contentSize.width * scale.value.minus(1),
                                                        maximumValue = contentSize.width * scale.value.minus(1)
                                                    ),
                                                    y = (offset.value.y + panChange.y * scale.value).coerceIn(
                                                        minimumValue = -contentSize.height.plus(60) * scale.value.minus(1),
                                                        maximumValue = contentSize.height.plus(60) * scale.value.minus(1)
                                                    )
                                                )
                                            }
                                        }
                                    )
                                },
                            contentScale = ContentScale.Fit,
                            url = url,
                            videoPlayerEnabled = true
                        )
                        Icon(
                            modifier = Modifier
                                .padding(bottom = 8.dp, end = 12.dp)
                                .size(36.dp)
                                .align(Alignment.BottomEnd)
                                .scalingClickable {
                                    processor.processFiles(url)
                                },
                            imageVector = Icons.Outlined.Download,
                            contentDescription = stringResource(Res.string.accessibility_message_download),
                            tint = LocalTheme.current.colors.secondary
                        )
                    }
                }
            }
            DownloadIndication(
                modifier = Modifier.align(alignment = Alignment.CenterHorizontally),
                state = downloadState
            )
        }
    }
}
