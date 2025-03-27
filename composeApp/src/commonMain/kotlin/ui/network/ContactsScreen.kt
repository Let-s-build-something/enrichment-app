package ui.network

import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyHorizontalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import augmy.composeapp.generated.resources.Res
import augmy.composeapp.generated.resources.button_search
import augmy.composeapp.generated.resources.screen_home
import augmy.interactive.shared.ext.scalingClickable
import augmy.interactive.shared.ui.base.LocalScreenSize
import augmy.interactive.shared.ui.components.input.CustomTextField
import augmy.interactive.shared.ui.theme.LocalTheme
import base.BrandBaseScreen
import base.navigation.NavIconType
import components.UserProfileImage
import data.io.user.NetworkItemIO
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import ui.network.profile.UserProfileLauncher
import kotlin.math.sqrt
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/**
 * Screen for the home page
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContactsScreen() {
    val density = LocalDensity.current
    val screenHeight = LocalScreenSize.current.height - WindowInsets.systemBars
        .asPaddingValues()
        .calculateTopPadding()
        .value
    val percentages = listOf(
        0.4f,
        0.175f,
        0.125f,
        0.1f,
        0.075f,
    )

    val searchState = remember { TextFieldState() }
    val contentHeight = rememberSaveable {
        mutableStateOf(screenHeight)
    }
    val selectedProfile = remember {
        mutableStateOf<NetworkItemIO?>(null)
    }

    if(selectedProfile.value != null) {
        UserProfileLauncher(
            userProfile = selectedProfile.value,
            onDismissRequest = {
                selectedProfile.value = null
            }
        )
    }

    BrandBaseScreen(
        title = stringResource(Res.string.screen_home),
        navIconType = NavIconType.HOME
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            CustomTextField(
                modifier = Modifier
                    .zIndex(10f)
                    .fillMaxWidth(),
                hint = stringResource(Res.string.button_search),
                state = searchState,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Text,
                    imeAction = ImeAction.Search
                ),
                isClearable = true,
                prefixIcon = Icons.Outlined.Search,
                paddingValues = PaddingValues(start = 16.dp),
                shape = RoundedCornerShape(
                    topStart = LocalTheme.current.shapes.screenCornerRadius,
                    topEnd = LocalTheme.current.shapes.screenCornerRadius,
                    bottomStart = LocalTheme.current.shapes.componentCornerRadius,
                    bottomEnd = LocalTheme.current.shapes.componentCornerRadius
                )
            )
            Box(
                modifier = Modifier
                    .weight(1f, fill = true)
                    .onSizeChanged { size ->
                        contentHeight.value = with(density) {
                            size.height.toDp().value
                        }
                    }
            ) {
                repeat(5) { index ->
                    val heightAbove = percentages.subList(0, index).sum() * contentHeight.value
                    val height = percentages[index] * contentHeight.value

                    val gridState = rememberLazyGridState()
                    val coroutineScope = rememberCoroutineScope()

                    SocialCircleTier(
                        modifier = Modifier
                            .offset(0.dp, heightAbove.dp)
                            .draggable(
                                orientation = Orientation.Horizontal,
                                state = rememberDraggableState { delta ->
                                    coroutineScope.launch {
                                        gridState.scrollBy(-delta)
                                    }
                                },
                            ),
                        gridState = gridState,
                        height = height * .8f,
                        rows = if(index == 0) 2 else 1,
                        showNames = index < 3
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalUuidApi::class)
@Composable
private fun SocialCircleTier(
    modifier: Modifier,
    height: Float,
    gridState: LazyGridState,
    showNames: Boolean,
    rows: Int = 1
) {
    val maxOffsetY = height * 2.5
    val width = gridState.layoutInfo.viewportSize.width.toFloat()
    val density = LocalDensity.current

    LazyHorizontalGrid(
        modifier = modifier
            .height(height.times(1.4f).dp)
            .fillMaxWidth(),
        rows = GridCells.Fixed(rows),
        state = gridState,
        verticalArrangement = Arrangement.Bottom
    ) {
        items(
            items = listOf<NetworkItemIO>(),
            key = { data -> data.userPublicId ?: Uuid.random().toString() }
        ) { data ->
            val offsetX = remember(data.userPublicId) {
                mutableStateOf(0f)
            }

            val radius = width / 2f
            val xCentered = offsetX.value - radius
            val constrainedX = xCentered.coerceIn(-radius, radius)
            val value = 1f - sqrt(radius * radius - constrainedX * constrainedX) / radius

            Column(
                modifier = Modifier
                    .requiredHeight(height.times(1.2f).div(rows).dp)
                    .padding(top = if(rows > 1) 0.dp else height.times(.1f).dp)
                    .offset(0.dp, -(maxOffsetY * value).dp)
                    .onGloballyPositioned { coordinates ->
                        val offset = coordinates.size.width * transform(coordinates.positionInParent().x / width)
                        offsetX.value = (coordinates.positionInParent().x + offset).coerceIn(
                            minimumValue = 0f,
                            maximumValue = width
                        )
                    }
                    .scalingClickable {

                    }
                    .width(IntrinsicSize.Min),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                UserProfileImage(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .aspectRatio(1f),
                    media = data.avatar,
                    tag = data.tag
                )
                if(showNames) {
                    Text(
                        modifier = Modifier.padding(4.dp),
                        text = data.name ?: "",
                        style = LocalTheme.current.styles.category.copy(
                            fontSize = with(density) {
                                (height / 3).dp.toSp().value.coerceAtMost(14f).sp
                            }
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

// whenever the value is 0 we want to return to 1, whenever the value is 1 we want to return -1,
// but when the value is 0.5 we want to return 0.5
// the returned number is range between the edges, meaning we want to have an equation that'd find the right number within a range
private fun transform(input: Float): Float {
    return 1f - 2f * input
}
