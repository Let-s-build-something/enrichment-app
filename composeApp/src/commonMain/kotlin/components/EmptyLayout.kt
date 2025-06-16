package components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import augmy.composeapp.generated.resources.Res
import augmy.interactive.shared.ui.components.OutlinedButton
import augmy.interactive.shared.ui.theme.LocalTheme
import io.github.alexzhirkevich.compottie.DotLottie
import io.github.alexzhirkevich.compottie.LottieCompositionSpec
import io.github.alexzhirkevich.compottie.rememberLottieComposition
import io.github.alexzhirkevich.compottie.rememberLottiePainter
import org.jetbrains.compose.resources.ExperimentalResourceApi

/** Simple layout for displaying empty states */
@OptIn(ExperimentalResourceApi::class)
@Composable
fun EmptyLayout(
    modifier: Modifier = Modifier,
    title: String? = null,
    animResPath: String = "files/empty.lottie",
    description: String? = null,
    action: String? = null,
    onClick: () -> Unit = {},
    content: @Composable ColumnScope.() -> Unit = {}
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        val composition by rememberLottieComposition {
            LottieCompositionSpec.DotLottie(
                Res.readBytes(animResPath)
            )
        }

        Image(
            modifier = Modifier
                .padding(top = 24.dp)
                .fillMaxWidth()
                .requiredHeight(200.dp),
            painter = rememberLottiePainter(
                composition = composition,
                reverseOnRepeat = true,
                iterations = Int.MAX_VALUE
            ),
            contentDescription = null
        )
        if (title != null) {
            Text(
                modifier = Modifier.fillMaxWidth(.8f),
                text = title,
                style = LocalTheme.current.styles.category.copy(
                    textAlign = TextAlign.Center
                )
            )
        }
        description?.let { text ->
            Text(
                modifier = Modifier.fillMaxWidth(.8f),
                text = text,
                style = LocalTheme.current.styles.regular.copy(
                    textAlign = TextAlign.Center
                )
            )
        }
        action?.let { text ->
            OutlinedButton(
                modifier = Modifier.padding(top = 12.dp),
                text = text,
                onClick = onClick,
                activeColor = LocalTheme.current.colors.secondary
            )
        }
        content()
    }
}