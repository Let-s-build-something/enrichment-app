package com.squadris.squadris.compose.components

import android.animation.ValueAnimator
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.pullrefresh.PullRefreshState
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.compose.ConstraintLayoutScope
import androidx.constraintlayout.compose.Dimension
import androidx.core.animation.doOnCancel
import androidx.core.animation.doOnEnd
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.rememberLottieComposition
import com.squadris.squadris.compose.theme.LocalTheme

private val animRawResBackground = mutableIntStateOf(randomLoadingLottieAnim)
private var lastProgressValue = 0f
private var lastAnimator: ValueAnimator? = null

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
    val indicatorOffset = remember { mutableStateOf(0.dp) }
    val composition by rememberLottieComposition(
        LottieCompositionSpec.RawRes(animRawResBackground.intValue)
    )
    fun animateTo(fromProgress: Float, toProgress: Float) {
        if(lastAnimator != null) return
        lastAnimator = ValueAnimator.ofFloat(fromProgress, toProgress)
            .setDuration(REFRESH_RETURN_ANIMATION_LENGTH)
        lastAnimator?.addUpdateListener { animator ->
            (animator.animatedValue as? Float)?.let { progress ->
                indicatorOffset.value = pullRefreshSize
                    .times(progress)
                onScrollChange(indicatorOffset.value)
            }
        }
        lastAnimator?.doOnEnd {
            if(toProgress == 0f) {
                animRawResBackground.intValue = randomLoadingLottieAnim
            }
            lastAnimator?.removeAllListeners()
            lastAnimator = null
        }
        lastAnimator?.doOnCancel {
            lastAnimator?.removeAllListeners()
            lastAnimator = null
        }
        lastAnimator?.start()
    }

    LaunchedEffect(indicatorOffset.value) {
        onScrollChange(indicatorOffset.value)
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
            && indicatorOffset.value > 0.dp
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
        }else if(isRefreshing.not() && lastAnimator == null) {
            // normal move, only if there is no ongoing animation - active lastAnimator
            indicatorOffset.value = pullRefreshSize
                .times(kotlin.math.min(state.progress, 2f))
        }
        lastProgressValue = kotlin.math.min(state.progress, 2f)
    }

    CustomPullRefresh(
        modifier = modifier,
        isRefreshing = isRefreshing,
        state = state
    ) {
        if(isRefreshing) {
            LottieAnimation(
                modifier = Modifier
                    .padding(
                        top = 4.dp,
                        start = 4.dp,
                        end = 4.dp,
                        bottom = 8.dp
                    )
                    .background(LocalTheme.current.colors.tetrial, RoundedCornerShape(20.dp))
                    .size(pullRefreshSize.minus(12.dp)),
                composition = composition,
                restartOnPlay = true,
                isPlaying = true,
                contentScale = ContentScale.FillHeight,
                iterations = Int.MAX_VALUE
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

/** Indicator of pull refresh progress */
@OptIn(ExperimentalMaterialApi::class)
@Composable
fun ConstraintLayoutScope.ProgressBarRefreshIndicator(
    isRefreshing: Boolean,
    state: PullRefreshState
) {
    val (progressIndicatorLeft, progressIndicatorRight) = createRefs()

    LinearProgressIndicator(
        progress = {
            if(isRefreshing) 1f else {
                kotlin.math.min(state.progress, 1f)
            }
        },
        modifier = Modifier
            .requiredHeight(4.dp)
            .rotate(180f)
            .constrainAs(progressIndicatorRight) {
                end.linkTo(parent.end)
                start.linkTo(progressIndicatorLeft.end)
                top.linkTo(parent.top, (-4).dp)
                width = Dimension.fillToConstraints
            },
        color = LocalTheme.current.colors.tetrial,
        trackColor = Color.Transparent,
    )
    LinearProgressIndicator(
        progress = { if(isRefreshing) 1f else kotlin.math.min(state.progress, 1f) },
        modifier = Modifier
            .requiredHeight(4.dp)
            .constrainAs(progressIndicatorLeft) {
                start.linkTo(parent.start)
                end.linkTo(progressIndicatorRight.start)
                top.linkTo(parent.top, (-4).dp)
                width = Dimension.fillToConstraints
            },
        color = LocalTheme.current.colors.tetrial,
        trackColor = Color.Transparent,
    )
}