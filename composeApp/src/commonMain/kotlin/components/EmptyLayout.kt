package components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import augmy.composeapp.generated.resources.Res
import augmy.interactive.shared.ui.components.BrandHeaderButton
import augmy.interactive.shared.ui.components.OutlinedButton
import augmy.interactive.shared.ui.components.dialog.ButtonState
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
    description: String? = null,
    animPath: String? = null,
    animReverseOnRepeat: Boolean = true,
    primaryAction: ButtonState? = null,
    secondaryAction: ButtonState? = null,
    content: @Composable ColumnScope.() -> Unit = {}
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        val composition by rememberLottieComposition {
            LottieCompositionSpec.DotLottie(
                Res.readBytes(animPath ?: "files/empty.lottie")
            )
        }

        Image(
            modifier = Modifier
                .padding(top = 24.dp)
                .fillMaxWidth()
                .requiredHeight(200.dp),
            painter = rememberLottiePainter(
                composition = composition,
                reverseOnRepeat = animReverseOnRepeat,
                iterations = Int.MAX_VALUE
            ),
            contentDescription = null
        )
        if (title != null) {
            Text(
                modifier = Modifier.fillMaxWidth(.8f),
                text = title,
                style = LocalTheme.current.styles.category.copy(
                    textAlign = TextAlign.Center,
                    color = LocalTheme.current.colors.secondary
                )
            )
        }
        description?.let { text ->
            Text(
                modifier = Modifier
                    .padding(top = 2.dp)
                    .fillMaxWidth(.8f),
                text = text,
                style = LocalTheme.current.styles.regular.copy(
                    textAlign = TextAlign.Center
                )
            )
        }
        Row(
            modifier = Modifier.padding(top = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            secondaryAction?.let { action ->
                OutlinedButton(
                    text = action.text,
                    trailingIcon = action.imageVector,
                    onClick = action.onClick,
                    activeColor = LocalTheme.current.colors.secondary
                )
            }
            primaryAction?.let { action ->
                BrandHeaderButton(
                    text = action.text,
                    onClick = action.onClick,
                    endImageVector = action.imageVector,
                    contentPadding = PaddingValues(
                        vertical = 6.dp,
                        horizontal = 12.dp
                    )
                )
            }
        }
        content()
    }
}