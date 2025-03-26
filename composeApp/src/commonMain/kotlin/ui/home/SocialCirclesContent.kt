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
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import augmy.interactive.shared.ext.brandShimmerEffect
import augmy.interactive.shared.ext.onMouseScroll
import augmy.interactive.shared.ext.scalingClickable
import augmy.interactive.shared.ui.base.LocalDeviceType
import augmy.interactive.shared.ui.base.LocalNavController
import augmy.interactive.shared.ui.theme.LocalTheme
import base.navigation.NavigationNode
import base.theme.DefaultThemeStyles.Companion.fontQuicksandSemiBold
import components.UserProfileImage
import data.NetworkProximityCategory
import data.io.user.NetworkItemIO
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
    viewModel: HomeModel
) {
    val density = LocalDensity.current
    val navController = LocalNavController.current

    val networkItems = viewModel.networkItems.collectAsState(null)
    val categories = viewModel.categories.collectAsState(initial = listOf())
    val customColors = viewModel.customColors.collectAsState(initial = mapOf())

    val itemPaddingPx = with(density) { 2.dp.toPx() }
    var contentSize by remember {
        mutableStateOf(IntSize(0, 0))
    }
    val layerPadding = 12.dp
    val isVertical = contentSize.width < contentSize.height
    val largerDimension = (if(isVertical) contentSize.height else contentSize.width).toFloat()
    val smallerDimension = (if(isVertical) contentSize.width else contentSize.height).toFloat()
    val maxZoom = if(LocalDeviceType.current == WindowWidthSizeClass.Expanded) 5f else 3.5f
    val coroutineScope = rememberCoroutineScope()

    val offset = remember {
        mutableStateOf(Offset(0f, 0f))
    }
    val scale = remember {
        Animatable(initialValue = 1f)
    }
    val initialScale = rememberSaveable { 1f }

    LaunchedEffect(Unit) {
        delay(500)
        if(scale.value == 1.0f) {

            // scale of sizes which is lower than 0, so we want to scale out the variable so it is equal to 1:1
            // this only means that it's scaled to be 1:1 ratio for the lowest size, but enlarged to the larger size
            scale.animateTo(targetValue = initialScale)
        }
    }

    LaunchedEffect(scale.value) {
        val maxOffsetX = (largerDimension * (scale.value.coerceAtLeast(1f) - 1)) / 2
        val maxOffsetY = (largerDimension * (scale.value.coerceAtLeast(1f) - 1)) / 2

        offset.value = Offset(
            x = offset.value.x.coerceIn(
                minimumValue = -maxOffsetX,
                maximumValue = maxOffsetX
            ),
            y = offset.value.y.coerceIn(
                minimumValue = -maxOffsetY,
                maximumValue = maxOffsetY
            )
        )
    }

    LaunchedEffect(Unit) {
        viewModel.onDataRequest(true)
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .onSizeChanged {
                contentSize = IntSize(it.width, it.height)
            }
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
                            val xDiff = contentSize.width - tapCenter.x
                            val yDiff = contentSize.height - tapCenter.y
                            offset.value = Offset(xDiff, yDiff)
                        }
                    }
                )
            }
            .onMouseScroll { direction, amount ->
                coroutineScope.launch {
                    scale.animateTo(
                        (scale.value + amount / 2 * direction).coerceIn(
                            minimumValue = 1f,
                            maximumValue = maxZoom
                        )
                    )
                }
            }
            .transformable(
                rememberTransformableState { zoomChange, panChange, _ ->
                    coroutineScope.launch {
                        val maxOffsetX = (largerDimension * (scale.value)) / 2
                        val maxOffsetY = (largerDimension * (scale.value)) / 2
                        val newScale = (scale.value * zoomChange * zoomChange).coerceIn(
                            minimumValue = smallerDimension / largerDimension,
                            maximumValue = initialScale * maxZoom
                        )
                        scale.animateTo(newScale)

                        offset.value = Offset(
                            x = (offset.value.x + panChange.x * scale.value).coerceIn(
                                minimumValue = -maxOffsetX,
                                maximumValue = maxOffsetX
                            ),
                            y = (offset.value.y + panChange.y * scale.value).coerceIn(
                                minimumValue = -maxOffsetY,
                                maximumValue = maxOffsetY
                            )
                        )
                    }
                }
            ),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .requiredSize(largerDimension.dp)
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

                    val zIndex = categories.value.size - categories.value.indexOf(category) + 1f
                    val endIndex = if(networkItems.value == null) NETWORK_SHIMMER_ITEM_COUNT else (networkItems.value?.size ?: 0)

                    val items = mutableListOf<NetworkItemIO?>()
                    var finished = false
                    for(index in startingIndex until endIndex) {
                        if(!finished) {
                            networkItems.value?.getOrNull(index).let { data ->
                                if(data == null || category.range.contains(data.proximity ?: -1f)) {
                                    items.add(data)
                                }else {
                                    startingIndex = index
                                    finished = true
                                }
                            }
                        }
                    }

                    val radius = with(density) {
                        (largerDimension * shares / 2).dp.minus(layerPadding).toPx()
                    }
                    val minRadius = with(density) {
                        (largerDimension * previousShares / 2).dp.toPx()
                    }
                    val mappings = getMappings(
                        radius = radius,
                        minRadius = minRadius,
                        paddingPx = itemPaddingPx,
                        items = items
                    )
                    val circleSize = mappings.first
                    val mappedItems = mappings.second

                    Layout(
                        modifier = Modifier
                            .background(
                                color = (customColors.value[category] ?: category.color).copy(alpha = .5f),
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
                            .size((largerDimension * shares).dp),
                        content = {
                            items.forEach { data ->
                                NetworkItemCompact(
                                    modifier = Modifier
                                        .align(Alignment.Center)
                                        .animateContentSize()
                                        .zIndex(zIndex),
                                    data = data,
                                    onClick = {
                                        data?.userPublicId?.let { userPublicId ->
                                            navController?.navigate(
                                                NavigationNode.Conversation(
                                                    conversationId = userPublicId,
                                                    name = data.name
                                                )
                                            )
                                        }
                                    },
                                    size = with(density) { circleSize.toDp() }
                                )
                            }
                        }
                    ) { measurables, constraints ->
                        val placeables = measurables.map { measurable ->
                            measurable.measure(constraints)
                        }

                        layout(constraints.maxWidth, constraints.minHeight) {
                            var placeableIndex = 0
                            val centerX = constraints.maxWidth / 2 - circleSize / 2
                            val centerY = constraints.maxHeight / 2 - circleSize / 2

                            mappedItems.map { it.value }.forEachIndexed { indexLayer, items ->
                                var verticalDistance = (indexLayer / mappedItems.size) * circleSize
                                val radiusOverride = radius - (indexLayer * circleSize) - circleSize / 2

                                for(index in items.indices) {
                                    val x = centerX + (radiusOverride * cos(verticalDistance / radiusOverride))
                                    val y = centerY + (radiusOverride * sin(verticalDistance / radiusOverride))

                                    placeables[placeableIndex++].placeRelative(
                                        x = x.toInt(),
                                        y = y.toInt()
                                    )
                                    verticalDistance += circleSize + itemPaddingPx
                                }
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
    size: Dp,
    onClick: () -> Unit,
    data: NetworkItemIO?
) {
    val density = LocalDensity.current

    Crossfade(data == null) { isShimmer ->
        if(isShimmer) {
            Column(
                modifier = modifier
                    .requiredSize(size)
                    .width(IntrinsicSize.Min),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .aspectRatio(1f)
                        .weight(1f)
                        .brandShimmerEffect(shape = CircleShape)
                )
                Text(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 2.dp)
                        .brandShimmerEffect(),
                    text = "",
                    style = LocalTheme.current.styles.category
                )
            }
        }else if(data != null) {
            Column(
                modifier = modifier
                    .requiredSize(size)
                    .width(IntrinsicSize.Min),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                UserProfileImage(
                    modifier = Modifier
                        .aspectRatio(1f)
                        .weight(1f)
                        .scalingClickable {
                            onClick()
                        },
                    media = data.avatar,
                    tag = data.tag
                )
                Text(
                    modifier = Modifier
                        .fillMaxWidth(.8f)
                        .align(Alignment.CenterHorizontally),
                    text = data.name ?: "",
                    style = TextStyle(
                        fontFamily = FontFamily(fontQuicksandSemiBold),
                        fontSize = with(density) { (size / 6).toSp() },
                        textAlign = TextAlign.Center,
                        color = Color.White
                    ),
                    maxLines = 1
                )
            }
        }
    }
}

/** Utils function which calculates the appropriate circleSize and maps items based on their layers */
private fun <T> getMappings(
    radius: Float,
    minRadius: Float,
    paddingPx: Float,
    items: List<T?>
): Pair<Float, HashMap<Int, List<T?>>> {
    val itemsLeft = items.toMutableList()
    var circleSize = radius
    val mappedItems = hashMapOf<Int, List<T?>>()

    //step 1 -> first circle with the largest value
    //step 2 -> check if the circle fits all the items
    //step 3 -> take the number of items it does fit
    //step 4 -> reiterate until in the middle of the circle
    //step 5 -> if there are items left, decrease the circleSize and start at the step 2 at index 0
    var index = 0
    while (itemsLeft.isNotEmpty()) {
        val circleSizeWPadding = circleSize + paddingPx
        val currentRadius = radius - (index * circleSize) - circleSizeWPadding / 2

        val circumference = 2 * PI * currentRadius
        val itemsToFit = (circumference / circleSizeWPadding).toInt()

        if (itemsToFit > 0
            && circleSizeWPadding < currentRadius
            && currentRadius >= minRadius + circleSize / 2
        ) {
            val itemsToBeTaken = itemsLeft.take(itemsToFit.coerceAtMost(itemsLeft.size))
            mappedItems[index] = itemsToBeTaken
            itemsLeft.removeAll(itemsToBeTaken)

            // Check if there can be another layer
            if(itemsLeft.size > 0) index++
        } else {
            // If there's no more space for a new layer, reduce circleSize
            circleSize = (circleSize - 1).coerceAtLeast(1f)
            index = 0
            mappedItems.clear()
            itemsLeft.clear()
            itemsLeft.addAll(items)
        }

        if(circleSize == 1f) break
    }
    return circleSize to mappedItems
}
