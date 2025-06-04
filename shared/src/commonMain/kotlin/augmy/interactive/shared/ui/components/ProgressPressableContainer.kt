package augmy.interactive.shared.ui.components

import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import augmy.interactive.shared.ext.scalingClickable
import augmy.interactive.shared.ui.theme.LocalTheme
import kotlinx.coroutines.launch

@Composable
fun ProgressPressableContainer(
    modifier: Modifier = Modifier,
    lengthMillis: Int = 1_500,
    trackColor: Color = LocalTheme.current.colors.tetrial,
    progressColor: Color = LocalTheme.current.colors.brandMainDark,
    onFinish: () -> Unit,
    content: @Composable BoxScope.() -> Unit
) {
    val scope = rememberCoroutineScope()
    val progress = remember {
        androidx.compose.animation.core.Animatable(0f)
    }

    Box(
        modifier = modifier.scalingClickable(
            onPress = { _, isPressed ->
                scope.launch {
                    if(isPressed) {
                        progress.animateTo(1f, animationSpec = tween(lengthMillis))
                    }else {
                        if(progress.value >= 1f) onFinish()
                        progress.animateTo(0f, animationSpec = tween(lengthMillis / 5))
                    }
                }
            }
        )
    ) {
        CircularProgressIndicator(
            modifier = Modifier
                .fillMaxSize()
                .aspectRatio(1f)
                .scale(1f + progress.value * 1.5f),
            strokeWidth = 1.dp,
            color = progressColor,
            trackColor = trackColor,
            progress = {
                progress.value
            }
        )
        Box(
            modifier = Modifier.padding(4.dp),
            content = content
        )
    }
}