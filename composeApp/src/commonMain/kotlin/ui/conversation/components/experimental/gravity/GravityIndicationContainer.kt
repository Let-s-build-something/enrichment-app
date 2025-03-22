package ui.conversation.components.experimental.gravity

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.LinearGradientShader
import androidx.compose.ui.graphics.Shader
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.unit.dp
import augmy.interactive.shared.ui.base.DeviceOrientation
import augmy.interactive.shared.ui.base.LocalOrientation
import augmy.interactive.shared.ui.theme.LocalTheme
import ui.conversation.components.experimental.gravity.GravityUseCase.Companion.FULL_GRAVITY
import ui.conversation.components.experimental.gravity.GravityUseCase.Companion.MAX_FRACTION_OFFSET
import kotlin.math.absoluteValue

private data class GravityIndicationStops(
    val stops: List<Float>,
    val from: Offset,
    val to: Offset,
)

private fun calculateIndicationStops(
    isVertical: Boolean,
    size: Size,
    gx: Float,
    gy: Float,
    gz: Float
): GravityIndicationStops {
    val x = if(isVertical) gx else gy
    val y = if(isVertical) gy else gx

    val verticalFraction = (y.absoluteValue / FULL_GRAVITY).minus(1f).absoluteValue * MAX_FRACTION_OFFSET
    val value = (x / FULL_GRAVITY / 2 + .5f).minus(1).absoluteValue.coerceAtMost(1f)
    //val depthFraction = gz / FULL_GRAVITY * 2

    val stops = listOf(
        value - verticalFraction / 2,
        value,
        value + verticalFraction / 2,
    )
    //val horizontalOffset = size.width * (1f - value) * 0.8f
    //val verticalOffset = size.height * (0.5f + depthFraction)

    val from = Offset.Zero //Offset(horizontalOffset, verticalOffset)
    val to = Offset(size.width, size.height) //Offset(size.width - horizontalOffset, size.height - verticalOffset)

    return GravityIndicationStops(
        stops = stops,
        from = from,
        to = to
    )
}

@Composable
fun GravityIndicationContainer(
    modifier: Modifier = Modifier,
    indicationColor: Color = LocalTheme.current.colors.tetrial,
    backgroundColor: Color,
    shape: Shape,
    content: @Composable BoxScope.() -> Unit= {}
) {
    val orientation = LocalOrientation.current
    val gravityValues = remember {
        mutableStateOf(Triple(0f, 0f, 0f))
    }

    val customBrush = rememberUpdatedState(
        gravityValues.value.let { gravity ->
            object: ShaderBrush() {
                override fun createShader(size: Size): Shader {
                    val indication = calculateIndicationStops(
                        isVertical = orientation == DeviceOrientation.Vertical,
                        size = size,
                        gx = gravity.first,
                        gy = gravity.second,
                        gz = gravity.third
                    )                    
                    return LinearGradientShader(
                        colors = listOf(
                            backgroundColor,
                            indicationColor,
                            backgroundColor,
                        ),
                        colorStops = indication.stops,
                        tileMode = TileMode.Decal,
                        from = indication.from,
                        to = indication.to
                    )
                }
            }
        }
    )

    Box(
        modifier = Modifier
            .border(
                width = 2.dp,
                shape = shape,
                brush = customBrush.value
            ).then(modifier),
        content = content
    )
}
