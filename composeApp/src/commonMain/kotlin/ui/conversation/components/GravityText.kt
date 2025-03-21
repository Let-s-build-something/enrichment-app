package ui.conversation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.DrawStyle
import androidx.compose.ui.graphics.drawscope.DrawTransform
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.isUnspecified
import androidx.compose.ui.graphics.takeOrElse
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp

/**
 * Alternation of the font based on motion sensors of the author vs. current device
 *
 * https://developer.android.com/develop/ui/compose/quick-guides/content/video/draw-text-compose
 */
@Composable
fun GravityText(
    modifier: Modifier = Modifier,
    text: AnnotatedString
) {
    val textMeasurer = rememberTextMeasurer()
    val density = LocalDensity.current
    val size = remember {
        mutableStateOf(IntSize.Zero)
    }

    Box(
        modifier = (if(size.value != IntSize.Zero) {
            modifier.size(
                width = with(density) { size.value.width.toDp() },
                height = with(density) { size.value.height.toDp() }
            )
        } else modifier)
            .drawWithCache {
            val textResult = textMeasurer.measure(
                text,
                overflow = TextOverflow.Ellipsis,
                constraints = Constraints(
                    maxWidth = this.size.width.toInt()
                )
            )
            size.value = textResult.size

            onDrawBehind {
                drawText(
                    textLayoutResult = textResult,
                    transform = {
                        this.transform(
                            Matrix().apply {
                                this.values[Matrix.SkewX] = .5f
                            }
                        )
                    }
                )
            }
        }
    ) {
        SelectionContainer {
            Text(text = text)
        }
    }
}

@Composable
private fun Sample() {
    GravityText(
        modifier = Modifier
            .heightIn(min = 40.dp)
            .background(Color.White)
            .padding(horizontal = 24.dp)
            .fillMaxWidth(),
        text = buildAnnotatedString {
            append(
                "Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat. Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur. Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt mollit anim id est laborum."
            )
        }
    )
}


/**
 * Draw an existing text layout as produced by [TextMeasurer].
 *
 * This draw function cannot relayout when async font loading resolves. If using async fonts or
 * other dynamic text layout, you are responsible for invalidating layout on changes.
 *
 * @param textLayoutResult Text Layout to be drawn
 * @param color Text color to use
 * @param topLeft Offsets the text from top left point of the current coordinate system.
 * @param alpha opacity to be applied to the [color] from 0.0f to 1.0f representing fully
 * transparent to fully opaque respectively
 * @param shadow The shadow effect applied on the text.
 * @param textDecoration The decorations to paint on the text (e.g., an underline).
 * @param drawStyle Whether or not the text is stroked or filled in.
 * @param blendMode Blending algorithm to be applied to the text
 */
fun DrawScope.drawText(
    textLayoutResult: TextLayoutResult,
    color: Color = Color.Unspecified,
    topLeft: Offset = Offset.Zero,
    alpha: Float = Float.NaN,
    transform: DrawTransform.() -> Unit,
    shadow: Shadow? = null,
    textDecoration: TextDecoration? = null,
    drawStyle: DrawStyle? = null,
    blendMode: BlendMode = DrawScope.DefaultBlendMode
) {
    val newShadow = shadow ?: textLayoutResult.layoutInput.style.shadow
    val newTextDecoration = textDecoration ?: textLayoutResult.layoutInput.style.textDecoration
    val newDrawStyle = drawStyle ?: textLayoutResult.layoutInput.style.drawStyle

    withTransform({
        translate(topLeft.x, topLeft.y)
        transform()
        clip(textLayoutResult)
    }) {
        // if text layout was created using brush, and [color] is unspecified, we should treat this
        // like drawText(brush) call
        val brush = textLayoutResult.layoutInput.style.brush
        if (brush != null && color.isUnspecified) {
            textLayoutResult.multiParagraph.paint(
                drawContext.canvas,
                brush,
                if (!alpha.isNaN()) alpha else textLayoutResult.layoutInput.style.alpha,
                newShadow,
                newTextDecoration,
                newDrawStyle,
                blendMode
            )
        } else {
            textLayoutResult.multiParagraph.paint(
                drawContext.canvas,
                color.takeOrElse { textLayoutResult.layoutInput.style.color }.modulate(alpha),
                newShadow,
                newTextDecoration,
                newDrawStyle,
                blendMode
            )
        }
    }
}

private fun Color.modulate(alpha: Float): Color = when {
    alpha.isNaN() || alpha >= 1f -> this
    else -> this.copy(alpha = this.alpha * alpha)
}

private fun DrawTransform.clip(textLayoutResult: TextLayoutResult) {
    if (textLayoutResult.hasVisualOverflow &&
        textLayoutResult.layoutInput.overflow != TextOverflow.Visible
    ) {
        clipRect(
            left = 0f,
            top = 0f,
            right = textLayoutResult.size.width.toFloat(),
            bottom = textLayoutResult.size.height.toFloat()
        )
    }
}
