package ui.home

import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.paging.LoadState
import app.cash.paging.compose.collectAsLazyPagingItems
import augmy.interactive.shared.ui.base.LocalContentSize
import augmy.interactive.shared.ui.theme.LocalTheme
import base.getOrNull
import base.theme.DefaultThemeStyles.Companion.fontQuicksandSemiBold
import components.UserProfileImage
import data.NetworkProximityCategory
import data.io.user.NetworkItemIO
import future_shared_module.ext.brandShimmerEffect
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import ui.network.list.NETWORK_SHIMMER_ITEM_COUNT
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/** 2 dimensional map representing currently downloaded network items */
@Composable
fun SocialCircleContent(
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel,
    headerHeightDp: Float
) {
    val contentSize = LocalContentSize.current
    val density = LocalDensity.current

    val networkItems = viewModel.networkItems.collectAsLazyPagingItems()
    val categories = viewModel.categories.collectAsState(initial = listOf())
    val customColors = viewModel.customColors.collectAsState(initial = mapOf())

    val isLoadingInitialPage = networkItems.loadState.refresh is LoadState.Loading
    val contentHeightPx = with(density) {
        contentSize.height.dp.toPx() - headerHeightDp.dp.toPx()
    }
    val isVertical = contentSize.width < contentSize.height
    val largerDimension = (if(isVertical) contentSize.height - headerHeightDp else contentSize.width).toFloat()
    val smallerDimension = (if(isVertical) contentSize.width else contentSize.height - headerHeightDp).toFloat()
    val coroutineScope = rememberCoroutineScope()

    val offset = remember {
        mutableStateOf(Offset(0f, 0f))
    }
    val scale = remember {
        Animatable(initialValue = 1f)
    }
    val initialScale = rememberSaveable {
        1 / (smallerDimension / largerDimension)
    }

    LaunchedEffect(Unit) {
        delay(500)
        if(scale.value == 1.0f) {

            // scale of sizes which is lower than 0, so we want to scale out the variable so it is equal to 1:1
            // this only means that it's scaled to be 1:1 ratio for the lowest size, but enlarged to the larger size
            scale.animateTo(targetValue = initialScale)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTapGestures(
                    onDoubleTap = { tapCenter ->
                        if (scale.value > initialScale) {
                            coroutineScope.launch {
                                scale.animateTo(initialScale)
                            }
                            offset.value = Offset(0f, 0f)
                        } else {
                            coroutineScope.launch {
                                scale.animateTo(initialScale * 2f)
                            }
                            val center = IntSize(
                                (contentSize.width.dp.toPx() / 2f).toInt(),
                                (contentHeightPx / 2f).toInt()
                            )
                            val xDiff = (tapCenter.x - center.width) * initialScale
                            val yDiff = ((tapCenter.y - center.height) * initialScale).coerceIn(
                                minimumValue = -(center.height * 2f),
                                maximumValue = (center.height * 2f)
                            )
                            offset.value = Offset(-xDiff, -yDiff)
                        }
                    }
                )
            }
            .transformable(
                rememberTransformableState { zoomChange, panChange, _ ->
                    val topX = contentSize.width * scale.value * 2 - smallerDimension
                    val topY = contentHeightPx * scale.value - smallerDimension

                    coroutineScope.launch {
                        scale.animateTo(
                            (scale.value * zoomChange * zoomChange).coerceIn(1f, initialScale * 3.5f)
                        )
                    }
                    offset.value = Offset(
                        x = (offset.value.x + panChange.x).coerceIn(
                            minimumValue = (-topX / 2),
                            maximumValue = (topX / 2)
                        ),
                        y = (offset.value.y + panChange.y).coerceIn(
                            minimumValue = (-topY / 4),
                            maximumValue = (topY / 4)
                        )
                    )
                }
            ),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .requiredSize(smallerDimension.dp)
                .aspectRatio(1f)
                .graphicsLayer {
                    scaleX = scale.value
                    scaleY = scale.value
                    translationX = offset.value.x
                    translationY = offset.value.y
                },
            contentAlignment = Alignment.Center
        ) {
            val presentShares = categories.value.sumOf { it.share.toDouble() }
            val missingShares = NetworkProximityCategory.entries.minus(categories.value.toSet())
                .sumOf { it.share.toDouble() }

            var previousShares = 0.0
            var startingIndex = 0
            NetworkProximityCategory.entries.forEach { category ->
                if(categories.value.contains(category)) {
                    val additionalShares = category.share / presentShares * missingShares
                    val shares = category.share + additionalShares + previousShares
                    val itemDimension = smallerDimension * (previousShares - shares)

                    val zIndex = categories.value.size - categories.value.indexOf(category) + 1f
                    val emptySpaceWidth = smallerDimension * (previousShares - shares)
                    val endIndex = if(networkItems.itemCount == 0 && isLoadingInitialPage) NETWORK_SHIMMER_ITEM_COUNT else networkItems.itemCount

                    val items = mutableListOf<NetworkItemIO?>()
                    var finished = false
                    for(index in startingIndex..endIndex) {
                        if(!finished) {
                            networkItems.getOrNull(index).let { data ->
                                if(data == null || category.range.contains(data.proximity ?: -1f)) {
                                    items.add(data)
                                }else {
                                    startingIndex = index
                                    finished = true
                                }
                            }
                        }
                    }

                    Layout(
                        modifier = Modifier
                            .background(
                                color = customColors.value[category] ?: category.color,
                                shape = CircleShape
                            )
                            .clip(CircleShape)
                            .animateContentSize(
                                alignment = Alignment.Center,
                                animationSpec = spring(
                                    dampingRatio = Spring.DampingRatioMediumBouncy,
                                    stiffness = Spring.StiffnessMedium
                                )
                            )
                            .zIndex(zIndex)
                            .size((smallerDimension * shares).dp),
                        content = {
                            val itemWidth = 2 * PI * smallerDimension * shares / items.size

                            items.forEach { data ->
                                NetworkItemCompact(
                                    modifier = Modifier
                                        .align(Alignment.Center)
                                        .width(itemWidth.dp - 6.dp)
                                        .zIndex(zIndex),
                                    data = data
                                )
                            }
                        }
                    ) { measurables, constraints ->
                        val placeables = measurables.map { measurable ->
                            measurable.measure(constraints)
                        }

                        layout(constraints.maxWidth, constraints.minHeight) {
                            val centerX = constraints.maxWidth / 2
                            val centerY = constraints.maxHeight / 2

                            val radius = smallerDimension * shares / 2

                            val itemCount = placeables.size
                            if (itemCount == 0) return@layout

                            val angleIncrement = (2 * PI) / itemCount

                            placeables.forEachIndexed { index, placeable ->
                                val angle = angleIncrement * index

                                val xOffset = (radius * cos(angle)).toInt()
                                val yOffset = (radius * sin(angle)).toInt()

                                placeable.placeRelative(
                                    x = centerX + xOffset - placeable.width / 2,
                                    y = centerY + yOffset - placeable.height / 2
                                )
                            }
                        }
                    }
                    previousShares = shares
                }
            }
        }
    }
}

@Composable
private fun NetworkItemCompact(
    modifier: Modifier = Modifier,
    data: NetworkItemIO?
) {
    Crossfade(data == null) { isShimmer ->
        Column(
            modifier = modifier.width(IntrinsicSize.Min),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if(isShimmer) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .brandShimmerEffect(shape = CircleShape)
                )
                Text(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 2.dp)
                        .brandShimmerEffect(),
                    text = "",
                    style = LocalTheme.current.styles.category.copy(
                        fontFamily = FontFamily(fontQuicksandSemiBold)
                    )
                )
            }else if(data != null) {
                UserProfileImage(
                    modifier = Modifier.size(48.dp),
                    model = data.photoUrl,
                    tag = data.tag
                )
                Text(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 2.dp),
                    text = data.displayName ?: "",
                    style = LocalTheme.current.styles.category.copy(
                        fontFamily = FontFamily(fontQuicksandSemiBold)
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}