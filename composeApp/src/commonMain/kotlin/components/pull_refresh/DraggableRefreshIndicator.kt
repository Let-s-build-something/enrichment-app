package components.pull_refresh

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.pullrefresh.PullRefreshState
import androidx.compose.material.pullrefresh.pullRefreshIndicatorTransform
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import chat.enrichment.shared.ui.components.REFRESH_RETURN_ANIMATION_LENGTH
import chat.enrichment.shared.ui.components.getRandomLoadingLottieAnim
import chat.enrichment.shared.ui.theme.LocalTheme
import io.github.alexzhirkevich.compottie.LottieCompositionSpec
import io.github.alexzhirkevich.compottie.rememberLottieComposition
import io.github.alexzhirkevich.compottie.rememberLottiePainter
import kotlinx.coroutines.launch

private var lastProgressValue = 0f

/** Main screen, visible first when user opens the app */
@OptIn(ExperimentalMaterialApi::class)
@Composable
fun DraggableRefreshIndicator(
    modifier: Modifier = Modifier,
    pullRefreshSize: Dp,
    isRefreshing: Boolean = false,
    state: PullRefreshState,
    onScrollChange: (indicatorOffsetDp: Dp) -> Unit
) {
    val animRawResBackground = remember {
        mutableStateOf<String?>(null)
    }
    val indicatorOffset = remember { Animatable(0f) }

    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        if(animRawResBackground.value == null) {
            animRawResBackground.value = getRandomLoadingLottieAnim()
        }
    }
    LaunchedEffect(indicatorOffset.value) {
        onScrollChange(indicatorOffset.value.dp)
        if (indicatorOffset.value == 0f) {
            animRawResBackground.value = getRandomLoadingLottieAnim()
        }
    }

    fun animateTo(fromProgress: Float, toProgress: Float) {
        scope.launch {
            indicatorOffset.snapTo(pullRefreshSize.times(fromProgress).value)
            indicatorOffset.animateTo(
                targetValue = pullRefreshSize.times(toProgress).value,
                animationSpec = tween(durationMillis = REFRESH_RETURN_ANIMATION_LENGTH)
            )
        }
    }

    LaunchedEffect(key1 = state.progress, isRefreshing) {
        if (state.progress == 0f
            && lastProgressValue >= 1f
        ) {
            // confirmed refresh, lets go to the right position
            animateTo(
                fromProgress = lastProgressValue,
                toProgress = 1f
            )
        } else if(state.progress == 0f
            && lastProgressValue == 0f
            && isRefreshing.not()
            && indicatorOffset.value.dp > 0.dp
        ) {
            // refresh ended, return back
            animateTo(
                fromProgress = 1f,
                toProgress = 0f
            )
        } else if(lastProgressValue in 0.1f..1f && state.progress == 0f) {
            // something in between confirmation and beginning - failed refresh
            animateTo(
                fromProgress = lastProgressValue,
                toProgress = if(isRefreshing) 1f else 0f
            )
        }else if(isRefreshing.not()) {
            // normal move, only if there is no ongoing animation - active lastAnimator
            onScrollChange(
                pullRefreshSize.times(kotlin.math.min(state.progress, 2f)).value.dp
            )
        }
        lastProgressValue = kotlin.math.min(state.progress, 2f)
    }

    Box(
        modifier = modifier.pullRefreshIndicatorTransform(state),
        contentAlignment = Alignment.BottomCenter
    ) {
        if(isRefreshing) {
            val composition by rememberLottieComposition {
                LottieCompositionSpec.JsonString(animRawResBackground.value ?: "")
            }

            Image(
                modifier = Modifier
                    .padding(
                        top = 4.dp,
                        start = 4.dp,
                        end = 4.dp,
                        bottom = 8.dp
                    )
                    .background(LocalTheme.current.colors.tetrial, RoundedCornerShape(20.dp))
                    .size(pullRefreshSize.minus(12.dp)),
                painter = rememberLottiePainter(
                    composition = composition
                ),
                contentDescription = null,
                contentScale = ContentScale.FillHeight
            )
        }else {
            Box(
                modifier = Modifier
                    .padding(
                        top = 4.dp,
                        start = 4.dp,
                        end = 4.dp,
                        bottom = 8.dp
                    )
                    .background(LocalTheme.current.colors.tetrial, RoundedCornerShape(20.dp))
                    .size(pullRefreshSize.minus(12.dp))

            )
        }
    }
}