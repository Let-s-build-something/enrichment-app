package components

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import augmy.composeapp.generated.resources.Res
import augmy.interactive.shared.ui.theme.LocalTheme
import data.io.base.BaseResponse
import io.github.alexzhirkevich.compottie.DotLottie
import io.github.alexzhirkevich.compottie.LottieCompositionSpec
import io.github.alexzhirkevich.compottie.rememberLottieComposition
import io.github.alexzhirkevich.compottie.rememberLottiePainter
import org.jetbrains.compose.resources.ExperimentalResourceApi

/** Indication reflective of [response] state */
@OptIn(ExperimentalResourceApi::class)
@Composable
fun LoadingIndicator(
    modifier: Modifier = Modifier,
    response: BaseResponse<*>?
) {
    Crossfade(
        modifier = modifier,
        targetState = response
    ) { res ->
        if(res is BaseResponse.Loading) {
            CircularProgressIndicator(
                modifier = Modifier.requiredSize(32.dp),
                color = LocalTheme.current.colors.brandMainDark,
                trackColor = LocalTheme.current.colors.tetrial
            )
        }else if(res != null) {
            val comp by rememberLottieComposition {
                LottieCompositionSpec.DotLottie(
                    Res.readBytes("files/${
                        if(res is BaseResponse.Success) "success" else "error"
                    }.lottie")
                )
            }

            Image(
                modifier = Modifier.requiredSize(32.dp),
                painter = rememberLottiePainter(
                    reverseOnRepeat = false,
                    composition = comp
                ),
                contentDescription = null,
                contentScale = ContentScale.Fit
            )
        }
    }
}