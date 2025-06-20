package components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import augmy.interactive.shared.ui.components.getRandomLoadingLottieAnim
import augmy.interactive.shared.ui.theme.LocalTheme
import io.github.alexzhirkevich.compottie.LottieCompositionSpec
import io.github.alexzhirkevich.compottie.rememberLottieComposition
import io.github.alexzhirkevich.compottie.rememberLottiePainter

@Composable
fun LoadingDialog(
    onDismissRequest: () -> Unit,
    properties: DialogProperties = DialogProperties(dismissOnBackPress = true, dismissOnClickOutside = false),
    content: @Composable ColumnScope.() -> Unit = {}
) {
    Dialog(
        onDismissRequest = onDismissRequest,
        properties = properties
    ) {
        Column(
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            val composition by rememberLottieComposition {
                LottieCompositionSpec.JsonString(getRandomLoadingLottieAnim())
            }

            Image(
                modifier = Modifier
                    .background(LocalTheme.current.colors.tetrial, RoundedCornerShape(20.dp))
                    .fillMaxWidth(.2f)
                    .aspectRatio(1f),
                painter = rememberLottiePainter(composition = composition),
                contentDescription = null,
                contentScale = ContentScale.FillWidth
            )
            content()
        }
    }
}
