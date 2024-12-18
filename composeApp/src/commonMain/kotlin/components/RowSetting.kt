package components

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.unit.dp
import augmy.composeapp.generated.resources.Res
import augmy.interactive.shared.ui.theme.LocalTheme
import data.io.base.BaseResponse
import augmy.interactive.shared.ext.scalingClickable
import io.github.alexzhirkevich.compottie.DotLottie
import io.github.alexzhirkevich.compottie.LottieCompositionSpec
import io.github.alexzhirkevich.compottie.rememberLottieComposition
import io.github.alexzhirkevich.compottie.rememberLottiePainter
import org.jetbrains.compose.resources.ExperimentalResourceApi

/**
 * Animated single setting with variable modes.
 * It responds to [response] states with animations.
 * @param lottieFileName name of the .lottie file, whose progress value is dictated by [progressValue],
 * generally, it can display multitude of states based on the progress value
 */
@OptIn(ExperimentalResourceApi::class)
@Composable
fun RowSetting(
    modifier: Modifier = Modifier,
    response: BaseResponse<*>?,
    title: String,
    scale: Float = 1f,
    lottieFileName: String,
    tint: Color? = null,
    progressValue: Float,
    content: String? = null,
    onTap: (Offset) -> Unit
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .scalingClickable(
                onTap = onTap,
                scaleInto = 0.92f
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        val composition by rememberLottieComposition {
            LottieCompositionSpec.DotLottie(
                Res.readBytes("files/$lottieFileName.lottie")
            )
        }

        val progress = animateFloatAsState(
            targetValue = progressValue,
            label = "progressPrivatePublic",
            animationSpec = tween(durationMillis = 750)
        )

        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                modifier = Modifier
                    .weight(.2f)
                    .scale(scale)
                    .height(65.dp)
                    .animateContentSize(),
                painter = rememberLottiePainter(
                    composition = composition,
                    progress = {
                        progress.value
                    }
                ),
                colorFilter = if(tint != null) ColorFilter.tint(tint) else null,
                contentDescription = null
            )
            Column(modifier = Modifier.weight(.8f)) {
                Text(
                    text = title,
                    style = LocalTheme.current.styles.category
                )
                if(content != null) {
                    Text(
                        text = content,
                        style = LocalTheme.current.styles.regular
                    )
                }
            }
        }

        LoadingIndicator(response = response)
    }
}