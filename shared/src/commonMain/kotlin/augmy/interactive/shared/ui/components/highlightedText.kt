package augmy.interactive.shared.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import augmy.interactive.shared.ui.theme.LocalTheme

/** Build an [AnnotatedString] with given highlight */
@Composable
fun highlightedText(
    highlight: String?,
    highLightColor: Color = LocalTheme.current.colors.brandMain,
    text: String
): AnnotatedString {
    return buildAnnotatedString {
        if(!highlight.isNullOrBlank() && text.lowercase().contains(highlight.lowercase())) {
            var textLeft = text

            while(textLeft.isNotEmpty()) {
                val index = textLeft.lowercase().indexOf(highlight.lowercase())

                if(index != -1) {
                    append(textLeft.substring(0, index))
                    withStyle(
                        style = SpanStyle(
                            fontWeight = FontWeight.ExtraBold,
                            color = highLightColor
                        )
                    ) {
                        append(textLeft.substring(index, index + highlight.length))
                    }
                    textLeft = textLeft.substring(index + highlight.length, textLeft.length)
                }else {
                    append(textLeft)
                    textLeft = ""
                }
            }
        }else {
            append(text)
        }
    }
}