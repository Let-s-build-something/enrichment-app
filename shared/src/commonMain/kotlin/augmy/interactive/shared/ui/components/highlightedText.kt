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
    annotatedString: AnnotatedString
): AnnotatedString {
    if (highlight.isNullOrBlank()) return annotatedString

    val text = annotatedString.text
    val lowerText = text.lowercase()
    val lowerHighlight = highlight.lowercase()

    if (!lowerText.contains(lowerHighlight)) return annotatedString

    return buildAnnotatedString {
        var startIndex = 0
        while (startIndex < text.length) {
            val matchIndex = lowerText.indexOf(lowerHighlight, startIndex)
            if (matchIndex == -1) {
                append(annotatedString.subSequence(startIndex, text.length))
                break
            }

            if (matchIndex > startIndex) {
                append(annotatedString.subSequence(startIndex, matchIndex))
            }

            withStyle(
                style = SpanStyle(
                    fontWeight = FontWeight.ExtraBold,
                    color = highLightColor
                )
            ) {
                append(annotatedString.subSequence(matchIndex, matchIndex + highlight.length))
            }

            startIndex = matchIndex + highlight.length
        }
    }
}
